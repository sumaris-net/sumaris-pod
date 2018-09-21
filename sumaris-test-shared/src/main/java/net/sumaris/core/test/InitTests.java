package net.sumaris.core.test;

/*-
 * #%L
 * SUMARiS :: Sumaris Test Shared
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
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.schema.DatabaseSchemaDao;
import net.sumaris.core.dao.schema.DatabaseSchemaDaoImpl;
import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.service.ServiceLocator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author peck7 on 13/10/2017.
 */
public class InitTests extends ExternalResource {

    private static final String DATASET_COMMON_XML_FILE = "sumaris.test.data.common";
    private static final String DATASET_ADDITIONAL_XML_FILES = "sumaris.test.data.additional";

    private static final Log log = LogFactory.getLog(InitTests.class);

    /**
     * Main method is used by clients projects, to generate and deploy a test DB
     * (e.g. for Dali project see http://doc.e-is.pro/dali/dali-data.properties)
     * @param args arguments
     * @throws Throwable if any
     */
    public static void main(String[] args) throws Throwable{

        // Check arguments
        if (ArrayUtils.isEmpty(args)) {
            log.error("Missing target directory, as first argument of InitTests.main(). Skipping");
            System.exit(-1);
        }

        InitTests initTests = new InitTests();

        try {

            // arg[0] : target DB directory
            String targetDbArg = args[0];
            initTests.setTargetDbDirectory(targetDbArg);

            // arg[1] : replace if exists
            if (args.length >= 2) {
                boolean replaceIfExists = Boolean.parseBoolean(args[1]);
                initTests.setReplaceDbIfExists(replaceIfExists);
            }

            log.info(String.format("Creating test database into [%s]", initTests.getTargetDbDirectory()));

            // Execute the DB creation + data load
            initTests.compactDatabase = true;
            initTests.before();
        } catch (Throwable ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw ex;
        }
    }

    protected SumarisConfiguration config;

    private String targetDbDirectory = null;

    private boolean replaceDbIfExists = false;

    private boolean compactDatabase = false;

    public void setTargetDbDirectory(String targetDbDirectory) {
        this.targetDbDirectory = targetDbDirectory;
    }

    public String getTargetDbDirectory() {
        return targetDbDirectory;
    }

    public void setReplaceDbIfExists(boolean replaceDbIfExists) {
        this.replaceDbIfExists = replaceDbIfExists;
    }

    public boolean getReplaceDbIfExists() {
        return replaceDbIfExists;
    }

    protected String getDbEnumerationResource() {
        return "classpath*:sumaris-db-enumerations.properties";
    }

    protected String getModuleName() {
        return "sumaris-test-shared";
    }

    protected String[] getConfigArgs() {
        return new String[]{
                "--option", SumarisConfigurationOption.DB_DIRECTORY.getKey(), getTargetDbDirectory(),
                "--option", SumarisConfigurationOption.JDBC_URL.getKey(), SumarisConfigurationOption.JDBC_URL.getDefaultValue(),
                "--option", SumarisConfigurationOption.DB_ENUMERATION_RESOURCE.getKey(), getDbEnumerationResource()
        };
    }

    protected SumarisConfiguration createConfig() {

        SumarisConfiguration config = new SumarisConfiguration(getModuleName() + "-test.properties",
                getConfigArgs()
        );
        SumarisConfiguration.setInstance(config);
        return config;

    }

    protected void initServiceLocator() {

        ServiceLocator.initDefault();
    }

