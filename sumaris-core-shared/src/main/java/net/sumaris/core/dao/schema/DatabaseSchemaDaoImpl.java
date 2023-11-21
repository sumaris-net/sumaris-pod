package net.sumaris.core.dao.schema;

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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import liquibase.exception.LiquibaseException;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.hibernate.HibernateConnectionProvider;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.hibernate.HibernateImplicitNamingStrategy;
import net.sumaris.core.dao.technical.hibernate.HibernatePhysicalNamingStrategy;
import net.sumaris.core.dao.technical.liquibase.Liquibase;
import net.sumaris.core.exception.DatabaseSchemaUpdateException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.VersionNotFoundException;
import net.sumaris.core.model.annotation.Comment;
import net.sumaris.core.util.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.nuiton.i18n.I18n;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.function.Predicate;

/**
 * <p>DatabaseSchemaDaoImpl class.</p>
 */
@Repository("databaseSchemaDao")
@Lazy
@Slf4j
public class DatabaseSchemaDaoImpl
        extends HibernateDaoSupport
        implements DatabaseSchemaDao {

    private final Liquibase liquibase;

    private DataSource dataSource;

    /**
     * Constructor used by Spring
     *
     * @param config a {@link SumarisConfiguration} config.
     * @param entityManager a {@link EntityManager} object.
     * @param dataSource a {@link DataSource} object.
     * @param liquibase a {@link Liquibase} object.
     */
    @Autowired
    public DatabaseSchemaDaoImpl(SumarisConfiguration config,
                                 EntityManager entityManager,
                                 DataSource dataSource,
                                 Liquibase liquibase) {
        super(config);
        setEntityManager(entityManager);
        this.dataSource = dataSource;
        this.liquibase = liquibase;
    }

    /**
     * Constructor to use when Spring not started
     *
     * @param config a {@link SumarisConfiguration} object.
     */
    public DatabaseSchemaDaoImpl(SumarisConfiguration config) {
        super(config);
        this.liquibase = new Liquibase(config);
    }

    /**
     * Constructor to use when Spring not started
     *
     * @param config a {@link SumarisConfiguration} object.
     * @param liquibase a {@link Liquibase} object.
     */
    public DatabaseSchemaDaoImpl(SumarisConfiguration config, Liquibase liquibase) {
        super(config);
        this.liquibase = liquibase;
    }

    /**
     * {@inheritDoc}
     *
     * Executed automatically when the bean is initialized.
     */
    @PostConstruct
    protected void init() {

        // check database and server timezones conformity
        try {
            checkTimezoneConformity();
        } catch (SQLException e) {
            throw new SumarisTechnicalException("Could not check database timezone", e);
        }

        if (log.isInfoEnabled()) {
            try {
                Version schemaVersion = getSchemaVersion();
                if (schemaVersion != null) {
                    log.info(I18n.t("sumaris.persistence.schemaVersion", schemaVersion.toString()));
                }
            } catch (VersionNotFoundException | PersistenceException e) {
                // silent
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void generateCreateSchemaFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename could not be null or empty.");
        }
        generateCreateSchemaFile(filename, false, false, true);
    }

    /** {@inheritDoc} */
    @Override
    public void generateCreateSchemaFile(String filename, boolean doExecute, boolean withDrop, boolean withCreate) {
        Metadata metadata = getMetadata();
        new SchemaExport()
            .setDelimiter(";")
            .setOutputFile(filename)
            .execute(EnumSet.of(TargetType.SCRIPT),
                withDrop ? SchemaExport.Action.BOTH : SchemaExport.Action.CREATE,
                metadata
            );

        // Add table and columns comment
        try {
            appendRemarks(filename, metadata);
        } catch (Exception e) {
            // FIXME java.lang.NullPointerException: Cannot invoke "org.hibernate.boot.model.relational.QualifiedName.getCatalogName()" because "name" is null
            // throw new SumarisTechnicalException("Error when appending comments on file", e);
            log.error("Failed to export remarks: {}", e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void generateUpdateSchemaFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename could not be null or empty.");
        }
        generateUpdateSchemaFile(filename, false);
    }

    /** {@inheritDoc} */
    @Override
    public void generateUpdateSchemaFile(String filename, boolean doUpdate) {
        EnumSet<TargetType> targets = doUpdate ?
            EnumSet.of(TargetType.SCRIPT, TargetType.DATABASE) :  EnumSet.of(TargetType.SCRIPT);

        SchemaUpdate task = new SchemaUpdate();
        task.setDelimiter(";");
        task.setOutputFile(filename);
        task.execute(targets, getMetadata());
    }

    /** {@inheritDoc} */
    @Override
    public void updateSchema() throws DatabaseSchemaUpdateException {
        updateSchema(getConfig().getConnectionProperties());
    }

    /** {@inheritDoc} */
    @Override
    public void updateSchema(Properties connectionProperties) throws DatabaseSchemaUpdateException {
        try {
            liquibase.executeUpdate(connectionProperties);


        } catch (LiquibaseException le) {
            if (log.isErrorEnabled()) {
                log.error(le.getMessage(), le);
            }
            throw new DatabaseSchemaUpdateException("Could not update schema", le);
        }

        Version schemaVersion = null;
        try {
            schemaVersion = getSchemaVersion();
        }
        catch(VersionNotFoundException e) {
            // Continue
        }
        log.info(I18n.t("sumaris.persistence.liquibase.executeUpdate.success") +
            (schemaVersion != null ? (" " + I18n.t("sumaris.persistence.schemaVersion",  schemaVersion)) : ""));

    }

    /** {@inheritDoc} */
    @Override
    public void updateSchema(File dbDirectory) throws DatabaseSchemaUpdateException {
        // Preparing connection properties
        Properties connectionProperties = getConfig().getConnectionProperties();
        connectionProperties.setProperty(Environment.URL, Daos.getJdbcUrl(dbDirectory, getConfig().getDbName()));

        // Run update
        updateSchema(connectionProperties);
    }

    /** {@inheritDoc} */
    @Override
    public void generateStatusReport(File outputFile) throws IOException {
        FileWriter fw = new FileWriter(outputFile);
        try {
            liquibase.reportStatus(fw);
        } catch (LiquibaseException le) {
            if (log.isErrorEnabled()) {
                log.error(le.getMessage(), le);
            }
            throw new SumarisTechnicalException("Could not report database status", le);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void generateDiffReport(File outputFile, String typesToControl) {
        try {
            liquibase.reportDiff(outputFile, typesToControl);
        } catch (LiquibaseException le) {
            if (log.isErrorEnabled()) {
                log.error(le.getMessage(), le);
            }
            throw new SumarisTechnicalException("Could not report database diff", le);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void generateDiffChangeLog(File outputChangeLogFile, String typesToControl) {
        try {
            liquibase.generateDiffChangelog(outputChangeLogFile, typesToControl);
        } catch (LiquibaseException le) {
            if (log.isErrorEnabled()) {
                log.error(le.getMessage(), le);
            }
            throw new SumarisTechnicalException("Could not create database diff changelog", le);
        }
    }


    /** {@inheritDoc} */
    @Override
    public Version getSchemaVersion() throws VersionNotFoundException {
        String systemVersion;
        try {
            EntityManager em = getEntityManager();
            if (em == null) {
                throw new VersionNotFoundException("Could not find the schema version. No entityManager found");
            }
            systemVersion = em.createNamedQuery("SystemVersion.last", String.class).getSingleResult();
            if (StringUtils.isBlank(systemVersion)) {
                throw new VersionNotFoundException("Could not find the schema version. No version found in SYSTEM_VERSION table.");
            }
        } catch (HibernateException | NoResultException e) {
            throw new VersionNotFoundException(String.format("Could not find the schema version: %s", e.getMessage()));
        }
        try {
            return VersionBuilder.create(systemVersion).build();
        } catch (IllegalArgumentException iae) {
            throw new VersionNotFoundException(String.format("Could not find the schema version. Bad schema version found table SYSTEM_VERSION: %s",
                systemVersion));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Version getSchemaVersionIfUpdate() {
        return liquibase.getMaxChangeLogFileVersion();
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldUpdateSchema() throws VersionNotFoundException {
        return getSchemaVersion().compareTo(getSchemaVersionIfUpdate()) >= 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDbLoaded() {

        // Do not try to run the validation query if the DB not exists
        if (!isDbExists()) {
            log.warn("Unable to check if database is empty or not: database directory not exists");
            return false;
        }

        Connection connection;
        try {
            connection = DataSourceUtils.getConnection(getDataSource());
        }
        catch(CannotGetJdbcConnectionException ex) {
            log.error("Unable to find JDBC connection from dataSource", ex);
            return false;
        }

        // Retrieve a validation query, from configuration
        String dbValidationQuery = getConfig().getDbValidationQuery();
        if (StringUtils.isBlank(dbValidationQuery)) {
            DataSourceUtils.releaseConnection(connection, getDataSource());
            return true;
        }

        log.debug(String.format("Check if the database is loaded, using validation query: %s", dbValidationQuery));

        // try to execute the validation query
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute(dbValidationQuery);
        } catch (SQLException ex) {
            log.error(String.format("Error while executing validation query [%s]: %s", dbValidationQuery, ex.getMessage()));
            return false;
        }
        finally {
            Daos.closeSilently(stmt);
            DataSourceUtils.releaseConnection(connection, getDataSource());
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDbExists() {
        String jdbcUrl = getConfig().getJdbcURL();

        if (!Daos.isFileDatabase(jdbcUrl)) {
            return true;
        }

        File f = new File(getConfig().getDbDirectory(), getConfig().getDbName() + ".script");
        return f.exists();
    }

    /** {@inheritDoc} */
    @Override
    public void generateNewDb(File dbDirectory, boolean replaceIfExists) {
        Preconditions.checkNotNull(dbDirectory);
        // Preparing connection properties
        Properties connectionProperties = getConfig().getConnectionProperties();
        connectionProperties.setProperty(Environment.URL, Daos.getJdbcUrl(dbDirectory, getConfig().getDbName()));

        // Run Db creation
        generateNewDb(dbDirectory, replaceIfExists, null, connectionProperties, false);
    }

    /** {@inheritDoc} */
    @Override
    public void generateNewDb(File dbDirectory, boolean replaceIfExists, File scriptFile, Properties connectionProperties, boolean isTemporaryDb) {
        Preconditions.checkNotNull(dbDirectory);

        // Log target connection
        if (log.isInfoEnabled()) {
            log.info(I18n.t("sumaris.persistence.newEmptyDatabase.directory", dbDirectory));
        }
        // Check output directory validity
        if (dbDirectory.exists() && !dbDirectory.isDirectory()) {
            throw new SumarisTechnicalException(
                I18n.t("sumaris.persistence.newEmptyDatabase.notValidDirectory.error", dbDirectory));
        }

        // Make sure the directory could be created
        try {
            FileUtils.forceMkdir(dbDirectory);
        } catch (IOException e) {
            throw new SumarisTechnicalException(
                I18n.t("sumaris.persistence.newEmptyDatabase.mkdir.error", dbDirectory),
                e);
        }

        if (ArrayUtils.isNotEmpty(dbDirectory.listFiles())) {
            if (replaceIfExists) {
                log.info(I18n.t("sumaris.persistence.newEmptyDatabase.deleteDirectory", dbDirectory));
                try {
                    FileUtils.deleteDirectory(dbDirectory);
                } catch (IOException e) {
                    throw new SumarisTechnicalException(
                        I18n.t("sumaris.persistence.newEmptyDatabase.deleteDirectory.error", dbDirectory), e);
                }
            }
            else {
                throw new SumarisTechnicalException(
                    I18n.t("sumaris.persistence.newEmptyDatabase.notEmptyDirectory.error", dbDirectory));
            }
        }

        // Get connections properties :
        Properties targetConnectionProperties = connectionProperties != null ? connectionProperties : getConfig().getConnectionProperties();

        // Check connections
        if (!checkConnection(targetConnectionProperties)) {
            return;
        }

        try {
            // Create the database
            createEmptyDb(getConfig(), targetConnectionProperties, scriptFile, isTemporaryDb);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            throw new SumarisTechnicalException(
                I18n.t("sumaris.persistence.newEmptyDatabase.create.error"),
                e);
        }

        try {
            // Shutdown database at end
            Daos.shutdownDatabase(targetConnectionProperties);
        } catch (SQLException e) {
            throw new SumarisTechnicalException(
                I18n.t("sumaris.persistence.newEmptyDatabase.shutdown.error"),
                e);
        }
    }


    /* -- Internal methods --*/


    /**
     * <p>checkConnection.</p>
     *
     * @param targetConnectionProperties a {@link Properties} object.
     * @return a boolean.
     */
    protected boolean checkConnection(
        Properties targetConnectionProperties) {

        // Log target connection
        if (log.isInfoEnabled()) {
            log.info("Connecting to target database...\n" + Daos.getLogString(targetConnectionProperties));
        }

        // Check target connection
        boolean isValidConnection = Daos.isValidConnectionProperties(targetConnectionProperties);
        if (!isValidConnection) {
            log.error("Connection error: could not connect to target database.");
            return false;
        }

        return true;
    }

    /**
     * <p>createEmptyDb.</p>
     *
     * @param config a {@link SumarisConfiguration} object.
     * @param targetConnectionProperties a {@link Properties} object.
     * @param scriptFile a {@link File} object.
     * @param isTemporaryDb a boolean.
     * @throws SQLException if any.
     * @throws IOException if any.
     */
    private void createEmptyDb(SumarisConfiguration config, Properties targetConnectionProperties, File scriptFile, boolean isTemporaryDb) throws SQLException, IOException {
        // Getting the script file
        String scriptPath = scriptFile == null ? config.getDbCreateScriptPath() : scriptFile.getAbsolutePath();
        Preconditions
            .checkArgument(
                StringUtils.isNotBlank(scriptPath),
                String.format(
                    "No path for the DB script has been set in the configuration. This is need to create a new database. Please set the option [%s] in configuration file.",
                    SumarisConfigurationOption.DB_CREATE_SCRIPT_PATH));
        scriptPath = scriptPath.replaceAll("\\\\", "/");

        // Make sure the path is an URL (if not, add "file:" prefix)
        String scriptPathWithPrefix = scriptPath;
        if (!org.springframework.util.ResourceUtils.isUrl(scriptPath)) {
            scriptPathWithPrefix = org.springframework.util.ResourceUtils.FILE_URL_PREFIX + scriptPath;
        }


        Resource scriptResource = ResourceUtils.getResource(scriptPathWithPrefix);
        if (scriptResource.exists()) {
            if (log.isInfoEnabled()) {
                log.info("Will use create script at: " + scriptPath);
            }
        }
        else {

            if (log.isInfoEnabled()) {
                log.info("Generating create script...");
            }

            // No script file: try to generate script
            try {
                scriptFile = File.createTempFile("script", ".tmp.sql");
                generateCreateSchemaFile(scriptFile.getAbsolutePath(), false, false, true);
                scriptResource = ResourceUtils.getResource(org.springframework.util.ResourceUtils.FILE_URL_PREFIX + scriptFile.getAbsolutePath());

                log.debug("Will use generated script at: " + scriptFile);

            } catch(IOException e){
                throw new SumarisTechnicalException(String.format("Could not find DB script file, at %s", scriptPath));
            }
        }

        Connection connection = Daos.createConnection(targetConnectionProperties);
        Daos.setTimezone(connection, config.getDbTimezone());
        try {
            List<String> importScriptSql = getImportScriptSql(scriptResource);
            for (String sql : importScriptSql) {
                PreparedStatement statement = null;
                try {
                    statement = connection.prepareStatement(sql);
                    statement.execute();
                } catch (SQLException sqle) {
                    log.warn("SQL command failed : " + sql, sqle);
                    throw sqle;
                } finally {
                    Daos.closeSilently(statement);
                }

            }
            connection.commit();
        } finally {
            Daos.closeSilently(connection);
        }
    }

    /**
     * <p>getImportScriptSql.</p>
     *
     * @param scriptResource a {@link Resource} object.
     * @return a {@link List} object.
     * @throws IOException if any.
     */
    protected List<String> getImportScriptSql(Resource scriptResource) throws IOException {

        List<String> result = Lists.newArrayList();

        Predicate<String> predicate = new Predicate<String>() {

            final Set<String> includedStarts = Sets.newHashSet(
                "INSERT INTO DATABASECHANGELOG ");

            final Set<String> excludedStarts = Sets.newHashSet(
                "SET ",
                "CREATE USER ",
                "ALTER USER ", // for HslDB 2.3+
                "CREATE SCHEMA ",
                "GRANT DBA TO ",
                "GRANT USAGE ON ",
                "INSERT INTO ", // In case there is memory tables
                "CREATE FUNCTION ",
                "ALTER SPECIFIC ROUTINE "
            );

            @Override
            public boolean test(String input) {
                boolean accept = true;
                for (String forbiddenStart : excludedStarts) {
                    if (input.startsWith(forbiddenStart)
                        // Allow this instructions
                        && !input.startsWith("SET WRITE_DELAY")       // for HslDB 1.8+
                        && !input.startsWith("SET FILES WRITE DELAY") // for HslDB 2.3+
                    ) {
                        accept = false;
                        break;
                    }
                }
                if (!accept) {
                    for (String forbiddenStart : includedStarts) {
                        if (input.startsWith(forbiddenStart)) {
                            accept = true;
                            break;
                        }
                    }
                }
                return accept;
            }
        };


        try (InputStream is = scriptResource.getInputStream()) {
            Iterator<String> lines = IOUtils.lineIterator(is, Charsets.UTF_8);

            int sequenceStartWithValue = getConfig().getSequenceStartWithValue();
            while (lines.hasNext()) {
                String line = lines.next().trim().toUpperCase();
                if (predicate.test(line)) {
                    if (line.contains("\\U000A")) {
                        line = line.replaceAll("\\\\U000A", "\n");
                    }

                    // Reset sequence to initial value
                    if (line.startsWith("CREATE SEQUENCE") || line.startsWith("ALTER SEQUENCE")) {
                        line = line.replaceAll("START WITH [0-9]+", "START WITH " + sequenceStartWithValue);
                    }

                    // Use cached table
                    if (line.startsWith("CREATE TABLE")
                        || line.startsWith("CREATE MEMORY TABLE")) {
                        line = line.replaceAll("CREATE (MEMORY )?TABLE", "CREATE CACHED TABLE");
                    }

                    // Always use TEMP_QUERY_PARAMETER as standard CACHED table
                    // (no more use a GLOBAL TEMPORARY table, because JDBC metdata could not be extract)
                    if (line.matches("CREATE [ A-Z_-]*TABLE [.A-Z_-]*TEMP_QUERY_PARAMETER\\(.*")) {
                        line = line.replaceAll("CREATE [ A-Z_-]*TABLE [.A-Z_-]*TEMP_QUERY_PARAMETER", "CREATE CACHED TABLE TEMP_QUERY_PARAMETER");
                    }

                    if (StringUtils.isNotBlank(line)) {
                        result.add(line);
                    }
                }
            }
        }
        return result;
    }

    protected Map<String, Object> getSessionSettings(boolean configureHibernateConnectionProvider) {
        if (getEntityManager() != null) {
            SessionFactory session = getEntityManager().unwrap(Session.class).getSessionFactory();

            if (session != null) {
                // Allow Hibernate to get the connection
                if (configureHibernateConnectionProvider) {
                    HibernateConnectionProvider.setDataSource(getDataSource());
                }
                return session.getProperties();
            }
        }

        return getSessionSettings(getConfig().getConnectionProperties(), configureHibernateConnectionProvider);
    }
    protected Map<String, Object> getSessionSettings(@Nullable Properties connectionProperties, boolean configureHibernateConnectionProvider) {


        // Allow Hibernate to get the connection
        if (configureHibernateConnectionProvider) {
            try {
                Connection conn = Daos.createConnection(connectionProperties);
                HibernateConnectionProvider.setConnection(conn);
            } catch (SQLException e) {
                throw new SumarisTechnicalException("Could not open connection: " + connectionProperties.get(Environment.URL));
            }
        }

        Map<String, Object> sessionSettings = Maps.newHashMap();
        sessionSettings.put(Environment.DIALECT, connectionProperties.get(Environment.DIALECT));
        sessionSettings.put(Environment.DRIVER, connectionProperties.get(Environment.DRIVER));
        sessionSettings.put(Environment.URL, connectionProperties.get(Environment.URL));
        sessionSettings.put(Environment.IMPLICIT_NAMING_STRATEGY, HibernateImplicitNamingStrategy.class.getName());

        sessionSettings.put(Environment.PHYSICAL_NAMING_STRATEGY, HibernatePhysicalNamingStrategy.class.getName());
        //sessionSettings.put(Environment.PHYSICAL_NAMING_STRATEGY, "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");

        return sessionSettings;
    }

    protected Metadata getMetadata() {
        return getMetadata(getSessionSettings(true));
    }

    protected Metadata getMetadata(Map<String, Object> sessionSettings) {

        MetadataSources metadata = new MetadataSources(new StandardServiceRegistryBuilder()
            .applySettings(sessionSettings)
            .applySetting(Environment.CONNECTION_PROVIDER, HibernateConnectionProvider.class.getName())
            .build());

        // Add annotations entities
        Reflections reflections = (getConfig().isProduction() ? Reflections.collect() : new Reflections(getConfig().getHibernateEntitiesPackage()));
        reflections.getTypesAnnotatedWith(Entity.class)
            .forEach(metadata::addAnnotatedClass);

        return metadata.buildMetadata();
    }

    /**
     * Check server and database timezones conformity
     * Warn if offsets differs
     */
    private void checkTimezoneConformity() throws SQLException {

        // find server timezone
        TimeZone serverTimeZone = TimeZone.getDefault();
        log.info(I18n.t("sumaris.persistence.serverTimeZone", new Timestamp(new Date().getTime()), serverTimeZone.getID()));

        // find db timezone offset in time format ex: '1:00' for 1 hour offset
        String dbOffsetAsString = (String) Daos.sqlUnique(getDataSource(), getTimezoneQuery(getDataSource().getConnection()));
        log.info(I18n.t("sumaris.persistence.dbTimeZone", getDatabaseCurrentDate(), dbOffsetAsString));

        // convert db time zone offset in raw offset in milliseconds
        int dbOffset = Integer.parseInt(dbOffsetAsString.substring(0, dbOffsetAsString.lastIndexOf(":"))) * 3600 * 1000;

        // compare both offsets
        if (dbOffset != serverTimeZone.getRawOffset()) {
            // warn if offsets differs
            log.warn(I18n.t("sumaris.persistence.differentTimeZone"));
        }
    }

    private String getTimezoneQuery(Connection connection) {
        if (Daos.isHsqlDatabase(connection)) {
            return "CALL DATABASE_TIMEZONE()";
        }
        if (Daos.isOracleDatabase(connection)) {
            return "SELECT DBTIMEZONE FROM DUAL";
        }
        if (Daos.isPostgresqlDatabase(connection)){
            return "SELECT TO_CHAR(age(now() at time zone 'UTC', now()), 'HH24:MI');";
        }
        throw new SumarisTechnicalException("Cannot generate Timezone query : not implemented for this database type");
    }

    private void appendRemarks(String filename, Metadata metadata) throws SQLException, IOException {
        List<String> linesToAppend = new ArrayList<>();

        // Prepare hibernate connection (will be used by buildSessionFactory() )
        Connection connection;
        if (dataSource != null) {
            connection = DataSourceUtils.getConnection(dataSource);
            HibernateConnectionProvider.setDataSource(dataSource);
        }
        else {
            connection = Daos.createConnection(getConfig().getConnectionProperties());
            HibernateConnectionProvider.setConnection(connection);
        }

        try (SessionFactory sf = metadata.buildSessionFactory()) {
            String schemaName = connection.getSchema();
            sf.getMetamodel()
                .getEntities()
                .stream()
                .filter(entityType -> entityType.getName() != null)
                .sorted(Comparator.comparing(EntityType::getName))
                .forEach(entityType -> {
                    Table table = entityType.getJavaType().getAnnotation(Table.class);
                    Comment tableComment = entityType.getJavaType().getAnnotation(Comment.class);
                    if (table != null) {
                        if (tableComment != null) {
                            Optional.ofNullable(getTableCommentQuery(schemaName, table.name(), tableComment.value())).ifPresent(linesToAppend::add);
                        }
                        // iterate attributes
                        entityType.getAttributes().stream()
                            .filter(attribute -> attribute.getName() != null)
                            .sorted(Comparator.comparing(Attribute::getName))
                            .forEach(attribute -> {
                                if (attribute.getJavaMember() instanceof Field) {
                                    Field field = (Field) attribute.getJavaMember();
                                    Column column = field.getAnnotation(Column.class);
                                    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                                    Comment columnComment = field.getAnnotation(Comment.class);
                                    String columnName = Optional.ofNullable(column).map(Column::name).orElse(
                                        Optional.ofNullable(joinColumn).map(JoinColumn::name).orElse(null)
                                    );
                                    if (columnName != null && columnComment != null) {
                                        Optional.ofNullable(getColumnCommentQuery(schemaName, table.name(), columnName, columnComment.value())).ifPresent(linesToAppend::add);
                                    }
                                }
                            });
                    }
                });
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        if (!linesToAppend.isEmpty()) {
            Files.write(Paths.get(filename), linesToAppend, StandardOpenOption.APPEND);
        }
    }

    private String getTableCommentQuery(String schemaName, String tableName, String comment) {
        if (isOracleDialect()) {
            return String.format("comment on table %s.%s is '%s';", schemaName, tableName, comment.replaceAll("'", "''"));
        }
        return null;
    }

    private String getColumnCommentQuery(String schemaName, String tableName, String columnName, String comment) {
        if (isOracleDialect()) {
            return String.format("comment on column %s.%s.%s is '%s';", schemaName, tableName, columnName, comment.replaceAll("'", "''"));
        }
        return null;
    }

    private boolean isOracleDialect() {
        try {
            return getHibernateDialect() instanceof Oracle10gDialect;
        }
        catch (InstantiationException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    private Dialect getHibernateDialect() throws InstantiationException {
        EntityManager em = getEntityManager();
        if (em != null) {
            return Daos.getDialect(em);
        }
        String dialectClassName = getConfig().getHibernateDialect();
        if (StringUtils.isBlank(dialectClassName)) return null;
        try {
            Class dialectClass = Class.forName(getConfig().getHibernateDialect());
            return ((Dialect) dialectClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new InstantiationException(String.format("Cannot instantiate class %s: %s", dialectClassName, e.getMessage()));
        }
    }
}
