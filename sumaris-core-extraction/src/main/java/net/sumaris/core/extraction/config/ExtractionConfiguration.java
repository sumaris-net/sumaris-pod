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

package net.sumaris.core.extraction.config;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import org.nuiton.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

@Slf4j
public class ExtractionConfiguration {

    private static ExtractionConfiguration INSTANCE;

    public static ExtractionConfiguration instance() {
        if (INSTANCE == null) {
            SumarisConfiguration delegate = SumarisConfiguration.getInstance();
            Preconditions.checkNotNull(delegate, "SumarisConfiguration not initialized!");
            INSTANCE = new ExtractionConfiguration(delegate);
        }
        return INSTANCE;
    }

    public static void setInstance(ExtractionConfiguration instance) {
        INSTANCE = instance;
    }

    private final SumarisConfiguration delegate;

    @Autowired
    public ExtractionConfiguration(SumarisConfiguration configuration){
        this.delegate = configuration;
        setInstance(this);
    }

    public String getExtractionCliOutputFormat() {
        return getApplicationConfig().getOption(ExtractionConfigurationOption.EXTRACTION_CLI_OUTPUT_FORMAT.getKey());
    }

    public ProcessingFrequencyEnum getExtractionCliFrequency() {
        String value = getApplicationConfig().getOption(ExtractionConfigurationOption.EXTRACTION_CLI_FREQUENCY.getKey());
        try {
            return ProcessingFrequencyEnum.valueOf(value);
        }
        catch (IllegalArgumentException e) {
            throw new SumarisTechnicalException(String.format("Invalid frequency '%s'. Available values: %s",
                value,
                ProcessingFrequencyEnum.values()
            ));
        }
    }

    public String getJdbcURL() {
        return delegate.getJdbcURL();
    }

    public boolean enableExtractionProduct() {
        return getApplicationConfig().getOptionAsBoolean(ExtractionConfigurationOption.EXTRACTION_PRODUCT_ENABLE.getKey());
    }

    public boolean enableTechnicalTablesUpdate() {
        return getApplicationConfig().getOptionAsBoolean(SumarisConfigurationOption.ENABLE_TECHNICAL_TABLES_UPDATE.getKey());
    }

    public File getTempDirectory() {
        return getApplicationConfig().getOptionAsFile(SumarisConfigurationOption.TMP_DIRECTORY.getKey());
    }

    public ApplicationConfig getApplicationConfig() {
        return delegate.getApplicationConfig();
    }

    /**
     * Extraction query timeout, in millisecond
     * @return
     */
    public int getExtractionQueryTimeout() {
        return getApplicationConfig().getOptionAsInt(ExtractionConfigurationOption.EXTRACTION_QUERY_TIMEOUT.getKey());
    }

    public String getCsvSeparator() {
        return delegate.getCsvSeparator();
    }

}