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

package net.sumaris.rdf.config;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@ConditionalOnProperty(
        prefix = "rdf",
        name = {"enabled"},
        matchIfMissing = true)
public class RdfConfiguration  {

    @Resource(name = "sumarisConfiguration")
    private SumarisConfiguration config;

    private String modelPrefix;

    public boolean isRdfEnable() {
        return config.getApplicationConfig().getOptionAsBoolean(RdfConfigurationOption.RDF_ENABLED.getKey());
    }


    public String getModelUriPrefix() {
        if (this.modelPrefix != null) return this.modelPrefix;

        // Init property, if not init yet
        String modelPrefix = config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_URI_PREFIX.getKey());
        Preconditions.checkNotNull(modelPrefix, String.format("Missing configuration option {%s}", RdfConfigurationOption.RDF_MODEL_URI_PREFIX.getKey()));
        if (modelPrefix.lastIndexOf('/') != modelPrefix.length() - 1) {
            modelPrefix += "/";
        }

        this.modelPrefix = modelPrefix;
        return modelPrefix;
    }

    public String getModelNamespace() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_NS.getKey()).toLowerCase();
    }

    public String getModelVersion() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_VERSION.getKey());
    }

    public String getModelTitle() {
        return config.getAppName();
    }

    public String getModelDefaultLanguage() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_LANGUAGE.getKey());
    }

    public String getModelLabel() {
        return config.getAppName();
    }


    public String getModelDescription() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_DESCRIPTION.getKey());
    }

    public String getModelComment() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_COMMENT.getKey());
    }

    public String getModelDate() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_DATE.getKey());
    }

    public String getModelLicense() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_LICENSE.getKey());
    }

    public String getModelAuthors() {
        return config.getApplicationConfig().getOption(RdfConfigurationOption.RDF_MODEL_AUTHORS.getKey());
    }


}
