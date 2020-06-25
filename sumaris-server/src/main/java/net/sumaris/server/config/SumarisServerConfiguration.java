package net.sumaris.server.config;

/*-
 * #%L
 * Quadrige3 Core :: Quadrige3 Sumaris server
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
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

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>SumarisServerConfiguration class.</p>
 *
 */
public class SumarisServerConfiguration extends SumarisConfiguration {

    public static final String CONFIG_FILE_NAME = "application.properties";

    private static final String CONFIG_FILE_ENV_PROPERTY = "spring.config.location";

    private static final String CONFIG_FILE_JNDI_NAME = "java:comp/env/" + CONFIG_FILE_NAME;


    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(SumarisServerConfiguration.class);

    private static SumarisServerConfiguration instance;

    /**
     * <p>initDefault.</p>
     */
    public static void initDefault() {
        instance = new SumarisServerConfiguration(getWebConfigFile(), args);
        setInstance(instance);
    }

    /**
     * <p>Getter for the field <code>instance</code>.</p>
     *
     * @return a {@link net.sumaris.server.config.SumarisServerConfiguration} object.
     */
    public static SumarisServerConfiguration getInstance() {
        if (instance == null) {
            initDefault();
        }
        return instance;
    }

    /**
     * <p>Constructor for SumarisServerConfiguration.</p>
     *
     * @param applicationConfig a {@link org.nuiton.config.ApplicationConfig} object.
     */
    public SumarisServerConfiguration(ApplicationConfig applicationConfig) {
        super(applicationConfig);
    }

    /**
     * <p>Constructor for SumarisServerConfiguration.</p>
     *
     * @param file a {@link String} object.
     * @param args a {@link String} object.
     */
    public SumarisServerConfiguration(String file, String... args) {
        super(file, args);

        // Init i18n
        try {
            initI18n();
        } catch (IOException e) {
            throw new SumarisTechnicalException("i18n initialization failed", e);
        }

        // Init directories
        try {
            initDirectories();
        } catch (IOException e) {
            throw new SumarisTechnicalException("Directories initialization failed", e);
        }

        // Init active MQ data directory
        System.setProperty("org.apache.activemq.default.directory.prefix", getDataDirectory().getPath() + File.separator);

    }

    /** {@inheritDoc} */
    @Override
    protected void overrideExternalModulesDefaultOptions(ApplicationConfig applicationConfig) {
        super.overrideExternalModulesDefaultOptions(applicationConfig);
    }