    @Override
    protected void before() throws Throwable {

        config = createConfig();
        Assume.assumeNotNull(config);

        initServiceLocator();

        initI18n();

        boolean isFileDatabase = Daos.isFileDatabase(config.getJdbcURL());
        boolean needSchemaUpdate = true;

        if (isFileDatabase) {
            log.info("Init test data in database... [" + config.getJdbcURL() + "]");

            File dbDirectory = new File(getTargetDbDirectory());
            File dbConfigFile = new File(dbDirectory, DatabaseResource.HSQLDB_SRC_DATABASE_PROPERTIES_FILE);
            File dbScriptFile = new File(dbDirectory, DatabaseResource.HSQLDB_SRC_DATABASE_SCRIPT_FILE);

            // db not exists: create it
            if (!dbConfigFile.exists() || !dbScriptFile.exists() || replaceDbIfExists) {
                // Create DB
                generateNewDb(dbDirectory, replaceDbIfExists);

                // Update schema
                updateSchema(dbDirectory);
                needSchemaUpdate = false;
            }

            // Set database to readonly=false
            try {
                setProperty(dbConfigFile, "readonly", "false");
            } catch (IOException e) {
                Assume.assumeNoException(e);
            }
        }

        Connection conn = null;
        try {
            // Update DB schema
            if (needSchemaUpdate) {
                //log.info("Updating database schema...");
                // TODO: replace the service locator ?
                //ServiceLocator.instance().getDatabaseSchemaService().updateSchema();
            }

            conn = Daos.createConnection(config.getConnectionProperties());

            // Import Common dataset
            try {
                String commonDataSetFile = config.getApplicationConfig().getOption(DATASET_COMMON_XML_FILE);
                Assume.assumeTrue(
                        String.format("Missing value for configuration option [%s].\nPlease set this properties in the test configuration.",
                                DATASET_COMMON_XML_FILE),
                        commonDataSetFile != null);

                URL commonDataSetFileUrl = getClass().getResource("/" + commonDataSetFile);
                Assume.assumeTrue(
                        String.format("Unable to find resource for configuration option [%s] resource = %s. \nPlease review your properties in the test configuration.",
                                DATASET_COMMON_XML_FILE, commonDataSetFile),
                        commonDataSetFileUrl != null);

                // Prepare Database (e.g. disabling constraints, ...)
                beforeInsert(conn);

                // Delete all
                log.info(String.format("Deleting data, from tables found in file {%s}...", commonDataSetFile));
                deleteAllFromXmlDataSet(commonDataSetFileUrl, conn);

                afterInsert(conn);
                beforeInsert(conn);

                // Insert common data
                log.info(String.format("Importing data from file {%s}...", commonDataSetFile));
                insertFromXmlDataSet(commonDataSetFileUrl, conn);

                conn.commit();
            } finally {
                afterInsert(conn);
            }

            // DEV ONLY: on server DB, cleaning all previous database change log
            // if (!isFileDatabase) {
            // Daos.sqlUpdate(conn, "DELETE FROM DATABASECHANGELOG");
            // Daos.sqlUpdate(conn, "DELETE FROM DATABASECHANGELOGLOCK");
            // conn.commit();
            // }

            // Import additional datasets
            try {

                String importFileNames = config.getApplicationConfig().getOption(DATASET_ADDITIONAL_XML_FILES);
                Assume.assumeTrue(
                        String.format("Missing value for configuration option [%s].\nPlease set this properties in the test configuration.",
                                DATASET_ADDITIONAL_XML_FILES),
                        importFileNames != null);

                // Prepare Database (e.g. disabling constraints, ...)
                beforeInsert(conn);

                // If multiple files, split and loop over
                for(String importFileName : Splitter.on(',').split(importFileNames.trim())) {
                    log.info(String.format("Importing data from file {%s}...", importFileName));
                    URL importFileUrl = getClass().getResource("/" + importFileName.trim());
                    Assume.assumeTrue(
                            String.format("Unable to find resource for configuration option [%s] resource = %s. \nPlease review your properties in the test configuration.",
                                    DATASET_ADDITIONAL_XML_FILES, importFileName),
                            importFileUrl != null);

                    // Insert
                    insertFromXmlDataSet(importFileUrl, conn);
                };

                // Committing insertions
                conn.commit();

            } finally {
                // Restoring constraints
                afterInsert(conn);
            }

        } finally {

            if (conn != null && !conn.isClosed()) {
                if (isFileDatabase) {

                    // Shutdown database
                    if (compactDatabase) {
                        Daos.compactDatabase(conn);
                    }

                    // Shutdown database
                    Daos.shutdownDatabase(conn);
                }

                Daos.closeSilently(conn);
            }

            // Shutdown spring context
            IOUtils.closeQuietly(ServiceLocator.instance());
        }

        // Set database to readonly=true
        if (isFileDatabase) {
            File dbDirectory = new File(getTargetDbDirectory());
            File dbConfigFile = new File(dbDirectory, DatabaseResource.HSQLDB_SRC_DATABASE_PROPERTIES_FILE);

            try {
                setProperty(dbConfigFile, "readonly", "true");
            } catch (IOException e) {
                Assume.assumeNoException(e);
            }
        }

        log.info("Test database has been loaded");
    }

