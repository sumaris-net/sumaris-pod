package net.sumaris.core.test;

/*-
 * #%L
 * SUMARiS :: Test shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.schema.DatabaseSchemaDaoImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.service.ServiceLocator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * To be able to manage database connection for unit test.
 *
 * @author blavenie <benoit.lavenier@e-is.pro>
 * @since 3.3.3
 */
@Slf4j
public abstract class DatabaseResource implements TestRule {

    public static final String ACTIVE_PROFILE = "test";
    /** Constant <code>HSQLDB_DATASOURCE_TYPE="hsqldb"</code> */
    public static final String HSQLDB_DATASOURCE_TYPE = "hsqldb";
    public static final String ORACLE_DATASOURCE_TYPE = "oracle";
    public static final String PGSQL_DATASOURCE_TYPE = "pgsql";
    /** Constant <code>HSQLDB_SRC_DATABASE_DIRECTORY= ie : "../sumaris-core/src/test/db"</code> */
    public static final String HSQLDB_SRC_DATABASE_DIRECTORY_PATTERN = "../%s/target/db";
    public static final String HSQLDB_SRC_DATABASE_NAME = "sumaris";
    public static final String HSQLDB_SRC_DATABASE_SCRIPT_FILE = HSQLDB_SRC_DATABASE_NAME + ".script";
    public static final String HSQLDB_SRC_DATABASE_PROPERTIES_FILE = HSQLDB_SRC_DATABASE_NAME + ".properties";

    /** Constant <code>BUILD_TIMESTAMP=System.nanoTime()</code> */
    public static final long BUILD_TIMESTAMP = System.nanoTime();

    private File resourceDirectory;

    private String dbDirectory;

    private final boolean readOnly;

    private final String configFileSuffix;

    private final String datasourcePlatform;

    private boolean withError = false;

    private Class<?> testClass;

    /**
     * <p>Constructor for DatabaseResource.</p>
     *
     * @param configFileSuffix a {@link String} object.
     * @param datasourcePlatform the datasource platform (e.g. 'oracle', 'hsqldb', 'pgsql')
     * @param readOnly a boolean.
     */
    protected DatabaseResource(String configFileSuffix,
                               String datasourcePlatform,
                               boolean readOnly) {
        this.configFileSuffix = configFileSuffix;
        this.readOnly = readOnly;
        this.datasourcePlatform = datasourcePlatform != null ? datasourcePlatform :
            ((ORACLE_DATASOURCE_TYPE.equals(configFileSuffix) || PGSQL_DATASOURCE_TYPE.equals(configFileSuffix))
            ? configFileSuffix
            : HSQLDB_DATASOURCE_TYPE);

        // Enable profile
        System.setProperty("spring.config.location", "classpath:/");
        System.setProperty("spring.profiles.active", ACTIVE_PROFILE + "," + this.datasourcePlatform);
    }

    /**
     * Return configuration files prefix (i.e. 'sumaris-test')
     * Could be override by external project
     *
     * @return the prefix to use to retrieve configuration files
     */
    protected abstract String getConfigFilesPrefix();

    protected abstract String getModuleDirectory();

    protected String getHsqldbSrcDatabaseDirectory() {
        return String.format(HSQLDB_SRC_DATABASE_DIRECTORY_PATTERN, getModuleDirectory());
    }

    protected String getHsqldbSrcCreateScript() {
        return String.format("%s/%s", getHsqldbSrcDatabaseDirectory(), HSQLDB_SRC_DATABASE_SCRIPT_FILE);
    }

    /**
     * <p>Getter for the field <code>resourceDirectory</code>.</p>
     *
     * @param name a {@link String} object.
     * @return a {@link File} object.
     */
    public File getResourceDirectory(String name) {
        return new File(resourceDirectory, name);
    }

    /**
     * <p>Getter for the field <code>resourceDirectory</code>.</p>
     *
     * @return a {@link File} object.
     */
    public File getResourceDirectory() {
        return resourceDirectory;
    }

    /**
     * <p>canWrite.</p>
     *
     * @return a boolean.
     */
    protected boolean canWrite() {
        return !readOnly;
    }

    /**
     * <p>isWriteDb.</p>
     *
     * @return a boolean.
     */
    protected boolean isReadOnly() {
        return readOnly;
    }

