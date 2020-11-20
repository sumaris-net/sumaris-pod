package net.sumaris.core.config;

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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ApplicationConfigHelper;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ArgumentsParserException;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.nuiton.i18n.I18n.t;

/**
 * Access to configuration options
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public class SumarisConfiguration extends PropertyPlaceholderConfigurer {
    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(SumarisConfiguration.class);

    private static final String DEFAULT_SHARED_CONFIG_FILE = "sumaris-core-shared.config";

    protected static String[] args = null;

    /**
     * <p>remember app args.</p>
     */
    public static void setArgs(String[] sourceArgs) {
        args = sourceArgs;
    }

    /**
     * Delegate application config.
     */
    protected final ApplicationConfig applicationConfig;

    private static SumarisConfiguration instance;

    /**
     * <p>Getter for the field <code>instance</code>.</p>
     *
     * @return a {@link SumarisConfiguration} object.
     */
    public static SumarisConfiguration getInstance() {
        return instance;
    }

    /**
     * <p>Setter for the field <code>instance</code>.</p>
     *
     * @param instance a {@link SumarisConfiguration} object.
     */
    public static void setInstance(SumarisConfiguration instance) {
        SumarisConfiguration.instance = instance;
    }

    private File configFile;

    /**
     * <p>initDefault.</p>
     */
    public static void initDefault(String configFile) {
        instance = new SumarisConfiguration(configFile, args);
        setInstance(instance);
    }

    /**
     * <p>Constructor for SumarisConfiguration.</p>
     *
     * @param applicationConfig a {@link ApplicationConfig} object.
     */
    public SumarisConfiguration(ApplicationConfig applicationConfig) {
        super();
        this.applicationConfig = applicationConfig;
    }

    /**
     * <p>Constructor for SumarisConfiguration.</p>
     *
     * @param file a {@link String} object.
     * @param args a {@link String} object.
     */
    public SumarisConfiguration(String file, String... args) {
        super();

        this.applicationConfig = new ApplicationConfig();
        this.applicationConfig.setEncoding(Charsets.UTF_8.name());
        this.applicationConfig.setConfigFileName(file);


        System.setProperty("logging.level.Hibernate Types", "error");

        // find all config providers
        Set<ApplicationConfigProvider> providers =
                ApplicationConfigHelper.getProviders(null,
                        null,
                        null,
                        true);

        // load all default options
        ApplicationConfigHelper.loadAllDefaultOption(applicationConfig,
                providers);

        // Load actions
        for (ApplicationConfigProvider provider : providers) {
            applicationConfig.loadActions(provider.getActions());
        }

        // Define Alias
        addAlias(applicationConfig);

        // Override some external module default config (sumaris)
        overrideExternalModulesDefaultOptions(applicationConfig);

        // parse config file and inline arguments
        try {
            applicationConfig.parse(args);

        } catch (ArgumentsParserException e) {
            throw new SumarisTechnicalException(t("sumaris.config.parse.error"), e);
        }

        // Init the application version
        initVersion(applicationConfig);

        // Init time zone
        initTimeZone();

        // TODO Review this, this is very dirty to do this...
        File appBasedir = applicationConfig.getOptionAsFile(
                SumarisConfigurationOption.BASEDIR.getKey());

        if (appBasedir == null) {
            appBasedir = new File("");
        }
        if (!appBasedir.isAbsolute()) {
            appBasedir = new File(appBasedir.getAbsolutePath());
        }
        if (appBasedir.getName().equals("..")) {
            appBasedir = appBasedir.getParentFile().getParentFile();
        }
        if (appBasedir.getName().equals(".")) {
            appBasedir = appBasedir.getParentFile();
        }
        if (log.isInfoEnabled()) {
            String appName = applicationConfig.getOption(SumarisConfigurationOption.APP_NAME.getKey());
            log.info(String.format("Starting {%s} on basedir {%s}", appName, appBasedir));
        }
        applicationConfig.setOption(
                SumarisConfigurationOption.BASEDIR.getKey(),
                appBasedir.getAbsolutePath());

        if (log.isDebugEnabled())
            log.debug(applicationConfig.getPrintableConfig(null, 4));

    }

    /**
     * Add alias to the given ApplicationConfig. <p/>
     * This method could be override to add specific alias
     *
     * @param applicationConfig a {@link ApplicationConfig} object.
     */
    protected void addAlias(ApplicationConfig applicationConfig) {
        applicationConfig.addAlias("-u", "--option", SumarisConfigurationOption.JDBC_USERNAME.getKey());
        applicationConfig.addAlias("--user", "--option", SumarisConfigurationOption.JDBC_USERNAME.getKey());
        applicationConfig.addAlias("-p", "--option", SumarisConfigurationOption.JDBC_PASSWORD.getKey());
        applicationConfig.addAlias("--password", "--option", SumarisConfigurationOption.JDBC_PASSWORD.getKey());
        applicationConfig.addAlias("-db", "--option", SumarisConfigurationOption.JDBC_URL.getKey());
        applicationConfig.addAlias("--database", "--option", SumarisConfigurationOption.JDBC_URL.getKey());

        applicationConfig.addAlias("--output", "--option", SumarisConfigurationOption.CLI_OUTPUT_FILE.getKey());
        applicationConfig.addAlias("-f", "--option", SumarisConfigurationOption.CLI_FORCE_OUTPUT.getKey(), "true");

    }

    // Could be subclasses
    /**
     * <p>overrideExternalModulesDefaultOptions.</p>
     *
     * @param applicationConfig a {@link ApplicationConfig} object.
     */
    protected void overrideExternalModulesDefaultOptions(ApplicationConfig applicationConfig) {

    }

    /**
     * Initialization default timezone, from configuration (mantis #24623)
     */
    protected void initTimeZone() {

        String dbTimeZone = applicationConfig.getOption(SumarisConfigurationOption.DB_TIMEZONE.getKey());
        if (StringUtils.isNotBlank(dbTimeZone)) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Using timezone [%s] for database", dbTimeZone));
            }
        } else if (log.isInfoEnabled()) {
            log.info(String.format("Using default timezone [%s] for database", System.getProperty("user.timezone")));
        }
    }


    /**
     * Make sure the version default value is the implementation version (using a properties file, filtered by Maven
     * at build time). This avoid to manually update the version default value, in enumeration SumarisConfigurationOption)
     *
     * @param applicationConfig a {@link ApplicationConfig} object.
     */
    protected void initVersion(ApplicationConfig applicationConfig) {

        try {
            // Load the properties file, from classpath
            Properties sharedConfigFile = new Properties();
            InputStream fis = getClass().getClassLoader().getResourceAsStream(DEFAULT_SHARED_CONFIG_FILE);
            sharedConfigFile.load(fis);
            fis.close();

            // If version property has been filled, use it as default version
            String defaultVersion = applicationConfig.getOption(SumarisConfigurationOption.VERSION.getKey());
            String implementationVersion = sharedConfigFile.getProperty(SumarisConfigurationOption.VERSION.getKey());
            if (StringUtils.isNotBlank(implementationVersion)
                    && StringUtils.isNotBlank(defaultVersion)
                    && !Objects.equals(implementationVersion, defaultVersion)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Replace default version option value [%s] with implementation value [%s] found in file [%s]",
                            defaultVersion,
                            implementationVersion,
                            DEFAULT_SHARED_CONFIG_FILE));
                }
                applicationConfig.setDefaultOption(
                        SumarisConfigurationOption.VERSION.getKey(),
                        implementationVersion);
            }
            else if (StringUtils.isNotBlank(implementationVersion)) {
                if (log.isInfoEnabled()) {
                    log.info("Version: " + implementationVersion);
                }
                applicationConfig.setDefaultOption(
                        SumarisConfigurationOption.VERSION.getKey(),
                        implementationVersion);
            }
            else if (StringUtils.isNotBlank(defaultVersion)) {
                if (log.isInfoEnabled()) {
                    log.info("Version: " + defaultVersion);
                }
            }
            else if (log.isErrorEnabled()) {
                log.error(String.format("Could init version, from classpath file [%s]", DEFAULT_SHARED_CONFIG_FILE));
            }
        } catch (IOException e) {
            log.warn(String.format("Could not load implementation version from file [%s]", DEFAULT_SHARED_CONFIG_FILE));
        }

    }



    /**
     * <p>Getter for the field <code>configFile</code>.</p>
     *
     * @return a {@link File} object.
     */
    public File getConfigFile() {
        if (configFile == null) {
            File dir = getBasedir();
            if (dir == null || !dir.exists()) {
                dir = new File(applicationConfig.getUserConfigDirectory());
            }
            configFile = new File(dir, applicationConfig.getConfigFileName());
        }
        return configFile;
    }

    /**
     * <p>getBasedir.</p>
     *
     * @return a {@link File} object.
     */
    public File getBasedir() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.BASEDIR.getKey());
    }

    /**
     * <p>getDataDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getDataDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.DATA_DIRECTORY.getKey());
    }

    /**
     * <p>Getter for the field <code>applicationConfig</code>.</p>
     *
     * @return a {@link ApplicationConfig} object.
     */
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    /** {@inheritDoc} */
    @Override
    protected String resolvePlaceholder(String placeholder, Properties props) {
        if (applicationConfig == null) {
            throw new BeanInitializationException(
                    "Configuration.applicationConfig must not be null. Please initialize Configuration instance with a not null applicationConfig BEFORE starting Spring.");
        }

        // Try to resolve placeholder from application configuration
        String optionValue = applicationConfig.getOption(placeholder);
        if (optionValue != null) {
            return optionValue;
        }

        // If not found in configuration, delegate to the default Spring mecanism
        return super.resolvePlaceholder(placeholder, props);
    }

    /**
     * <p>getTempDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getTempDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.TMP_DIRECTORY.getKey());
    }

    /**
     * <p>getDbDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getDbDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.DB_DIRECTORY.getKey());
    }

    /**
     * <p>setDbDirectory.</p>
     *
     * @param dbDirectory a {@link File} object.
     */
    public void setDbDirectory(File dbDirectory) {
        applicationConfig.setOption(SumarisConfigurationOption.DB_DIRECTORY.getKey(), dbDirectory.getPath());
    }

    /**
     * <p>setJdbcUrl.</p>
     *
     * @param jdbcUrl a {@link String} object.
     */
    public void setJdbcUrl(String jdbcUrl) {
        applicationConfig.setOption(SumarisConfigurationOption.JDBC_URL.getKey(), jdbcUrl);
    }

    /**
     * <p>getDbTimezone.</p>
     *
     * @return a {@link TimeZone} object.
     */
    public TimeZone getDbTimezone() {
        String tz = applicationConfig.getOption(SumarisConfigurationOption.DB_TIMEZONE.getKey());
        return StringUtils.isNotBlank(tz) ? TimeZone.getTimeZone(tz) : TimeZone.getDefault();
    }

    /**
     * <p>getDbAttachmentDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getDbAttachmentDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.DB_ATTACHMENT_DIRECTORY.getKey());
    }


    /**
     * <p>getDbBackupDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getDbBackupDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.DB_BACKUP_DIRECTORY.getKey());
    }

    /**
     * <p>getDbTrashDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getTrashDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.TRASH_DIRECTORY.getKey());
    }

    /**
     * <p>useLiquibaseAutoRun.</p>
     *
     * @return a boolean.
     */
    public boolean useLiquibaseAutoRun() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.LIQUIBASE_RUN_AUTO.getKey());
    }

    /**
     * <p>useCompactAfterLiquibase.</p>
     *
     * @return a boolean.
     */
    public boolean useLiquibaseCompact() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.LIQUIBASE_RUN_COMPACT.getKey());
    }

    /**
     * <p>getLiquibaseChangeLogPath.</p>
     *
     * @return a {@link String} object.
     */
    public String getLiquibaseChangeLogPath() {
        return applicationConfig.getOption(SumarisConfigurationOption.LIQUIBASE_CHANGE_LOG_PATH.getKey());
    }

    /**
     * <p>getDbCreateScriptPath.</p>
     *
     * @return a {@link String} object.
     */
    public String getDbCreateScriptPath() {
        return applicationConfig.getOption(SumarisConfigurationOption.DB_CREATE_SCRIPT_PATH.getKey());
    }

    /**
     * <p>getHibernateDialect.</p>
     *
     * @return a {@link String} object.
     */
    public String getHibernateDialect() {
        return applicationConfig.getOption(SumarisConfigurationOption.HIBERNATE_DIALECT.getKey());
    }

    /**
     * <p>getHibernateClientQueriesFile.</p>
     *
     * @return a {@link String} object.
     */
    public String getHibernateEntitiesPackage() {
        return applicationConfig.getOption(SumarisConfigurationOption.HIBERNATE_ENTITIES_PACKAGE.getKey());
    }

    /**
     * <p>getDatasourceJndiName.</p>
     *
     * @return a {@link String} object.
     */
    public String getDatasourceJndiName() {
        return applicationConfig.getOption(SumarisConfigurationOption.DATASOURCE_JNDI_NAME.getKey());
    }

    /**
     * <p>getJdbcDriver.</p>
     *
     * @return a {@link String} object.
     */
    public String getJdbcDriver() {
        return applicationConfig.getOption(SumarisConfigurationOption.JDBC_DRIVER.getKey());
    }

    /**
     * <p>getJdbcURL.</p>
     *
     * @return a {@link String} object.
     */
    public String getJdbcURL() {
        return applicationConfig.getOption(SumarisConfigurationOption.JDBC_URL.getKey());
    }

    /**
     * <p>getJdbcCatalog.</p>
     *
     * @return a {@link String} object.
     */
    public String getJdbcCatalog() {
        return applicationConfig.getOption(SumarisConfigurationOption.JDBC_CATALOG.getKey());
    }

    /**
     * <p>getJdbcSchema.</p>
     *
     * @return a {@link String} object.
     */
    public String getJdbcSchema() {
        return applicationConfig.getOption(SumarisConfigurationOption.JDBC_SCHEMA.getKey());
    }

    /**
     * <p>debugEntityLoad.</p>
     *
     * @return a boolean.
     */
    public boolean debugEntityLoad() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.DEBUG_ENTITY_LOAD.getKey());
    }

    /**
     * Enable trash of delete entities (e.g. Trip, Operation, etc)
     * @return
     */
    public boolean enableEntityTrash() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_ENTITY_TRASH.getKey());
    }

    public void setEnableTrash(boolean enable) {
        applicationConfig.setOption(SumarisConfigurationOption.ENABLE_ENTITY_TRASH.getKey(), String.valueOf(enable));
    }

    /**
     * <p>getDbName.</p>
     *
     * @return a {@link String} object.
     */
    public String getDbName() {
        return applicationConfig.getOption(SumarisConfigurationOption.DB_NAME.getKey());
    }

    /**
     * <p>getDbValidationQuery.</p>
     *
     * @return a {@link String} object.
     */
    public String getDbValidationQuery() {
        return applicationConfig.getOption(SumarisConfigurationOption.DB_VALIDATION_QUERY.getKey());
    }

    /**
     * <p>getJdbcUsername.</p>
     *
     * @return a {@link String} object.
     */
    public String getJdbcUsername() {
        return applicationConfig.getOption(SumarisConfigurationOption.JDBC_USERNAME.getKey());
    }

    /**
     * <p>getJdbcPassword.</p>
     *
     * @return a {@link String} object.
     */
    public String getJdbcPassword() {
        return applicationConfig.getOption(SumarisConfigurationOption.JDBC_PASSWORD.getKey());
    }

    /**
     * <p>getJdbcBatchSize.</p>
     *
     * @return a int.
     */
    public int getJdbcBatchSize() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.JDBC_BATCH_SIZE.getKey());
    }

    /**
     * <p>getHibernateSecondLevelCache.</p>
     *
     * @return a boolean.
     */
    public boolean useHibernateSecondLevelCache() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.HIBERNATE_SECOND_LEVEL_CACHE.getKey());
    }

    /**
     * <p>useHibernateSqlComment.</p>
     *
     * @return a boolean.
     */
    public boolean getFormatSql() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.HIBERNATE_FORMAT_SQL.getKey());
    }

    /**
     * <p>useHibernateSqlComment.</p>
     *
     * @return a boolean.
     */
    public boolean useHibernateSqlComment() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.HIBERNATE_USE_SQL_COMMENT.getKey());
    }

    /**
     * <p>getStatusIdTemporary.</p>
     *
     * @return a {@link int} object.
     */
    public int getStatusIdTemporary() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.STATUS_ID_TEMPORARY.getKey());
    }

    /**
     * <p>getStatusIdValid.</p>
     *
     * @return a {@link int}.
     */
    public int getStatusIdValid() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.STATUS_ID_ENABLE.getKey());
    }

    /**
     * <p>getUnitIdNone.</p>
     *
     * @return a {@link int}.
     */
    public int getUnitIdNone() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.UNIT_ID_NONE.getKey());
    }

    /**
     * <p>getMatrixIdIndividual.</p>
     *
     * @return a {@link int}.
     */
    public int getMatrixIdIndividual() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.MATRIX_ID_INDIVIDUAL.getKey());
    }

    /**
     * <p>getAppName.</p>
     *
     * @return a {@link String} object: the application id.
     */
    public String getAppName() {
        return applicationConfig.getOption(SumarisConfigurationOption.APP_NAME.getKey());
    }

    /**
     * <p>getVersion.</p>
     *
     * @return a {@link Version} object.
     */
    public Version getVersion() {
        return VersionBuilder.create(applicationConfig.getOptionAsVersion(SumarisConfigurationOption.VERSION.getKey())).build();
    }

    /**
     * <p>getI18nDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getI18nDirectory() {
        return applicationConfig.getOptionAsFile(
                SumarisConfigurationOption.I18N_DIRECTORY.getKey());
    }

    /**
     * <p>getI18nLocale.</p>
     *
     * @return a {@link Locale} object.
     */
    public Locale getI18nLocale() {
        return applicationConfig.getOptionAsLocale(
                SumarisConfigurationOption.I18N_LOCALE.getKey());
    }

    /**
     * <p>setI18nLocale.</p>
     *
     * @param locale a {@link Locale} object.
     */
    public void setI18nLocale(Locale locale) {
        applicationConfig.setOption(SumarisConfigurationOption.I18N_LOCALE.getKey(), locale.toString());
    }

    /**
     * <p>getConnectionProperties.</p>
     *
     * @return a {@link Properties} object.
     */
    public Properties getConnectionProperties() {
        return Daos.getConnectionProperties(
                getJdbcURL(),
                getJdbcUsername(),
                getJdbcPassword(),
                null,
                getHibernateDialect(),
                getJdbcDriver());
    }

    /**
     * <p>getLiquibaseDiffTypes.</p>
     *
     * @return a {@link String} object.
     */
    public String getLiquibaseDiffTypes() {
        return applicationConfig.getOption(SumarisConfigurationOption.LIQUIBASE_DIFF_TYPES.getKey());
    }

    /**
     * <p>Get the output file, for action (e.g. a file to create, when executing a dump action).</p>
     * <p>Used by CLI (Command Line Interface) actions</p>
     *
     * @return a {@link File} object.
     */
    public File getCliOutputFile() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.CLI_OUTPUT_FILE.getKey());
    }

    /**
     * <p>Should overwrite output file, if exists?</p>
     * <p>Used by CLI (Command Line Interface) actions</p>
     *
     * @return a boolean.
     */
    public boolean isCliForceOutput() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.CLI_FORCE_OUTPUT.getKey());
    }


    /**
     * <p>getLaunchMode.</p>
     *
     * @return a {@link String} object.
     */
    public String getLaunchMode() {
        return applicationConfig.getOption(SumarisConfigurationOption.LAUNCH_MODE.getKey());
    }

    /**
     * <p>isProduction.</p>
     *
     * @return true if production mode.
     */
    public boolean isProduction() {
       return LaunchModeEnum.production.name().equalsIgnoreCase(getLaunchMode());
    }

    public int getDefaultQualityFlagId() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.DEFAULT_QUALITY_FLAG.getKey());
    }

    public int getSequenceIncrementValue() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.SEQUENCE_INCREMENT.getKey());
    }

    public int getSequenceStartWithValue() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.SEQUENCE_START_WITH.getKey());
    }

    public String getSequenceSuffix() {
        return applicationConfig.getOption(SumarisConfigurationOption.SEQUENCE_SUFFIX.getKey());
    }

    public String getCsvSeparator() {
        return applicationConfig.getOption(SumarisConfigurationOption.CSV_SEPARATOR.getKey());
    }

    public boolean isInitStatisticalRectangles() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.INIT_STATISTICAL_RECTANGLES.getKey());
    }

    public boolean enableBatchHashOptimization() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_BATCH_HASH_OPTIMIZATION.getKey());
    }

    public boolean enableSampleHashOptimization() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_SAMPLE_HASH_OPTIMIZATION.getKey());
    }

    public String getVesselDefaultProgramLabel() {
        return applicationConfig.getOption(SumarisConfigurationOption.VESSEL_DEFAULT_PROGRAM_LABEL.getKey());
    }


    /* -- protected methods -- */

    /**
     * <p>getOptionAsURL.</p>
     *
     * @param key a {@link String} object.
     * @return a {@link URL} object.
     */
    protected URL getOptionAsURL(String key) {
        String urlString = applicationConfig.getOption(key);

        // Could be empty (e.g. demo deployment)
        if (StringUtils.isBlank(urlString)) {
            return null;
        }

        // correct end of the url string
        if (!urlString.endsWith("/")) {
            int schemeIndex = urlString.indexOf("://");
            int firstSlashIndex = urlString.indexOf('/', schemeIndex + 3);
            boolean addSlash = false;
            if (firstSlashIndex == -1) {
                addSlash = true;
            }
            else {
                int lastSlashIndex = urlString.lastIndexOf('/');
                if (lastSlashIndex > firstSlashIndex) {
                    addSlash = urlString.indexOf('.', lastSlashIndex) == -1;
                }
            }

            if (addSlash) {
                urlString += '/';
            }
        }

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            log.error(ex.getLocalizedMessage(), ex);
        }

        return url;
    }

    public String getColumnDefaultValue(String tableName, String columnName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(tableName));
        Preconditions.checkArgument(StringUtils.isNotBlank(columnName));

        return applicationConfig.getOption("sumaris." + tableName.toUpperCase() + "." + columnName.toUpperCase() + ".defaultValue");
    }
}
