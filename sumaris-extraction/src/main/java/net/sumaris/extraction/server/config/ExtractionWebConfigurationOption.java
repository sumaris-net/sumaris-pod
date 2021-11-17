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

package net.sumaris.extraction.server.config;

import org.nuiton.config.ConfigOptionDef;

import static org.nuiton.i18n.I18n.n;

public enum ExtractionWebConfigurationOption implements ConfigOptionDef {

    // Same as SumarisServerConfigurationOption.SERVER_URL
    SERVER_URL(
            "server.url",
            n("sumaris.config.option.server.url.description"),
            "${server.protocol}://${server.host}:${server.port}",
            String.class,
            false),

    ACCESS_NOT_SELF_EXTRACTION_MIN_ROLE(
            "sumaris.extraction.accessNotSelfExtraction.role",
            n("sumaris.config.option.extraction.accessNotSelfExtraction.role.description"),
            "ROLE_ADMIN", // Possible values: ROLE_GUEST, ROLE_USER, ROLE_SUPERVISOR, ROLE_ADMIN
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

    ExtractionWebConfigurationOption(String key,
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

    ExtractionWebConfigurationOption(String key,
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
