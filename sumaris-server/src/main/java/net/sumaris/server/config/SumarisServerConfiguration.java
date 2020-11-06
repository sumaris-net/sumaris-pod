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
import net.sumaris.core.util.I18nUtil;
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

    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(SumarisServerConfiguration.class);

    private static SumarisServerConfiguration instance;

    /**
     * <p>initDefault.</p>
     */
    public static void initDefault(String configFileName) {
        instance = new SumarisServerConfiguration(configFileName, args);
        setInstance(instance);
    }

    /**
     * <p>Getter for the field <code>instance</code>.</p>
     *
     * @return a {@link SumarisServerConfiguration} object.
     */
    public static SumarisServerConfiguration getInstance() {
        return instance;
    }

    /**
     * <p>Constructor for SumarisServerConfiguration.</p>
     *
     * @param applicationConfig a {@link ApplicationConfig} object.
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



    }

    /** {@inheritDoc} */
    @Override
    protected void overrideExternalModulesDefaultOptions(ApplicationConfig applicationConfig) {
        super.overrideExternalModulesDefaultOptions(applicationConfig);
    }

    public String getAuthRoleForNotSelfData() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.AUTH_ROLE_NOT_SELF_DATA_ACCESS.getKey());
    }

    public String getAuthRoleForNotSelfExtraction() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.AUTH_ROLE_NOT_SELF_EXTRACTION_ACCESS.getKey());
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

    public boolean enableMailService() {
        return applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.EMAIL_ENABLED.getKey());
    }

    /**
     * <p>Get mail host?</p>
     *
     * @return a {@link Boolean}
     */
    public String getMailHost() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.MAIL_HOST.getKey());
    }

    /**
     * <p>Get mail host?</p>
     *
     * @return a {@link Boolean}
     */
    public int getMailPort() {
        return applicationConfig.getOptionAsInt(SumarisServerConfigurationOption.MAIL_PORT.getKey());
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
     * <p>find mail from address.</p>
     *
     * @return a {@link String} object, the 'from' address to use for mail .
     */
    public String getMailFrom() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.MAIL_FROM.getKey());
    }

    /**
     * <p>find keypair salt.</p>
     *
     * @return a {@link String} object, the 'salt' for the server keypair generation.
     */
    public String getKeypairSalt() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.KEYPAIR_SALT.getKey());
    }

    /**
     * <p>find keypair password.</p>
     *
     * @return a {@link String} object, the 'password' for the server keypair generation.
     */
    public String getKeypairPassword() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.KEYPAIR_PASSWORD.getKey());
    }

    /**
     * <p>find auth challenge life time (in seconds).</p>
     *
     * @return a {@link Integer}
     */
    public int getAuthChallengeLifeTime() {
        return applicationConfig.getOptionAsInt(SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getKey());
    }

    /**
     * <p>find auth session duration (in seconds).</p>
     *
     * @return a {@link Integer}
     */
    public int getAuthTokenLifeTimeInSeconds() {
        return applicationConfig.getOptionAsInt(SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getKey());
    }

    /**
     * <p>find the ActiveMQ broker URL.</p>
     *
     * @return a {@link Integer}
     */
    public String getActiveMQBrokerURL() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.ACTIVEMQ_BROKER_URL.getKey());
    }

    /**
     * <p>Is ActiveMQ enabled ?</p>
     *
     * @return a {@link Boolean}
     */
    public boolean enableActiveMQ() {
        return applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.ACTIVEMQ_ENABLED.getKey());
    }

    /* -- Internal methods -- */




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
