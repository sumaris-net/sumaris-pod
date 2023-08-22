package net.sumaris.core.dao.technical.liquibase;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
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


import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.diff.output.report.DiffToReport;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.structure.core.DatabaseObjectFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.hibernate.HibernateConnectionProvider;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.cfg.Environment;
import org.nuiton.i18n.I18n;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Liquibase class.</p>
 */
@Component
@Slf4j
public class Liquibase implements ResourceLoaderAware {

    /** Constant <code>CHANGE_LOG_SNAPSHOT_SUFFIX="-SNAPSHOT.xml"</code> */
    private final static String CHANGE_LOG_SNAPSHOT_SUFFIX = "-SNAPSHOT.xml";

    private ResourceLoader resourceLoader;

    private DataSource dataSource;

    private final SumarisConfiguration configuration;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    private String defaultSchema;

    private String contexts;

    private Map<String, String> parameters;

    private Version maxChangeLogFileVersion;

    /**
     * Constructor used by Spring
     *
     * @param dataSource a {@link DataSource} object.
     * @param configuration a {@link SumarisConfiguration} object.
     */
    @Autowired
    public Liquibase(DataSource dataSource, SumarisConfiguration configuration) {
        this.dataSource = dataSource;
        this.configuration = configuration;
    }

    /**
     * Constructor used when Spring is not started (no datasource, and @Resource not initialized)
     *
     * @param configuration a {@link SumarisConfiguration} object.
     */
    public Liquibase(SumarisConfiguration configuration) {
        this.dataSource = null;
        this.configuration = configuration;
        // Init change log
        setChangeLog(configuration.getLiquibaseChangeLogPath());
    }

    /**
     *
     * Executed automatically when the bean is initialized.
     */
    @PostConstruct
    public void init() throws LiquibaseException {

        // Update the change log path, from configuration
        setChangeLog(configuration.getLiquibaseChangeLogPath());

        // Default schema
        setDefaultSchema(configuration.getJdbcSchema());

        // Compute the max changelog file version
        computeMaxChangeLogFileVersion();

    }

    /**
     * <p>Getter for the field <code>dataSource</code>.</p>
     *
     * @return The DataSource that liquibase will use to perform the migration.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * The DataSource that liquibase will use to perform the migration.
     *
     * @param dataSource a {@link DataSource} object.
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * <p>Getter for the field <code>changeLog</code>.</p>
     *
     * @return a Resource that is able to resolve to a file or classpath resource.
     */
    public String getChangeLog() {
        return changeLog;
    }

    /**
     * Sets a Spring Resource that is able to resolve to a file or classpath resource.
     * An example might be <code>classpath:db-changelog.xml</code>.
     *
     * @param dataModel a {@link String} object.
     */
    public void setChangeLog(String dataModel) {
        if (this.changeLog == null)
            this.changeLog = dataModel;
    }

    /**
     * <p>Getter for the field <code>contexts</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getContexts() {
        return contexts;
    }

    /**
     * <p>Setter for the field <code>contexts</code>.</p>
     *
     * @param contexts a {@link String} object.
     */
    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    /**
     * <p>Getter for the field <code>defaultSchema</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getDefaultSchema() {
        return defaultSchema;
    }

    /**
     * <p>Setter for the field <code>defaultSchema</code>.</p>
     *
     * @param defaultSchema a {@link String} object.
     */
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    /**
     * Execute liquibase update, using change log
     *
     * @throws LiquibaseException if any.
     */
    public void executeUpdate() throws LiquibaseException {
        executeUpdate(null);
    }

