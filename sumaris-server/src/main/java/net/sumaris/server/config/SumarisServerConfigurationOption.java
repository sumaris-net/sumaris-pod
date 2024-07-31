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

import net.sumaris.core.util.Beans;
import net.sumaris.extraction.server.config.ExtractionWebConfigurationOption;
import org.nuiton.config.ConfigOptionDef;
import org.nuiton.version.Version;

import java.io.File;

import static org.nuiton.i18n.I18n.n;

public enum SumarisServerConfigurationOption implements ConfigOptionDef {

    SERVER_PORT(
            "server.port",
            n("sumaris.config.option.server.port.description"),
            "8080",
            Integer.class,
            false),

    SERVER_ADDRESS(
        "server.address",
        n("sumaris.config.option.server.address.description"),
        "localhost",
        String.class,
        false),

    SERVER_HOST(
            "server.host",
            n("sumaris.config.option.server.host.description"),
            "${server.address}",
            String.class,
            false),

    SERVER_PROTOCOL(
            "server.protocol",
            n("sumaris.config.option.server.protocol.description"),
            "http",
            String.class,
            false),

    SERVER_URL(
            "server.url",
            n("sumaris.config.option.server.url.description"),
            "${server.protocol}://${server.host}:${server.port}",
            String.class,
            false),

    APP_URL(
            "sumaris.app.url",
            n("sumaris.config.option.app.url.description"),
            "${server.url}",
            String.class,
            false),

    APP_REDIRECTION_CACHE_MAX_AGE(
        "sumaris.app.redirection.cache.maxAge",
        n("sumaris.config.option.app.redirection.cache.maxAge.description"),
        "300",
        Integer.class,
        false),

    REGISTRATION_CONFIRM_URL(
            "sumaris.server.account.register.confirm.url",
            n("sumaris.config.option.server.account.register.confirm.url.description"),
            "${sumarie.app.url}/confirm/{email}/{code}",
            String.class,
            false),

    PASSWORD_CHANGE_URL(
            "sumaris.server.account.change.confirm.url",
            n("sumaris.config.option.server.account.change.password.url.description"),
            "${server.url}/api/confirmPassword/?token={token}&email={email}",
            String.class,
            false),

    PASSWORD_CHANGE_DURATION(
            "sumaris.server.account.change.confirm.duration",
            n("sumaris.config.option.server.account.confirm.change.duration.description"),
            "15",
            Integer.class,
            false),
    EMAIL_ENABLED("spring.mail.enabled",
            n("sumaris.config.option.spring.mail.enabled.description"),
            "true",
            Boolean.class,
            false),

    ADMIN_MAIL(
            "sumaris.server.admin.mail",
            n("sumaris.config.option.server.admin.mail.description"),
            "contact@sumaris.net",
            String.class,
            false),

    KEYPAIR_SALT(
            "sumaris.server.keypair.salt",
            n("sumaris.config.option.server.keypair.salt.description"),
            "abc",
            String.class,
            false),

    KEYPAIR_PASSWORD(
            "sumaris.server.keypair.password",
            n("sumaris.config.option.server.keypair.password.description"),
            "def",
            String.class,
            false),

    MAIL_HOST(
            "spring.mail.host",
            n("sumaris.config.option.mail.host.description"),
            "localhost",
            String.class,
            false),

    MAIL_PORT(
            "spring.mail.port",
            n("sumaris.config.option.mail.port.description"),
            "25",
            Integer.class,
            false),

    MAIL_FROM(
            "sumaris.mail.from",
            n("sumaris.config.option.mail.from.description"),
            "no-reply@sumaris.net",
            String.class,
            false),

    AUTH_CHALLENGE_LIFE_TIME(
            "sumaris.auth.challenge.lifeTime",
            n("sumaris.config.option.auth.challenge.lifeTime.description"),
            "120", // a challenge leave 2 minutes
            Integer.class,
            false),

    AUTH_TOKEN_LIFE_TIME(
            "sumaris.auth.session.duration",
            n("sumaris.config.option.auth.session.duration.description"),
            "14400", // = 4 hours
            Integer.class,
            false),

    ACCESS_NOT_SELF_DATA_MIN_ROLE(
            "sumaris.data.accessNotSelfData.role",
            n("sumaris.config.option.data.accessNotSelfData.role.description"),
            "ROLE_ADMIN", // Possible values: ROLE_GUEST, ROLE_USER, ROLE_SUPERVISOR, ROLE_ADMIN
            String.class,
            false),

    ACCESS_NOT_SELF_DATA_DEPARTMENT_IDS(
            "sumaris.data.accessNotSelfData.department.ids",
            n("sumaris.config.option.data.accessNotSelfData.department.ids.description"),
            null,
            String.class,
            false),