    public static void setProperty(File file, String key, String value) throws IOException {
        // Load old properties values
        Properties props = new Properties();
        try (BufferedReader reader = Files.newReader(file, Charsets.UTF_8)) {
            props.load(reader);
        }

        // Store new properties values
        props.setProperty(key, value);
        try (BufferedWriter writer = Files.newWriter(file, Charsets.UTF_8)) {
            props.store(writer, "");
        }
    }

    public void insertFromXmlDataSet(URL fileURL, Connection conn) throws SQLException, DatabaseUnitException {

        IDatabaseConnection connection = createDbUnitConnection(conn);
        IDataSet dataSet = new FlatXmlDataSetBuilder()
                .setColumnSensing(true)
                .build(fileURL);
        DatabaseOperation.INSERT.execute(connection, dataSet);
    }

    public void deleteAllFromXmlDataSet(URL fileURL, Connection conn) throws SQLException, DatabaseUnitException {

        IDatabaseConnection connection = createDbUnitConnection(conn);
        IDataSet dataSet = new FlatXmlDataSetBuilder()
                .setColumnSensing(true)
                .build(fileURL);
        DatabaseOperation.DELETE_ALL.execute(connection, dataSet);
    }

	/* -- internal methods -- */

    protected void initI18n() throws IOException {
        SumarisConfiguration config = SumarisConfiguration.getInstance();

        // --------------------------------------------------------------------//
        // init i18n
        // --------------------------------------------------------------------//
        File i18nDirectory = new File(config.getDataDirectory(), "i18n");
        if (i18nDirectory.exists()) {
            // clean i18n cache
            FileUtils.cleanDirectory(i18nDirectory);
        }

        FileUtils.forceMkdir(i18nDirectory);

        if (log.isDebugEnabled()) {
            log.debug("I18N directory: " + i18nDirectory);
        }

        Locale i18nLocale = config.getI18nLocale();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Starts i18n with locale [%s] at [%s]",
                    i18nLocale, i18nDirectory));
        }
        I18n.init(new UserI18nInitializer(
                        i18nDirectory, new DefaultI18nInitializer(getI18nBundleName())),
                i18nLocale);
    }

    protected String getI18nBundleName() {
        return getModuleName() + "-i18n";
    }

    protected void generateNewDb(File outputDirectory, boolean replaceDbIfExists) {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        DatabaseSchemaDao databaseSchemaDao = new DatabaseSchemaDaoImpl(config);

        try {
            // Create the database
            databaseSchemaDao.generateNewDb(outputDirectory, replaceDbIfExists);
        } catch (SumarisTechnicalException e) {
            log.error(e.getMessage());
            Assume.assumeNoException(e);
        }
    }

    protected void updateSchema(File outputDirectory) {
        SumarisConfiguration config = SumarisConfiguration.getInstance();
        DatabaseSchemaDao databaseSchemaDao = new DatabaseSchemaDaoImpl(config);

        try {
            // Update the DB schema
            databaseSchemaDao.updateSchema(outputDirectory);
        } catch (SumarisTechnicalException | DatabaseSchemaUpdateException e) {
            log.error(e.getMessage());
            Assume.assumeNoException(e);
        }
    }

    protected IDatabaseConnection createDbUnitConnection(Connection jdbcConnection) throws DatabaseUnitException {

        IDatabaseConnection dbUnitConnection;

        // HsqldDB connecion
        if (Daos.isHsqlDatabase(config.getJdbcURL())) {
            dbUnitConnection = new DatabaseConnection(jdbcConnection);
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
        }
        // Oracle connecion
        else if (Daos.isOracleDatabase(config.getJdbcURL())){
            dbUnitConnection = new DatabaseConnection(jdbcConnection, config.getJdbcSchema());
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
            dbUnitConnection.getConfig().setProperty(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, Boolean.TRUE);
        }
        else {
            throw new SumarisTechnicalException("Unable to create DBUnit connection: Unknown DB type for URL [" + config.getJdbcURL() + "]");
        }

        return dbUnitConnection;
    }

    protected void beforeInsert(Connection connection) throws SQLException {
        // Disable integrity constraints
        log.debug("Disabling database constraints...");
        Daos.setIntegrityConstraints(connection, false);

    }

    protected void afterInsert(Connection connection) throws SQLException {

        // Enable integrity constraints
        log.debug("Enabling database constraints...");
        Daos.setIntegrityConstraints(connection, true);
    }

}
