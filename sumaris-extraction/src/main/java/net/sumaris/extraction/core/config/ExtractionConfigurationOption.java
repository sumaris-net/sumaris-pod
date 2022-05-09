package net.sumaris.extraction.core.config;

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

import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import org.nuiton.config.ConfigOptionDef;

import static org.nuiton.i18n.I18n.n;

/**
 * All application configuration options.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public enum ExtractionConfigurationOption implements ConfigOptionDef {

    // ------------------------------------------------------------------------//
    // -- READ-WRITE OPTIONS ---------------------------------------------------//
    // ------------------------------------------------------------------------//

    /*
    * Application options
    */

    EXTRACTION_ENABLED(
            "sumaris.extraction.enabled",
            n("sumaris.config.option.extraction.enabled.description"),
            Boolean.TRUE.toString(),
            Boolean.class,
            false),

    EXTRACTION_CLI_OUTPUT_FORMAT(
            "sumaris.extraction.cli.output.format",
            n("sumaris.config.option.extraction.cli.output.format.description"),
            LiveExtractionTypeEnum.RDB.getLabel(),
            String.class,
            false),

    EXTRACTION_CLI_FREQUENCY(
            "sumaris.extraction.cli.frequency",
            n("sumaris.config.option.extraction.cli.frequency.description"),
            ProcessingFrequencyEnum.DAILY.name(),
            String.class,
            false),

    EXTRACTION_PRODUCT_ENABLE(
            "sumaris.extraction.product.enable",
            n("sumaris.config.option.extraction.product.enable.description"),
            Boolean.FALSE.toString(),
            Boolean.class,
        false),

    EXTRACTION_CACHE_DEFAULT_TTL(
        "sumaris.extraction.cache.ttl.default",
        n("sumaris.config.option.extraction.ttl.default.description"),
        CacheTTL.SHORT.name(),
        CacheTTL.class,
        false),

    EXTRACTION_MAP_ENABLE(
        "sumaris.extraction.map.enable",
        n("sumaris.config.option.extraction.map.enable.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    EXTRACTION_QUERY_TIMEOUT(
        "sumaris.extraction.query.timeout",
        n("sumaris.config.option.extraction.query.timeout.description"),
        String.valueOf(5 * 60 * 1000), // 5min
        Integer.class,
        false)
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

    ExtractionConfigurationOption(String key,
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

    ExtractionConfigurationOption(String key,
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