    ACCESS_DATA_PROGRAM_IDS(
            "sumaris.data.program.ids",
            n("sumaris.config.option.data.program.ids.description"),
            null,
            String.class,
            false),

    ACCESS_NOT_SELF_EXTRACTION_MIN_ROLE(ExtractionWebConfigurationOption.ACCESS_NOT_SELF_EXTRACTION_MIN_ROLE),

    SECURITY_AUTHENTICATION_TOKEN_ENABLED(
            "spring.security.token.enabled",
            n("sumaris.config.option.spring.security.token.enabled.description"),
            "true",
            Boolean.class),

    SECURITY_AUTHENTICATION_LDAP_ENABLED(
            "spring.security.ldap.enabled",
            n("sumaris.config.option.spring.security.ldap.enabled.description"),
            "false",
            Boolean.class),

    SECURITY_AUTHENTICATION_AD_ENABLED(
            "spring.security.ad.enabled",
            n("sumaris.config.option.spring.security.ad.enabled.description"),
            "false",
            Boolean.class),

    AUTH_TOKEN_TYPE(
            "sumaris.auth.token.type",
            n("sumaris.config.option.auth.token.type.description"),
            null, // NUll == auto detected
            String.class),

    AUTH_API_TOKEN_ENABLED(
            "sumaris.auth.api.token.enabled",
            n("sumaris.config.option.auth.api.token.enable"),
            Boolean.FALSE.toString(),
            Boolean.class,
            false),

    APP_MIN_VERSION(
            "sumaris.app.version.min",
            n("sumaris.config.option.sumaris.app.version.min.description"),
            "2.8.0",
            Version.class,
            false),

    DOWNLOAD_DIRECTORY(
            "sumaris.download.directory",
            n("sumaris.config.option.download.directory.description"),
            "${sumaris.data.directory}/download",
            File.class),

    UPLOAD_DIRECTORY(
            "sumaris.upload.directory",
            n("sumaris.config.option.upload.directory.description"),
            "${sumaris.data.directory}/uploads",
            File.class),

    SITE_FAVICON(
            "sumaris.favicon",
            n("sumaris.config.option.favicon.description"),
            "${sumaris.site.url}/api/favicon",
            Integer.class,
            false),

    SITE_LOGO_SMALL(
            "sumaris.logo",
            n("sumaris.config.option.logo.description"),
            "${sumaris.site.url}/assets/img/logo-menu.png",
            Integer.class,
            false),

    LOGO_LARGE(
            "sumaris.logo.large",
            n("sumaris.config.option.logo.large.description"),
            "${sumaris.site.logo}",
            Integer.class,
            false),

    SITE_PARTNER_DEPARTMENTS(
            "sumaris.partner.departments",
            n("sumaris.config.option.partner.departments.description"),
            "department:1",
            String.class,
            false),

    SITE_BACKGROUND_IMAGES(
            "sumaris.background.images",
            n("sumaris.config.option.site.background.images.description"),
            "1",
            String.class,
            false),

    ANDROID_INSTALL_URL(
            "sumaris.android.install.url",
            n("sumaris.config.option.android.install.url.description"),
            "${server.url}/download/android/sumaris-app-latest.apk",
            String.class,
            false),

    ENABLE_GRAVATAR(
            "sumaris.gravatar.enable",
            n("sumaris.config.option.gravatar.enable.description"),
            "false",
            Boolean.class,
            false),

    GRAVATAR_URL(
            "sumaris.gravatar.url",
            n("sumaris.config.option.gravatar.url.description"),
            "https://www.gravatar.com/avatar/{md5}",
            String.class,
            false);

    /**
     * Configuration key.
     */
    private final String key;

    /**
     * I18n key of option description
     */
    private final String description;

    /**
     * Type of option
     */
    private final Class<?> type;

    /**
     * Default value of option.
     */
    private String defaultValue;

    /**
     * Flag to not keep option value on disk
     */
    private boolean isTransient;

    /**
     * Flag to not allow option value modification
     */
    private boolean isFinal;

    SumarisServerConfigurationOption(String key,
                                     String description,
                                     String defaultValue,
                                     Class<?> type,
                                     boolean isTransient) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
        this.isTransient = isTransient;
        this.isFinal = isTransient;
    }

    SumarisServerConfigurationOption(String key,
                                     String description,
                                     String defaultValue,
                                     Class<?> type) {
        this(key, description, defaultValue, type, true);
    }

    SumarisServerConfigurationOption(ConfigOptionDef bean) {
        Beans.copyProperties(bean, this);
        this.key = bean.getKey();
        this.description = bean.getDescription();
        this.type = bean.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransient(boolean newValue) {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFinal(boolean newValue) {
        // not used
    }

}
