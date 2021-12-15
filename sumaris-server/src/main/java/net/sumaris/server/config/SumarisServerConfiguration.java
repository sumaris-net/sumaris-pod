/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.config;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.server.http.security.AuthTokenTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * <p>SumarisServerConfiguration class.</p>
 *
 */
@Slf4j
public class SumarisServerConfiguration extends SumarisConfiguration {

    private static SumarisServerConfiguration instance;

    /**
     * <p>initDefault.</p>
     */
    public static void initDefault(ConfigurableEnvironment env) {
        instance = new SumarisServerConfiguration(env, args);
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

    /**
     * <p>Constructor for SumarisServerConfiguration.</p>
     *
     * @param env  a {@link ConfigurableEnvironment} object.
     * @param args a {@link String} object.
     */
    public SumarisServerConfiguration(ConfigurableEnvironment env, String... args) {
        super(env, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void overrideExternalModulesDefaultOptions(ApplicationConfig applicationConfig) {
        super.overrideExternalModulesDefaultOptions(applicationConfig);
    }

    public List<Integer> getConfigurationOptionAsNumbers(String optionKey) {
        List<Integer> result = (List<Integer>) complexOptionsCache.getIfPresent(optionKey);

        // Not exists in cache
        if (result == null) {
            String ids = applicationConfig.getOption(optionKey);
            if (StringUtils.isBlank(ids)) {
                result = ImmutableList.of();
            } else {
                final List<String> invalidIds = Lists.newArrayList();
                result = Splitter.on(",").omitEmptyStrings().trimResults()
                        .splitToList(ids)
                        .stream()
                        .map(id -> {
                            try {
                                return Integer.parseInt(id);
                            } catch (Exception e) {
                                invalidIds.add(id);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(invalidIds)) {
                    log.error("Skipping invalid values found in configuration option '{}': {}", optionKey, invalidIds);
                }
            }

            // Add to cache
            complexOptionsCache.put(optionKey, result);
        }
        return result;
    }

    public List<Integer> getAccessNotSelfDataDepartmentIds() {
        return getConfigurationOptionAsNumbers(SumarisServerConfigurationOption.ACCESS_NOT_SELF_DATA_DEPARTMENT_IDS.getKey());
    }

    public List<Integer> getAuthorizedProgramIds() {
        return getConfigurationOptionAsNumbers(SumarisServerConfigurationOption.ACCESS_DATA_PROGRAM_IDS.getKey());
    }

    public String getAccessNotSelfDataMinRole() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.ACCESS_NOT_SELF_DATA_MIN_ROLE.getKey());
    }

    public String getAccessNotSelfExtractionMinRole() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.ACCESS_NOT_SELF_EXTRACTION_MIN_ROLE.getKey());
    }

    public boolean enableAuthToken() {
        return applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.SECURITY_AUTHENTICATION_TOKEN_ENABLED.getKey());
    }

    public boolean enableAuthBasic() {
        return applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.SECURITY_AUTHENTICATION_LDAP_ENABLED.getKey())
                || applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.SECURITY_AUTHENTICATION_AD_ENABLED.getKey());
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
     *
     * @return a {@link File} object.
     */
    public File getDownloadDirectory() {
        return applicationConfig.getOptionAsFile(SumarisServerConfigurationOption.DOWNLOAD_DIRECTORY.getKey());
    }

    /**
     * <p>getUploadDirectory</p>
     *
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

    public AuthTokenTypeEnum getAuthTokenType() {
        if (enableAuthBasic() && enableAuthToken()) {
            return AuthTokenTypeEnum.BASIC_AND_TOKEN;
        } else if (enableAuthBasic()) {
            return AuthTokenTypeEnum.BASIC;
        } else {
            return AuthTokenTypeEnum.TOKEN;
        }
    }

    /**
     * <p>getAppMinVersion.</p>
     *
     * @return a {@link String} object.
     */
    public Version getAppMinVersion() {
        String versionStr = applicationConfig.getOption(SumarisServerConfigurationOption.APP_MIN_VERSION.getKey());
        if (StringUtils.isBlank(versionStr)) return null;

        try {
            return VersionBuilder.create(versionStr).build();
        } catch (Exception e) {
            logger.error(String.format("Unable to parse value '%s' for config option '%s': %s",
                    versionStr, SumarisServerConfigurationOption.APP_MIN_VERSION.getKey(), e.getMessage()));
            return null;
        }

    }

    public boolean enableGravatarFallback() {
        return applicationConfig.getOptionAsBoolean(SumarisServerConfigurationOption.ENABLE_GRAVATAR.getKey());
    }

    public String gravatarUrl() {
        return applicationConfig.getOption(SumarisServerConfigurationOption.GRAVATAR_URL.getKey());
    }

    /* -- Internal methods -- */


    /**
     * Initialization default timezone, from configuration (mantis #34754)
     */
    @Override
    protected void initTimeZone(ApplicationConfig applicationConfig) {

        String timeZone = applicationConfig.getOption(SumarisConfigurationOption.TIMEZONE.getKey());
        if (StringUtils.isNotBlank(timeZone)) {
            log.info("Using timezone {{}}", timeZone);
            TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
            System.setProperty("user.timezone", timeZone);
        } else {
            log.info("Using default timezone {{}}", System.getProperty("user.timezone"));
        }

        String dbTimeZone = applicationConfig.getOption(SumarisConfigurationOption.DB_TIMEZONE.getKey());
        if (StringUtils.isNotBlank(dbTimeZone)) {
            log.info("Using timezone {{}} for database", dbTimeZone);
        } else {
            log.info("Using default timezone {{}} for database", System.getProperty("user.timezone"));
        }
    }


}
