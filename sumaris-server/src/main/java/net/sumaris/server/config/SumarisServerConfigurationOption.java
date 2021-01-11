package net.sumaris.server.config;

/*-
 * #%L
 * SUMARiS:: Server
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

import org.nuiton.config.ConfigOptionDef;

import java.io.File;

import static org.nuiton.i18n.I18n.n;

public enum SumarisServerConfigurationOption implements ConfigOptionDef {

    SERVER_PORT(
            "server.port",
            n("sumaris.config.option.server.port.description"),
            "8080",
            Integer.class,
            false),

    SERVER_HOST(
            "server.host",
            n("sumaris.config.option.server.host.description"),
            "localhost",
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

    REGISTRATION_CONFIRM_URL(
            "sumaris.server.account.register.confirm.url",
            n("sumaris.config.option.server.account.register.confirm.url.description"),
            "${server.url}/api/confirmEmail/?email={email}&code={code}",
            String.class,
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

    AUTH_NOT_SELF_DATA_ROLE(
            "sumaris.auth.notSelfDataAccess.role",
            n("sumaris.config.option.auth.notSelfDataAccess.role.description"),
            null, // Possible values: ROLE_GUEST, ROLE_USER, ROLE_SUPERVISOR, ROLE_ADMIN
            String.class,
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

    ACTIVEMQ_ENABLE(
            "spring.activemq.pool.enabled",
            n("sumaris.config.option.spring.activemq.pool.enabled.description"),
            "false",
            Boolean.class),

    ACTIVEMQ_BROKER_URL(
            "spring.activemq.broker-url",
            n("sumaris.config.option.spring.activemq.broker-url.description"),
            "vm://embedded?broker.persistent=true",
            String.class),

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
    ;

    /** Configuration key. */
    private final String key;

    /** I18n key of option description */
    private final String description;

    /** Type of option */
    private final Class<?> type;

    /** Default value of option. */
    private String defaultValue;

    /** Flag to not keep option value on disk */
    private boolean isTransient;

    /** Flag to not allow option value modification */
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

    /** {@inheritDoc} */
    @Override
    public String getKey() {
        return key;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransient() {
        return isTransient;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFinal() {
        return isFinal;
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public void setTransient(boolean newValue) {
        // not used
    }

    /** {@inheritDoc} */
    @Override
    public void setFinal(boolean newValue) {
        // not used
    }

}