    /** {@inheritDoc} */
    @Override
    public Statement apply(final Statement base, final Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(description);
                try {
                    base.evaluate();
                } catch (Throwable e) {
                    withError = true;
                    log.error("Error during test", e);
                } finally {
                    after(description);
                }
            }
        };
    }

    /**
     * <p>before.</p>
     *
     * @param description a {@link Description} object.
     * @throws Throwable if any.
     */
    protected void before(Description description) throws Throwable {
        testClass = description.getTestClass();

        boolean defaultDbName = StringUtils.isEmpty(configFileSuffix);

        dbDirectory = null;

        if (log.isDebugEnabled()) {
            log.debug("Prepare test " + testClass);
        }

        resourceDirectory = getTestSpecificDirectory(testClass, "");
        addToDestroy(resourceDirectory);

        // Load datasource type
        String datasourcePlatform = getDatasourcePlatform();

        // check that config file is in classpath (avoid to find out why it does not works...)
        String configFilename = getConfigFilesPrefix();
        if (!defaultDbName) {
            configFilename += "-" + configFileSuffix;
        }
        String configFilenameNoPlatform = configFilename + ".properties";
        if (StringUtils.isNotBlank(datasourcePlatform)) {
            configFilename += "-" + datasourcePlatform;
        }
        configFilename += ".properties";

        InputStream resourceAsStream = getClass().getResourceAsStream("/" + configFilename);
        if (resourceAsStream == null && StringUtils.isNotBlank(datasourcePlatform)) {
            resourceAsStream = getClass().getResourceAsStream("/" + configFilenameNoPlatform);
            Preconditions.checkNotNull(resourceAsStream, "Could not find " + configFilename + " or " + configFilenameNoPlatform + " in test class-path");
            configFilename = configFilenameNoPlatform;
        }
        else {
            Preconditions.checkNotNull(resourceAsStream, "Could not find " + configFilename + " in test class-path");
        }

        // Prepare Hsqldb DB
        if (HSQLDB_DATASOURCE_TYPE.equalsIgnoreCase(datasourcePlatform)) {

            dbDirectory = getHsqldbSrcDatabaseDirectory();
            if (StringUtils.isNotEmpty(configFileSuffix)) {
                dbDirectory += configFileSuffix;
            }

            if (canWrite()) {
                Properties p = new Properties();
                p.load(resourceAsStream);
                String jdbcUrl = p.getProperty(SumarisConfigurationOption.JDBC_URL.getKey());
                boolean serverMode = jdbcUrl != null && !Daos.isFileDatabase(jdbcUrl);

                // If running on server mode
                if (serverMode) {
                    // OK. Do not copy DB files

                    // Display a warn, if hsqldb
                    if (HSQLDB_DATASOURCE_TYPE.equalsIgnoreCase(datasourcePlatform)) {
                        log.warn(String.format("Database running in server mode! Please remove the property '%s' in file %s, to use a file database.",
                            SumarisConfigurationOption.JDBC_URL.getKey(), configFilename));
                    }
                }
                else {
                    Tests.checkDbExists(testClass, dbDirectory);
                    // Copy DB files into test directory
                    copyDb(new File(dbDirectory), "db", false, null);

                    // Update db directory with the new path
                    dbDirectory = new File(resourceDirectory, "db").getAbsolutePath();
                    dbDirectory = dbDirectory.replaceAll("[\\\\]", "/");
                }
            } else {
                Tests.checkDbExists(testClass, dbDirectory);
                // Load db config properties
                File dbConfig = new File(dbDirectory, getTestDbName() + ".properties");

                // Make readonly=true
                String readonly = getProperty(dbConfig, "readonly");
                Preconditions.checkNotNull(readonly, "Could not find readonly property on db config: " + dbConfig);
                Preconditions.checkState("true".equals(readonly), "readonly property must be at true value in read mode test in  db config: "
                        + dbConfig);
            }
        }

        // Initialize configuration
        initConfiguration(configFilename);
    }

    protected final Set<File> toDestroy = Sets.newHashSet();

    /**
     * <p>addToDestroy.</p>
     *
     * @param dir a {@link File} object.
     */
    public void addToDestroy(File dir) {
        toDestroy.add(dir);
    }

    public String getProperty(File file, String key) throws IOException {
        Properties p = new Properties();
        try (BufferedReader reader = Files.newReader(file, Charsets.UTF_8)) {
            p.load(reader);
            return p.getProperty(key);
        }
    }

    /**
     * <p>setProperty.</p>
     *
     * @param file a {@link File} object.
     * @param key a {@link String} object.
     * @param value a {@link String} object.
     * @throws IOException if any.
     */
    public void setProperty(File file, String key, String value) throws IOException {
        // Load old properties values
        Properties props = new Properties();
        BufferedReader reader = Files.newReader(file, Charsets.UTF_8);
        props.load(reader);
        reader.close();

        // Store new properties values
        props.setProperty(key, value);
        BufferedWriter writer = Files.newWriter(file, Charsets.UTF_8);
        props.store(writer, "");
        writer.flush();
        writer.close();
    }

    /**
     * <p>copyDb.</p>
     *
     * @param sourceDirectory a {@link File} object.
     * @param targetDbDirectoryName a {@link String} object.
     * @param readonly a boolean.
     * @param p a {@link Properties} object.
     * @throws IOException if any.
     */
    public void copyDb(File sourceDirectory, String targetDbDirectoryName, boolean readonly, Properties p) throws IOException {
        File targetDirectory = getResourceDirectory(targetDbDirectoryName);
        copyDb(sourceDirectory, targetDirectory, readonly, p, true);
    }

    /**
     * <p>copyDb.</p>
     *
     * @param sourceDirectory a {@link File} object.
     * @param targetDirectory a {@link File} object.
     * @param readonly a boolean.
     * @param p a {@link Properties} object.
     * @param destroyAfterTest a boolean.
     * @throws IOException if any.
     */
    public void copyDb(File sourceDirectory, File targetDirectory, boolean readonly, Properties p, boolean destroyAfterTest) throws IOException {
        if (!sourceDirectory.exists()) {

            if (log.isWarnEnabled()) {
                log.warn("Could not find db at " + sourceDirectory + ", test [" +
                        testClass + "] is skipped.");
            }
            Assume.assumeTrue(false);
        }

        if (p != null) {
            String jdbcUrl = Daos.getJdbcUrl(targetDirectory, getTestDbName());
            Daos.fillConnectionProperties(p, jdbcUrl, "SA", "");
        }

        // Add to destroy files list
        if (destroyAfterTest) {
            addToDestroy(targetDirectory);
        }

        log.debug(String.format("Copy directory %s at %s", sourceDirectory.getPath(), targetDirectory.getPath()));
        FileUtils.copyDirectory(sourceDirectory, targetDirectory);

        // Set readonly property
        log.debug(String.format("Set database properties with readonly=%s", readonly));
        File dbConfig = new File(targetDirectory, getTestDbName() + ".properties");
        setProperty(dbConfig, "readonly", String.valueOf(readonly));
    }

    /**
     * <p>after.</p>
     *
     * @param description a {@link Description} object.
     */
    protected void after(Description description) {
        if (log.isDebugEnabled()) {
            log.debug("After test " + testClass);
        }

        ServiceLocator serviceLocator = ServiceLocator.instance();

        // If service and database has been started
        if (serviceLocator.isOpen()) {
            Properties connectionProperties = SumarisConfiguration.getInstance().getConnectionProperties();

            // Shutdown if HSQLDB database is a file database (not server mode)
            if (Daos.isFileDatabase(Daos.getUrl(connectionProperties))) {
                try {
                    Daos.shutdownDatabase(connectionProperties);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Could not close database.", e);
                    }
                    withError = true;
                }
            }

            // Shutdown spring context
            serviceLocator.shutdown();
        }

        if (!withError) {
            destroyDirectories(toDestroy, true);
        }

    }

    /**
     * <p>getTestSpecificDirectory.</p>
     *
     * @param testClass a {@link Class} object.
     * @param name a {@link String} object.
     * @return a {@link File} object.
     * @throws IOException if any.
     */
    public static File getTestSpecificDirectory(Class<?> testClass,
                                                String name) throws IOException {
        // Trying to look for the temporary folder to store data for the test
        String tempDirPath = System.getProperty("java.io.tmpdir");
        if (tempDirPath == null) {
            // can this really occur ?
            tempDirPath = "";
            if (log.isWarnEnabled()) {
                log.warn("'\"java.io.tmpdir\" not defined");
            }
        }
        File tempDirFile = new File(tempDirPath);

        // create the directory to store database data
        String dataBasePath = testClass.getName()
                + File.separator // a directory with the test class name
                + name // a sub-directory with the method name
                + '_'
                + BUILD_TIMESTAMP; // and a timestamp
        File databaseFile = new File(tempDirFile, dataBasePath);
        FileUtils.forceMkdir(databaseFile);

        return databaseFile;
    }

    /**
     * <p>createEmptyDb.</p>
     *
     * @param dbDirectory a {@link String} object.
     * @param dbName a {@link String} object.
     * @return a {@link Connection} object.
     * @throws SQLException if any.
     */
    public Connection createEmptyDb(String dbDirectory,
            String dbName) throws SQLException {
        File externalDbFile = getResourceDirectory(dbDirectory);
        File scriptFile = new File(getHsqldbSrcCreateScript());
        return createEmptyDb(externalDbFile, dbName, null, scriptFile);
    }

    /**
     * <p>createEmptyDb.</p>
     *
     * @param dbDirectory a {@link String} object.
     * @param dbName a {@link String} object.
     * @param p a {@link Properties} object.
     * @return a {@link Connection} object.
     * @throws SQLException if any.
     */
    public Connection createEmptyDb(String dbDirectory,
            String dbName, Properties p) throws SQLException {
        File externalDbFile = getResourceDirectory(dbDirectory);
        File scriptFile = new File(getHsqldbSrcCreateScript());
        return createEmptyDb(externalDbFile, dbName, p, scriptFile);
    }

    /**
     * <p>createEmptyDb.</p>
     *
     * @param directory a {@link File} object.
     * @param dbName a {@link String} object.
     * @param p a {@link Properties} object.
     * @param scriptFile a {@link File} object.
     * @return a {@link Connection} object.
     * @throws SQLException if any.
     */
    protected Connection createEmptyDb(
            File directory,
            String dbName,
            Properties p,
            File scriptFile) throws SQLException {

        SumarisConfiguration config = SumarisConfiguration.getInstance();

        if (log.isDebugEnabled()) {
            log.debug("Create new db at " + directory);
        }
        addToDestroy(directory);
        String jdbcUrl = Daos.getJdbcUrl(directory, dbName);
        String user = "SA";
        String password = "";

        p = (p == null) ? config.getConnectionProperties() : p;
        Daos.fillConnectionProperties(p, jdbcUrl, user, password);

        Preconditions.checkState(scriptFile.exists(), "Could not find db script at " + scriptFile);

        DatabaseSchemaDao schemaDao = new DatabaseSchemaDaoImpl(config);
        schemaDao.generateNewDb(directory, true, scriptFile, p, true/*isTemporaryDb*/);
        Connection connection = Daos.createConnection(jdbcUrl, user, password);

        if (log.isDebugEnabled()) {
            log.debug("Created connection at " + connection.getMetaData().getURL());
        }
        return connection;
    }

    /**
     * <p>getDatasourcePlatform.</p>
     *
     * @return a {@link String} object.
     */
    public String getDatasourcePlatform() {
        return getDatasourcePlatform(this.datasourcePlatform);
    }

    /**
     * -- protected methods--
     *
     * @param defaultValue a {@link String} object.
     * @return a {@link String} object.
     */
    protected String getDatasourcePlatform(String defaultValue) {
        String result = System.getProperty("spring.datasource.platform");

        // Check validity
        if (result == null && StringUtils.isNotBlank(defaultValue)) {
            result = defaultValue;
            log.debug("Could not find build environment. Please add -Dspring.datasource.platform=<hsqldb|oracle|pgsql>. Test [{}] will use default environment : {}", testClass, defaultValue);
        } else if (!"hsqldb".equals(result)
                && !"oracle".equals(result)
                && !"pgsql".equals(result)) {

            if (log.isWarnEnabled()) {
                log.warn("Could not find build environment. Please add -Dspring.datasource.platform=<hsqldb|oracle|pgsql>. Test [{}] will be skipped.", testClass);
            }
            Assume.assumeTrue(false);
        }
        return result;
    }

    /**
     * <p>getConfigArgs.</p>
     *
     * @return an array of {@link String} objects.
     */
    protected String[] getConfigArgs() {
        List<String> configArgs = Lists.newArrayList();
        configArgs.addAll(Lists.newArrayList(
                "--option", SumarisConfigurationOption.BASEDIR.getKey(), resourceDirectory.getAbsolutePath()));
        if (dbDirectory != null) {
            configArgs.addAll(Lists.newArrayList("--option", SumarisConfigurationOption.DB_DIRECTORY.getKey(), dbDirectory));
        }

        // Push sequence to 1000
        if (canWrite()) {
            configArgs.addAll(Lists.newArrayList("--option", SumarisConfigurationOption.SEQUENCE_START_WITH.getKey(), String.valueOf(1000)));
        }

        return configArgs.toArray(new String[0]);
    }

    /**
     * Convenience methods that could be override to initialize other configuration
     *
     * @param configFilename a {@link String} object.
     */
    protected void initConfiguration(String configFilename) {
        TestConfiguration.createConfiguration(configFilename, getConfigArgs());
    }

    /**
     * <p>getTestDbName.</p>
     *
     * @return a {@link String} object.
     */
    protected String getTestDbName() {
        return HSQLDB_SRC_DATABASE_NAME;
    }

    /* -- Internal methods -- */

    private void destroyDirectories(Set<File> toDestroy, boolean retry) {
        if (CollectionUtils.isEmpty(toDestroy)) {
            return;
        }

        Set<File> directoriesToRetry = Sets.newHashSet(toDestroy);
        for (File file : toDestroy) {
            if (file.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("Destroy directory: " + file);
                }
                try {
                    FileUtils.deleteDirectory(file);

                } catch (IOException e) {
                    if (retry) {
                        if (log.isErrorEnabled()) {
                            log.error("Could not delete directory: " + file + ". Will retry later.");
                        }
                        directoriesToRetry.add(file);
                    }
                    else {
                        if (log.isErrorEnabled()) {
                            log.error("Could not delete directory: " + file + ". Please delete it manually.");
                        }
                    }
                }
            }
        }

        if (retry && CollectionUtils.isEmpty(directoriesToRetry)) {
            destroyDirectories(directoriesToRetry, false);
        }
    }
}