    /**
     * Execute liquibase update, using change log
     *
     * @param connectionProperties the properties for connection
     * @throws LiquibaseException if any.
     */
    public void executeUpdate(Properties connectionProperties) throws LiquibaseException {

        Connection c = null;
        liquibase.Liquibase liquibase;
        try {
            // open connection
            c = createConnection(connectionProperties);

            log.info(I18n.t("sumaris.persistence.liquibase.executeUpdate", c.getMetaData().getURL()));

            // create liquibase instance
            liquibase = createLiquibase(c);

            // First, release locks, then update and release locks again
            liquibase.forceReleaseLocks();
            performUpdate(liquibase);
            liquibase.forceReleaseLocks();

            // Compact database
            if (configuration.useLiquibaseCompact()) {
                Daos.compactDatabase(c);
            }

        } catch (SQLException e) {
            log.error(I18n.t("sumaris.persistence.liquibase.executeUpdate.error", e.getMessage()));
            throw new DatabaseException(e);
        } finally {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException e) {
                    // nothing to do
                }
                releaseConnection(c);
            }
        }

    }

    /**
     * <p>performUpdate.</p>
     *
     * @param liquibase a {@link liquibase.Liquibase} object.
     * @throws LiquibaseException if any.
     */
    protected void performUpdate(liquibase.Liquibase liquibase) throws LiquibaseException {
        liquibase.update(getContexts());
    }

    /**
     * Execute liquibase status, using change log
     *
     * @param writer a {@link Writer} object.
     * @throws LiquibaseException if any.
     */
    public void reportStatus(Writer writer) throws LiquibaseException {

        Connection c = null;
        liquibase.Liquibase liquibase;
        Writer myWriter = null;
        try {
            // open connection
            c = createConnection();

            // create liquibase instance
            liquibase = createLiquibase(c);

            // First, release locks, then update and release locks again
            liquibase.forceReleaseLocks();
            if (writer != null) {
                performReportStatus(liquibase, writer);
            } else {
                myWriter = new OutputStreamWriter(System.out);
                performReportStatus(liquibase, myWriter);
            }
            liquibase.forceReleaseLocks();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException e) {
                    // nothing to do
                }
                releaseConnection(c);
            }
            if (myWriter != null) {
                try {
                    myWriter.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }

    }

    /**
     * <p>performReportStatus.</p>
     *
     * @param liquibase a {@link liquibase.Liquibase} object.
     * @param writer    a {@link Writer} object.
     * @throws LiquibaseException if any.
     */
    protected void performReportStatus(liquibase.Liquibase liquibase, Writer writer) throws LiquibaseException {
        liquibase.reportStatus(true, getContexts(), writer);
    }

    /**
     * <p>createLiquibase.</p>
     *
     * @param c a {@link Connection} object.
     * @return a {@link liquibase.Liquibase} object.
     * @throws LiquibaseException if any.
     */
    protected liquibase.Liquibase createLiquibase(Connection c) throws LiquibaseException {
        // Remove 'classpath:' prefix
        String adjustedChangeLog = adjustNoClasspath(getChangeLog());

        // If Spring not started: remove 'files:' prefixes
        if (this.resourceLoader == null) {
            adjustedChangeLog = adjustNoFilePrefix(adjustedChangeLog);
        }

        liquibase.Liquibase liquibase = new liquibase.Liquibase(adjustedChangeLog, createResourceAccessor(), createDatabase(c));
        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                liquibase.setChangeLogParameter(entry.getKey(), entry.getValue());
            }
        }

        return liquibase;
    }

    /**
     * Subclasses may override this method add change some database settings such as
     * default schema before returning the database object.
     *
     * @param c a {@link Connection} object.
     * @return a Database implementation retrieved from the {@link DatabaseFactory}.
     * @throws DatabaseException if any.
     */
    protected Database createDatabase(Connection c) throws DatabaseException {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
        if (StringUtils.trimToNull(this.defaultSchema) != null
            && !Daos.isHsqlDatabase(c)) {
            database.setDefaultSchemaName(this.defaultSchema);
        }
        return database;
    }

    /**
     * Create a database connection to hibernate model.
     * This is useful for diff report
     *
     * @return a {@link Database} object.
     * @throws DatabaseException if any.
     */
    protected Database createHibernateDatabase() throws DatabaseException {

        // To be able to retrieve connection from datasource
        HibernateConnectionProvider.setDataSource(dataSource);

        ResourceAccessor accessor = new ClassLoaderResourceAccessor(this.getClass().getClassLoader());

        return CommandLineUtils.createDatabaseObject(accessor,
            "hibernate:classic:hibernate.cfg.xml",
            null,
            null,
            null,
            configuration.getJdbcCatalog(),
            configuration.getJdbcSchema(),
            false, false,
            null,
            null,
            null, null, null, null, null
        );
    }

    /**
     * <p>setChangeLogParameters.</p>
     *
     * @param parameters a {@link Map} object.
     */
    public void setChangeLogParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Create a new resourceAccessor.
     *
     * @return a {@link ResourceAccessor} object.
     */
    protected ResourceAccessor createResourceAccessor() {
        // Classpath resource accessor
        if (isClasspathPrefixPresent(changeLog)) {
            return new ClassLoaderResourceAccessor(this.getClass().getClassLoader());
        }

        // File resource accessor
        return new FileSystemResourceAccessor(new File(adjustNoFilePrefix(changeLog)).getParentFile());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * <p>Getter for the field <code>resourceLoader</code>.</p>
     *
     * @return a {@link ResourceLoader} object.
     */
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + "(" + this.getResourceLoader().toString() + ")";
    }

    /**
     * <p>computeMaxChangeLogFileVersion.</p>
     */
    protected void computeMaxChangeLogFileVersion() {
        this.maxChangeLogFileVersion = null;

        // Get the changelog path
        String changeLogPath = getChangeLog();
        if (StringUtils.isBlank(changeLogPath)) {
            return;
        }

        // Secure all separator (need for regex)
        changeLogPath = changeLogPath.replaceAll("\\\\", "/");

        // Get the parent folder path
        int index = changeLogPath.lastIndexOf('/');
        if (index == -1 || index == changeLogPath.length() - 1) {
            return;
        }

        // Compute a regex (based from changelog master file)
        String changeLogWithVersionRegex = changeLogPath.substring(index + 1);
        changeLogWithVersionRegex = changeLogWithVersionRegex.replaceAll("master\\.xml", "([0-9]\\\\.[.-_a-zA-Z]+)\\\\.xml");
        Pattern changeLogWithVersionPattern = Pattern.compile(changeLogWithVersionRegex);

        Version maxVersion = null;

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);

        try {
            // Get resources from classpath
            String pathPrefix = changeLogPath.substring(0, index);
            Resource[] resources = resolver.getResources(pathPrefix + "/**/db-changelog-*.xml"); // WARNING: '**/' is mandatory, for multi-dbms (e.g. sumaris-core-server)
            if (ArrayUtils.isNotEmpty(resources)) {
                for (Resource resource : resources) {
                    String filename = resource.getFilename();
                    Assert.notNull(filename);
                    Matcher matcher = changeLogWithVersionPattern.matcher(filename);

                    // If the filename match the changelog with version pattern
                    if (matcher.matches()) {
                        String fileVersion = matcher.group(1);
                        // Skip SNAPSHOT versions
                        if (!fileVersion.endsWith(CHANGE_LOG_SNAPSHOT_SUFFIX)) {
                            try {
                                Version version = VersionBuilder.create(fileVersion).build();

                                // Store a version has max if need
                                if (maxVersion == null || maxVersion.before(version)) {
                                    maxVersion = version;
                                }
                            } catch (IllegalArgumentException iae) {
                                // Bad version format : log but continue
                                log.warn(
                                        String.format(
                                                "Bad format version found in file: %s/%s. Ignoring this file when computing the max schema version.",
                                                changeLogPath, filename));
                            }
                        }
                    }
                }
            }
            else {
                log.warn(String.format("No changelog files with version found. Please check master changelog file exists at [%s]", changeLogPath));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not find changelog files", e);
        }

        if (maxVersion != null) {
            this.maxChangeLogFileVersion = maxVersion;
        }
    }

    /**
     * Get the max version from all change log files.
     * change log file with version must have a same pattern as the master changelog
     *
     * @return the max version founded in files, or null if version found
     */
    public Version getMaxChangeLogFileVersion() {
        return this.maxChangeLogFileVersion;
    }

    /**
     * Generate a diff report (using text format)
     *
     * @param outputFile a {@link File} object.
     * @param typesToControl
     *            a comma separated database object to check (i.e Table, View, Column...). If null, all types are
     *            checked
     * @throws LiquibaseException if any.
     */
    public void reportDiff(File outputFile, String typesToControl) throws LiquibaseException {
        Connection c = null;
        liquibase.Liquibase liquibase;
        PrintStream writer = null;
        try {
            // open connection
            c = createConnection();

            // create liquibase instance
            liquibase = createLiquibase(c);

            // First, release locks, then update and release locks again
            liquibase.forceReleaseLocks();
            DiffResult diffResult = performDiff(liquibase, typesToControl);
            liquibase.forceReleaseLocks();

            // Write the result into report file
            writer = outputFile != null ? new PrintStream(outputFile) : null;

            new DiffToReport(diffResult, writer != null ? writer : System.out)
                    .print();

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } catch (FileNotFoundException e) {
            throw new SumarisTechnicalException( "Could not write diff report file.", e);
        } finally {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException e) {
                    // nothing to do
                }
                releaseConnection(c);
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Generate a changelog file, with all diff found
     *
     * @param changeLogFile  a {@link File} object.
     * @param typesToControl a comma separated database object to check (i.e Table, View, Column...). If null, all types are
     *                       checked
     * @throws LiquibaseException if any.
     */
    public void generateDiffChangelog(File changeLogFile, String typesToControl) throws LiquibaseException {
        Connection c = null;
        liquibase.Liquibase liquibase;
        PrintStream writer = null;
        try {
            // open connection
            c = createConnection();

            // create liquibase instance
            liquibase = createLiquibase(c);

            DiffResult diffResult;
            // First, release locks, then update and release locks again
            // (only if not in a transaction - because it can be a read-only transaction)
            if (!DataSourceUtils.isConnectionTransactional(c, dataSource)) {
                liquibase.forceReleaseLocks();
                diffResult = performDiff(liquibase, typesToControl);
                liquibase.forceReleaseLocks();
            }
            else {
                diffResult = performDiff(liquibase, typesToControl);
            }

            // Write the result into report file
            writer = changeLogFile != null ? new PrintStream(changeLogFile) : null;

            DiffOutputControl diffOutputControl = new DiffOutputControl(false, false, false, null);
            new DiffToChangeLog(diffResult, diffOutputControl)
                    .print(writer != null ? writer : System.out);

        } catch (SQLException e) {
            throw new DatabaseException(e);
        } catch (ParserConfigurationException | IOException e) {
            throw new SumarisTechnicalException( "Could not generate changelog file.", e);
        } finally {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException e) {
                    // nothing to do
                }
                releaseConnection(c);
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * <p>performDiff.</p>
     *
     * @param liquibase
     *            the connection to the target database
     * @param typesToControl
     *            a comma separated database object to check (i.e Table, View, Column...). If null, all types are
     *            checked
     * @return the diff result
     * @throws LiquibaseException if any.
     */
    protected DiffResult performDiff(liquibase.Liquibase liquibase, String typesToControl) throws LiquibaseException {
        Database referenceDatabase = createHibernateDatabase();
        CompareControl compareControl = new CompareControl(DatabaseObjectFactory.getInstance().parseTypes(typesToControl));
        return liquibase.diff(referenceDatabase, liquibase.getDatabase(), compareControl);
    }

    /**
     * <p>createConnection.</p>
     *
     * @return a {@link Connection} object.
     * @throws SQLException if any.
     */
    protected Connection createConnection() throws SQLException {
        if (dataSource != null) {
            return DataSourceUtils.getConnection(dataSource);
        }
        return Daos.createConnection(configuration.getConnectionProperties());
    }

    /**
     * Create a connection from the given properties.<p/>
     * If JDBC Url is equals to the datasource, use the datsource to create the connection
     *
     * @param connectionProperties a {@link Properties} object.
     * @throws SQLException if any.
     * @return a {@link Connection} object.
     */
    protected Connection createConnection(Properties connectionProperties) throws SQLException {
        Properties targetConnectionProperties = (connectionProperties != null) ? connectionProperties : configuration.getConnectionProperties();
        String jdbcUrl = targetConnectionProperties.getProperty(Environment.URL);
        if (Objects.equals(configuration.getJdbcURL(), jdbcUrl) && dataSource != null) {
            return DataSourceUtils.getConnection(dataSource);
        }
        return Daos.createConnection(targetConnectionProperties);
    }

    /**
     * <p>releaseConnection.</p>
     *
     * @param conn a {@link Connection} object.
     */
    protected void releaseConnection(Connection conn) {
        if (dataSource != null) {
            DataSourceUtils.releaseConnection(conn, dataSource);
            return;
        }
        Daos.closeSilently(conn);
    }

    /**
     * <p>adjustNoClasspath.</p>
     *
     * @param file a {@link String} object.
     * @return a {@link String} object.
     */
    protected String adjustNoClasspath(String file) {
        return isClasspathPrefixPresent(file)
            ? file.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length())
            : file;
    }

    /**
     * <p>isClasspathPrefixPresent.</p>
     *
     * @param file a {@link String} object.
     * @return a boolean.
     */
    protected boolean isClasspathPrefixPresent(String file) {
        return file.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
    }

    /**
     * <p>isFilePrefixPresent.</p>
     *
     * @param file a {@link String} object.
     * @return a boolean.
     */
    protected boolean isFilePrefixPresent(String file) {
        return file.startsWith(ResourceUtils.FILE_URL_PREFIX);
    }

    /**
     * <p>adjustNoFilePrefix.</p>
     *
     * @param file a {@link String} object.
     * @return a {@link String} object.
     */
    protected String adjustNoFilePrefix(String file) {
        return isFilePrefixPresent(file)
            ? file.substring(ResourceUtils.FILE_URL_PREFIX.length())
            : file;
    }

}
