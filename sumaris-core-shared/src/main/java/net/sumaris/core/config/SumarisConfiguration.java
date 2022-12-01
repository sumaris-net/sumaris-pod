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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.env.ConfigurableEnvironments;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.config.*;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.persistence.LockModeType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.nuiton.i18n.I18n.t;

/**
 * Access to configuration options
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Slf4j
public class SumarisConfiguration extends PropertyPlaceholderConfigurer {

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

    /**
     * Cache for complexe options (e.g. for list of values, to avoid many call of split())
     */
    protected final Cache<String, Object> complexOptionsCache = CacheBuilder.newBuilder().build();

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

    protected final Set<String> transientOptionKeys;

    protected final Properties defaults;

    /**
     * <p>initDefault.</p>
     */
    public static void initDefault(@NonNull ConfigurableEnvironment env) {
        instance = new SumarisConfiguration(env, args);
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
        this.transientOptionKeys = null;
        this.defaults = null;

        // Override application version
        initVersion(applicationConfig);
    }

    public SumarisConfiguration(ConfigurableEnvironment env,
                                String... args) {
        this(env, "application.fake.properties", args);
    }

    public SumarisConfiguration(String file,
                                String... args) {
        this(null, file, args);
    }

    /**
     * <p>Constructor for SumarisConfiguration.</p>
     *
     * @param env a {@link ConfigurableEnvironment} object.
     * @param file a {@link String} object.
     * @param args a {@link String} object.
     */
    protected SumarisConfiguration(ConfigurableEnvironment env,
                                   String file,
                                   String... args) {


        // load all default options
        Set<ApplicationConfigProvider> providers = getProviders();
        this.defaults = getDefaults(providers, env);

        // Create Nuiton config instance
        this.applicationConfig = new ApplicationConfig(ApplicationConfigInit.forAllScopesWithout(
                ApplicationConfigScope.HOME
        )
                .setDefaults(this.defaults));
        this.applicationConfig.setEncoding(Charsets.UTF_8.name());
        this.applicationConfig.setConfigFileName(file);

        // Load transient options keys
        this.transientOptionKeys = ImmutableSet.copyOf(ApplicationConfigHelper.getTransientOptionKeys(providers));

        System.setProperty("logging.level.Hibernate Types", "error");

        // Load actions
        for (ApplicationConfigProvider provider : providers) {
            applicationConfig.loadActions(provider.getActions());
        }

        // Define Alias
        addAlias(applicationConfig);

        // parse config file and inline arguments
        try {
            applicationConfig.parse(args);

        } catch (ArgumentsParserException e) {
            throw new SumarisTechnicalException(t("sumaris.config.parse.error"), e);
        }

        // Init the application version
        initVersion(applicationConfig);

        // Init time zone
        initTimeZone(applicationConfig);

        // Prepare basedir
        fixBasedir(applicationConfig);

        if (log.isTraceEnabled())
            log.trace(applicationConfig.getPrintableConfig(null, 4));
    }

    public void doAllAction() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        // Make sure all alias has been used, for parsing args, even those defined in modules (e.g. extraction)
        parseUnparsedArgs();

        applicationConfig.doAllAction();
    }

    /**
     * Parse unparsed args. Useful when SumarisConfiguration is calling parse() before new alias
     * (e.g. alias defined by external modules - see extraction module).
     * Calling this method allow to parse missing args, that correspond to external module's alias.
     */
    public void parseUnparsedArgs() {
        List<String> unparsedArgs = applicationConfig.getUnparsed();
        if (CollectionUtils.isNotEmpty(unparsedArgs)) {

            // Parse unparsed args
            try {
                applicationConfig.parse(unparsedArgs.toArray(new String[0]));

            } catch (ArgumentsParserException e) {
                throw new SumarisTechnicalException(t("sumaris.config.parse.error"), e);
            }
        }
    }

    public void cleanCache() {
        complexOptionsCache.invalidateAll();
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

        // CLI options
        applicationConfig.addAlias("--daemon", "--option", SumarisConfigurationOption.CLI_DAEMONIZE.getKey(), "true");
        applicationConfig.addAlias("-d", "--option", SumarisConfigurationOption.CLI_DAEMONIZE.getKey(), "true");
        applicationConfig.addAlias("--output", "--option", SumarisConfigurationOption.CLI_OUTPUT_FILE.getKey());
        applicationConfig.addAlias("-f", "--option", SumarisConfigurationOption.CLI_FORCE_OUTPUT.getKey(), "true");
        applicationConfig.addAlias("--year", "--option", SumarisConfigurationOption.CLI_FILTER_YEAR.getKey());

    }

    protected static Set<ApplicationConfigProvider> getProviders() {
        // get allOfToList config providers
        return ApplicationConfigHelper.getProviders(null,
            null,
            null,
            true);
    }

    protected Properties getDefaults(Set<ApplicationConfigProvider> providers, ConfigurableEnvironment env) {

        // Populate defaults from providers
        final Properties defaults = new Properties();
        providers.forEach(provider -> Arrays.stream(provider.getOptions())
            .filter(configOptionDef -> configOptionDef.getDefaultValue() != null)
            .forEach(configOptionDef -> defaults.setProperty(configOptionDef.getKey(), configOptionDef.getDefaultValue())));

        // Set options from env if provided
        if (env != null) {
            return ConfigurableEnvironments.readProperties(env, defaults);
        }

        return defaults;
    }

    /**
     * Initialization default timezone, from configuration (mantis #24623)
     */
    protected void initTimeZone(ApplicationConfig applicationConfig) {

        String dbTimeZone = applicationConfig.getOption(SumarisConfigurationOption.HIBERNATE_JDBC_TIMEZONE.getKey());
        if (StringUtils.isNotBlank(dbTimeZone)) {
            if (log.isInfoEnabled()) {
                log.info("Using timezone {{}} for database", dbTimeZone);
            }
        } else if (log.isInfoEnabled()) {
            log.info("Using default timezone {{}} for database", System.getProperty("user.timezone"));
        }
        // Set to system properties (need by JPA)
        System.setProperty(SumarisConfigurationOption.HIBERNATE_JDBC_TIMEZONE.getKey(), dbTimeZone);
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

    protected void fixBasedir(ApplicationConfig applicationConfig) {
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
            log.info(String.format("Database URL {%s}", getJdbcURL()));
        }
        applicationConfig.setOption(
            SumarisConfigurationOption.BASEDIR.getKey(),
            appBasedir.getAbsolutePath());
    }


    public void restoreDefaults() {
        defaults.forEach((key, value) -> {
            applicationConfig.setOption(key.toString(), value.toString());
        });
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

    public Set<String> getTransientOptionKeys() {
        return transientOptionKeys;
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
     * <p>getCacheDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getCacheDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.CACHE_DIRECTORY.getKey());
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
     * <p>getDbTimezone.</p>
     *
     * @return a {@link TimeZone} object.
     */
    public TimeZone getTimezone() {
        String tz = applicationConfig.getOption(SumarisConfigurationOption.TIMEZONE.getKey());
        return StringUtils.isNotBlank(tz) ? TimeZone.getTimeZone(tz) : TimeZone.getDefault();
    }

    /**
     * <p>getMeasFileDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getMeasFileDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.MEASUREMENT_FILE_DIRECTORY.getKey());
    }

    /**
     * <p>getImageAttachmentDirectory.</p>
     *
     * @return a {@link File} object.
     */
    public File getImageAttachmentDirectory() {
        return applicationConfig.getOptionAsFile(SumarisConfigurationOption.IMAGE_ATTACHMENT_DIRECTORY.getKey());
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
     * <p>isLiquibaseEnabled.</p>
     *
     * @return a boolean.
     */
    public boolean isLiquibaseEnabled() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.LIQUIBASE_ENABLED.getKey());
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

    public boolean isOracleDatabase() {
        return Daos.isOracleDatabase(getJdbcURL());
    }

    public DatabaseType getDatabaseType() {
        return Daos.getDatabaseType(getJdbcURL());
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
     * Is spring cache enabled ?
     * @return
     */
    public boolean enableCache() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.CACHE_ENABLED.getKey());
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
     * Should enable configuration load from DB ?
     * @return
     */
    public boolean enableConfigurationDbPersistence() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_CONFIGURATION_DB_PERSISTENCE.getKey());
    }

    /**
     * <p>find the analytic references service URL.</p>
     *
     * @return a {@link String}
     */
    public boolean enableAnalyticReferencesService() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_ANALYTIC_REFERENCES.getKey());
    }


    /**
     * <p>find the analytic references service URL.</p>
     *
     * @return a {@link String}
     */
    public String getAnalyticReferencesServiceUrl() {
        return applicationConfig.getOption(SumarisConfigurationOption.ANALYTIC_REFERENCES_SERVICE_URL.getKey());
    }

    /**
     * <p>find the analytic references service authorization key (format "user:pass").</p>
     *
     * @return a {@link String}
     */
    public String getAnalyticReferencesServiceAuth() {
        return applicationConfig.getOption(SumarisConfigurationOption.ANALYTIC_REFERENCES_SERVICE_AUTH.getKey());
    }

    /**
     * <p>get the delay in days between two calls to the analytic references service.</p>
     *
     * @return a {@link int}.
     */
    public int getAnalyticReferencesServiceDelay() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.ANALYTIC_REFERENCES_SERVICE_DELAY.getKey());
    }

    /**
     * <p>get the analytic references service filter as a regexp on code.</p>
     *
     * @return a {@link String}.
     */
    public String getAnalyticReferencesServiceFilter() {
        return applicationConfig.getOption(SumarisConfigurationOption.ANALYTIC_REFERENCES_SERVICE_FILTER.getKey());
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
     * <p>Get year, to filter data.</p>
     * <p>Used by CLI (Command Line Interface) actions</p>
     *
     * @return a boolean.
     */
    public Integer getCliFilterYear() {
        int year = applicationConfig.getOptionAsInt(SumarisConfigurationOption.CLI_FILTER_YEAR.getKey());
        return year == -1 ? null : year;
    }

    public Integer getCliFilterTripId() {
        int tripId = applicationConfig.getOptionAsInt(SumarisConfigurationOption.CLI_FILTER_TRIP_ID.getKey());
        return tripId == -1 ? null : tripId;
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

    public Integer getLockTimeout() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.LOCK_TIMEOUT.getKey());
    }

    public LockModeType getLockModeType() {
        return LockModeType.valueOf(applicationConfig.getOption(SumarisConfigurationOption.LOCK_MODE_TYPE.getKey()));
    }

    public char getCsvSeparator() {
        return applicationConfig.getOption(SumarisConfigurationOption.CSV_SEPARATOR.getKey()).charAt(0);
    }

    public boolean enableTechnicalTablesUpdate() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_TECHNICAL_TABLES_UPDATE.getKey());
    }

    public int getGeometrySrid() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.GEOMETRY_SRID.getKey());
    }

    public boolean enableBatchHashOptimization() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_BATCH_HASH_OPTIMIZATION.getKey());
    }

    public boolean enableSampleHashOptimization() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_SAMPLE_HASH_OPTIMIZATION.getKey());
    }

    public boolean enableSampleUniqueTag() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_SAMPLE_UNIQUE_TAG.getKey());
    }

    public boolean enablePhysicalGearHashOptimization() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_PHYSICAL_GEAR_HASH_OPTIMIZATION.getKey());
    }


    public boolean enableAdagioOptimization() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ENABLE_ADAGIO_OPTIMIZATION.getKey());
    }
    public String getAdagioSchema() {
        return applicationConfig.getOption(SumarisConfigurationOption.DB_ADAGIO_SCHEMA.getKey());
    }


    /**
     * Prefer ProgramEnum.SIH.getLabel()
     */
    @Deprecated
    public String getVesselDefaultProgramLabel() {
        return applicationConfig.getOption(SumarisConfigurationOption.VESSEL_DEFAULT_PROGRAM_LABEL.getKey());
    }

    public boolean enableVesselRegistrationCodeNaturalOrder() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.VESSEL_REGISTRATION_CODE_NATURAL_ORDER.getKey());
    }

    public boolean enableVesselRegistrationCodeSearchAsPrefix() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.VESSEL_REGISTRATION_CODE_SEARCH_AS_PREFIX.getKey());
    }


    /**
     * <p>find the ActiveMQ broker URL.</p>
     *
     * @return a {@link String}
     */
    public String getActiveMQBrokerURL() {
        return applicationConfig.getOption(SumarisConfigurationOption.ACTIVEMQ_BROKER_URL.getKey());
    }

    /**
     * <p>find the ActiveMQ broker username (or null if no auth).</p>
     *
     * @return a {@link String}
     */
    public String getActiveMQBrokerUserName() {
        return applicationConfig.getOption(SumarisConfigurationOption.ACTIVEMQ_BROKER_USERNAME.getKey());
    }

    /**
     * <p>find the ActiveMQ broker username (or null if no auth).</p>
     *
     * @return a {@link Integer}
     */
    public String getActiveMQBrokerPassword() {
        return applicationConfig.getOption(SumarisConfigurationOption.ACTIVEMQ_BROKER_PASSWORD.getKey());
    }

    /**
     * <p>find the ActiveMQ broker username (or null if no auth).</p>
     *
     * @return a {@link Integer}
     */
    public int getActiveMQPrefetchLimit() {
        return applicationConfig.getOptionAsInt(SumarisConfigurationOption.ACTIVEMQ_PREFETCH_LIMIT.getKey());
    }


    /**
     * <p>Is ActiveMQ enabled ?</p>
     *
     * @return a {@link Boolean}
     */
    public boolean enableActiveMQPool() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.ACTIVEMQ_POOL_ENABLED.getKey());
    }

    public boolean enableDataImages() {
        return applicationConfig.getOptionAsBoolean(SumarisConfigurationOption.DATA_IMAGES_ENABLE.getKey());
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
