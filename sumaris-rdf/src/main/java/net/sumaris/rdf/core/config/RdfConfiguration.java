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

package net.sumaris.rdf.core.config;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.rdf.core.util.RdfFormat;
import org.nuiton.config.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Optional;

@Slf4j
public class RdfConfiguration  {

    private static RdfConfiguration INSTANCE;

    public static RdfConfiguration instance() {
        if (INSTANCE == null) {
            SumarisConfiguration delegate = SumarisConfiguration.getInstance();
            Preconditions.checkNotNull(delegate, "SumarisConfiguration not initialized!");
            INSTANCE = new RdfConfiguration(delegate);
        }
        return INSTANCE;
    }

    public static void setInstance(RdfConfiguration instance) {
        INSTANCE = instance;
    }

    private final SumarisConfiguration delegate;


    private String cachedModelBaseUri;

    @Autowired
    public RdfConfiguration(SumarisConfiguration configuration){
        this.delegate = configuration;
        setInstance(this);
    }


    public boolean isRdfEnable() {
        return getApplicationConfig().getOptionAsBoolean(RdfConfigurationOption.RDF_ENABLED.getKey());
    }

    public boolean isTdb2DatasetEnable() {
        return getApplicationConfig().getOptionAsBoolean(RdfConfigurationOption.RDF_TDB2_ENABLED.getKey());
    }

    public boolean isRdfImportEnable() {
        return getApplicationConfig().getOptionAsBoolean(RdfConfigurationOption.RDF_DATA_IMPORT_ENABLED.getKey());
    }

    public File getRdfDirectory() {
        return getApplicationConfig().getOptionAsFile(RdfConfigurationOption.RDF_DIRECTORY.getKey());
    }

    public File getRdfTdb2Directory() {
        return getApplicationConfig().getOptionAsFile(RdfConfigurationOption.RDF_TDB2_DIRECTORY.getKey());
    }

    public File getTempDirectory() {
        return getApplicationConfig().getOptionAsFile(SumarisConfigurationOption.TMP_DIRECTORY.getKey());
    }

    public String getModelBaseUri() {
        if (this.cachedModelBaseUri != null) return this.cachedModelBaseUri;

        // Init property, if not init yet
        String modelPrefix = getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_BASE_URI.getKey());
        Preconditions.checkNotNull(modelPrefix, String.format("Missing configuration option {%s}", RdfConfigurationOption.RDF_MODEL_BASE_URI.getKey()));
        if (modelPrefix.lastIndexOf('/') != modelPrefix.length() - 1) {
            modelPrefix += "/";
        }

        this.cachedModelBaseUri = modelPrefix;
        return modelPrefix;
    }

    public String getModelPrefix() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_PREFIX.getKey()).toLowerCase();
    }

    public String getModelVersion() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_VERSION.getKey());
    }

    public String getModelTitle() {
        return delegate.getAppName();
    }

    public String getModelDefaultLanguage() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_LANGUAGE.getKey());
    }

    public String getModelLabel() {
        return delegate.getAppName();
    }


    public String getModelDescription() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_DESCRIPTION.getKey());
    }

    public String getModelComment() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_COMMENT.getKey());
    }

    public String getModelDate() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_DATE.getKey());
    }

    public String getModelLicense() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_LICENSE.getKey());
    }

    public String getModelAuthors() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_AUTHORS.getKey());
    }

    public String getModelPublisher() {
        return getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_PUBLISHER.getKey());
    }

    public int getDefaultPageSize() {
        return getApplicationConfig().getOptionAsInt(RdfConfigurationOption.RDF_DEFAULT_PAGE_SIZE.getKey());
    }

    public int getMaxPageSize() {
        return getApplicationConfig().getOptionAsInt(RdfConfigurationOption.RDF_MAX_PAGE_SIZE.getKey());
    }

    public String[] getDataImportDbEntities() {
        String str = getApplicationConfig().getOption(RdfConfigurationOption.RDF_DATA_IMPORT_DB_ENTITIES.getKey());
        return str != null ? str.split(",") : null;
    }

    public boolean enableDataImportFromExternal() {
        return  getApplicationConfig().getOptionAsBoolean(RdfConfigurationOption.RDF_DATA_IMPORT_EXTERNAL.getKey());
    }

    public Optional<RdfFormat> getRdfOutputFormat() {
        String userFormat = getApplicationConfig().getOption(RdfConfigurationOption.RDF_OUTPUT_FORMAT.getKey());
        return RdfFormat.fromUserString(userFormat);
    }

    public File getCliOutputFile() {
        return delegate.getCliOutputFile();
    }

    public ApplicationConfig getApplicationConfig() {
        return delegate.getApplicationConfig();
    }

    /* -- protected functions -- */

}
