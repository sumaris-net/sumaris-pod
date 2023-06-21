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

package net.sumaris.extraction.core.config;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import org.nuiton.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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

        // Define Alias
        // Already done by provider class
        //addAlias(this.delegate.getApplicationConfig());


    }

    public boolean isProduction() {
        return this.delegate.isProduction();
    }

    public String getExtractionCliOutputFormat() {
        return getApplicationConfig().getOption(ExtractionConfigurationOption.EXTRACTION_CLI_OUTPUT_FORMAT.getKey());
    }

    public String getCliFilterProgramLabel() {
        return delegate.getCliFilterProgramLabel();
    }
    public List<Integer> getCliFilterTripIds() {
        return delegate.getCliFilterTripIds();
    }
    public List<Integer> getCliFilterOperationIds() {
        return delegate.getCliFilterOperationIds();
    }
    public Integer getCliFilterYear() {
        return delegate.getCliFilterYear();
    }

    public ProcessingFrequencyEnum getExtractionCliFrequency() {
        String value = getApplicationConfig().getOption(ExtractionConfigurationOption.EXTRACTION_CLI_FREQUENCY.getKey());
        try {
            return ProcessingFrequencyEnum.valueOf(value);
        }
        catch (IllegalArgumentException e) {
            throw new SumarisTechnicalException(String.format("Invalid frequency '%s'. Available values: %s",
                value,
                Arrays.toString(ProcessingFrequencyEnum.values())
            ));
        }
    }

    public String getJdbcURL() {
        return delegate.getJdbcURL();
    }

    public boolean enableCache() {
        return getApplicationConfig().getOptionAsBoolean(SumarisConfigurationOption.CACHE_ENABLED.getKey());
    }

    public boolean enableExtractionProduct() {
        return getApplicationConfig().getOptionAsBoolean(ExtractionConfigurationOption.EXTRACTION_PRODUCT_ENABLE.getKey());
    }

    public boolean enableExtractionScheduling() {
        return getApplicationConfig().getOptionAsBoolean(ExtractionConfigurationOption.EXTRACTION_SCHEDULING_ENABLED.getKey());
    }

    public CacheTTL getExtractionCacheDefaultTtl() {
        return getApplicationConfig().getOption(CacheTTL.class, ExtractionConfigurationOption.EXTRACTION_CACHE_DEFAULT_TTL.getKey());
    }

    public boolean enableTechnicalTablesUpdate() {
        return getApplicationConfig().getOptionAsBoolean(SumarisConfigurationOption.ENABLE_TECHNICAL_TABLES_UPDATE.getKey());
    }

    public boolean enableAdagioOptimization() {
        return getApplicationConfig().getOptionAsBoolean(SumarisConfigurationOption.ENABLE_ADAGIO_OPTIMIZATION.getKey());
    }

    public String getAdagioSchema() {
        return getApplicationConfig().getOption(SumarisConfigurationOption.DB_ADAGIO_SCHEMA.getKey());
    }

    public boolean enableBatchDenormalization() {
        return getApplicationConfig().getOptionAsBoolean(ExtractionConfigurationOption.EXTRACTION_BATCH_DENORMALISATION_ENABLE.getKey());
    }

    public void setEnableBatchDenormalization(boolean value) {
        getApplicationConfig().setOption(ExtractionConfigurationOption.EXTRACTION_BATCH_DENORMALISATION_ENABLE.getKey(), String.valueOf(value));
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

    public String getExtractionDefaultLandingCategory() {
        return getApplicationConfig().getOption(ExtractionConfigurationOption.EXTRACTION_LANDING_CATEGORY_DEFAULT.getKey());
    }
    public String getExtractionDefaultCommercialSizeCategoryScale() {
        return getApplicationConfig().getOption(ExtractionConfigurationOption.EXTRACTION_COMMERCIAL_SIZE_CATEGORY_SCALE_DEFAULT.getKey());
    }

    public char getCsvSeparator() {
        return delegate.getCsvSeparator();
    }

    /**
     * Add alias to the given ApplicationConfig. <p/>
     * This method could be override to add specific alias
     *
     * @param applicationConfig a {@link ApplicationConfig} object.
     */
    protected void addAlias(ApplicationConfig applicationConfig) {
        // CLI options
        applicationConfig.addAlias("--format", "--option", ExtractionConfigurationOption.EXTRACTION_CLI_OUTPUT_FORMAT.getKey());
        applicationConfig.addAlias("--frequency", "--option", ExtractionConfigurationOption.EXTRACTION_CLI_FREQUENCY.getKey());
    }

}