    public String getAuthNotSelfDataRole() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.AUTH_NOT_SELF_DATA_ROLE.getKey());
    }

    /**
     * <p>getVersionAsString.</p>
     *
     * @return a {@link String} object.
     */
    public String getVersionAsString() {
        return applicationConfig.getOption(SumarisConfigurationOption.VERSION.getKey());
    }

    /**
     * <p>getServerPort.</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getServerPort() {
        return applicationConfig.getOptionAsInt(SumarisServerConfigurationOption.SERVER_PORT.getKey());
    }

    /**
     * <p>getServerAddress.</p>
     *
     * @return a {@link String} object.
     */
    public String getServerUrl() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.SERVER_URL.getKey());
    }

    /**
     * <p>getDownloadDirectory</p>
     * @return a {@link File} object.
     */
    public File getDownloadDirectory() {
        return applicationConfig.getOptionAsFile(SumarisServerConfigurationOption.DOWNLOAD_DIRECTORY.getKey());
    }

    /**
     * <p>getUploadDirectory</p>
     * @return a {@link File} object.
     */
    public File getUploadDirectory() {
        return applicationConfig.getOptionAsFile(SumarisServerConfigurationOption.UPLOAD_DIRECTORY.getKey());
    }

    /**
     * <p>getServerAddress.</p>
     *
     * @return a {@link String} object.
     */
    public String getRegistrationConfirmUrlPattern() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.REGISTRATION_CONFIRM_URL.getKey());
    }

    /**
     * <p>getAdminMail.</p>
     *
     * @return a {@link String} object, the admin email.
     */
    public String getAdminMail() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.ADMIN_MAIL.getKey());
    }

    /**
     * <p>get mail from address.</p>
     *
     * @return a {@link String} object, the 'from' address to use for mail .
     */
    public String getMailFrom() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.MAIL_FROM.getKey());
    }

    /**
     * <p>get keypair salt.</p>
     *
     * @return a {@link String} object, the 'salt' for the server keypair generation.
     */
    public String getKeypairSalt() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.KEYPAIR_SALT.getKey());
    }

    /**
     * <p>get keypair password.</p>
     *
     * @return a {@link String} object, the 'password' for the server keypair generation.
     */
    public String getKeypairPassword() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.KEYPAIR_PASSWORD.getKey());
    }

    /**
     * <p>get auth challenge life time (in seconds).</p>
     *
     * @return a {@link Integer}
     */
    public int getAuthChallengeLifeTime() {
        return applicationConfig.getOptionAsInt(SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getKey());
    }

    /**
     * <p>get auth session duration (in seconds).</p>
     *
     * @return a {@link Integer}
     */
    public int getAuthTokenLifeTimeInSeconds() {
        return applicationConfig.getOptionAsInt(SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getKey());
    }

    /**
     * <p>get the ActiveMQ broker URL.</p>
     *
     * @return a {@link Integer}
     */
    public String getActiveMQBrokerURL() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.ACTIVEMQ_BROKER_URL.getKey());
    }

    /**
     * <p>get the ActiveMQ broker URL.</p>
     *
     * @return a {@link Integer}
     */
    public boolean isActiveMQEnable() {
        return applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.ACTIVEMQ_ENABLE.getKey());
    }

    /* -- Internal methods -- */

    /**
     * <p>getWebConfigFile.</p>
     *
     * @return a {@link String} object.
     */
    protected static String getWebConfigFile() {
        // Could override config file id (useful for dev)
        String configFile = CONFIG_FILE_NAME;
        if (System.getProperty(CONFIG_FILE_ENV_PROPERTY) != null) {
            configFile = System.getProperty(CONFIG_FILE_ENV_PROPERTY);
            configFile = configFile.replaceAll("\\\\", "/");
        }
        else {
            try {
                InitialContext ic = new InitialContext();
                String jndiPathToConfFile = (String) ic.lookup(CONFIG_FILE_JNDI_NAME);
                if (StringUtils.isNotBlank(jndiPathToConfFile)) {
                    configFile = jndiPathToConfFile;
                }
            } catch (NamingException e) {
                log.debug(String.format("Error while reading JNDI initial context. Skip configuration path override, from context [%s]", CONFIG_FILE_JNDI_NAME));
            }
        }

        return configFile;
    }

    /**
     * <p>initI18n.</p>
     *
     * @throws IOException if any.
     */
    protected void initI18n() throws IOException {

        // --------------------------------------------------------------------//
        // initConfig i18n
        // --------------------------------------------------------------------//
        File i18nDirectory = new File(getDataDirectory(), "i18n");
        if (i18nDirectory.exists()) {
            // clean i18n cache
            FileUtils.cleanDirectory(i18nDirectory);
        }

        FileUtils.forceMkdir(i18nDirectory);

        if (log.isDebugEnabled()) {
            log.debug("I18N directory: " + i18nDirectory);
        }

        Locale i18nLocale = getI18nLocale();

        I18n.init(new UserI18nInitializer(
            i18nDirectory, new DefaultI18nInitializer(getI18nBundleName())),
            i18nLocale);
        if (log.isInfoEnabled()) {
            log.info(I18n.t("sumaris.server.init.i18n",
                    i18nLocale, i18nDirectory));
        }
    }

    /**
     * <p>initDirectories.</p>
     *
     * @throws IOException if any.
     */
    protected void initDirectories() throws IOException {

        // log the data directory used
        log.info(I18n.t("sumaris.server.init.data.directory", getDataDirectory()));

        // Data directory
        FileUtils.forceMkdir(getDataDirectory());

        // DB attachment directory
        FileUtils.forceMkdir(getDbAttachmentDirectory());

        // DB backup directory
        FileUtils.forceMkdir(getDbBackupDirectory());

        // Download directory
        FileUtils.forceMkdir(getDownloadDirectory());

        // Upload directory
        FileUtils.forceMkdir(getUploadDirectory());

        // temp directory
        File tempDirectory = getTempDirectory();
        if (tempDirectory.exists()) {
            // clean temp files
            FileUtils.cleanDirectory(tempDirectory);
        }
    }

    /**
     * <p>getI18nBundleName.</p>
     *
     * @return a {@link String} object.
     */
    protected static String getI18nBundleName() {
        return "sumaris-core-server-i18n";
    }

    /**
     * Initialization default timezone, from configuration (mantis #34754)
     */
    @Override
    protected void initTimeZone() {

        String timeZone = applicationConfig.getOption(SumarisConfigurationOption.TIMEZONE.getKey());
        if (StringUtils.isNotBlank(timeZone)) {
            log.info(String.format("Using timezone [%s]", timeZone));
            TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
            System.setProperty("user.timezone", timeZone);
        } else {
            log.info(String.format("Using default timezone [%s]", System.getProperty("user.timezone")));
        }

        String dbTimeZone = applicationConfig.getOption(SumarisConfigurationOption.DB_TIMEZONE.getKey());
        if (StringUtils.isNotBlank(dbTimeZone)) {
            log.info(String.format("Using timezone [%s] for database", dbTimeZone));
        } else {
            log.info(String.format("Using default timezone [%s] for database", System.getProperty("user.timezone")));
        }
    }

}
