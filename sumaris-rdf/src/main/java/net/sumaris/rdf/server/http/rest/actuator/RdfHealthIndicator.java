package net.sumaris.rdf.server.http.rest.actuator;

/*-
 * #%L
 * SUMARiS:: RDF features
 * %%
 * Copyright (C) 2018 - 2022 SUMARiS Consortium
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

import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.ModelType;
import net.sumaris.rdf.core.service.store.RdfDatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass({HealthIndicator.class})
@ConditionalOnWebApplication
@ConditionalOnProperty(
    prefix = "management.health.rdf",
    name = "enabled",
    matchIfMissing = true
)
public class RdfHealthIndicator implements HealthIndicator {
    private static final String PROVIDER_KEY = "provider";
    private static final String MODEL_BASE_URI_KEY = "uri";
    private static final String MODEL_SCHEMA_URI_KEY = "schemaUri";
    private static final String MODEL_DATA_URI_KEY = "dataUri";
    private static final String MODEL_VERSION_KEY = "modelVersion";
    private static final String MODEL_PREFIX_KEY = "modelPrefix";

    @Autowired(required = false)
    private RdfDatasetService datasetService;

    @Autowired(required = false)
    private RdfConfiguration configuration;

    @Override
    public Health health() {
        if (!isRunningService()) {
            return Health.down()
                .withDetail(PROVIDER_KEY, "Not Available").build();
        }

        return Health.up()
            .withDetail(PROVIDER_KEY, datasetService.getProviderName())
            .withDetail(MODEL_PREFIX_KEY, configuration.getModelPrefix())
            .withDetail(MODEL_VERSION_KEY, configuration.getModelVersion())
            .withDetail(MODEL_BASE_URI_KEY, configuration.getModelBaseUri())
            .withDetail(MODEL_SCHEMA_URI_KEY, configuration.getModelTypeUri(ModelType.SCHEMA))
            .withDetail(MODEL_DATA_URI_KEY, configuration.getModelTypeUri(ModelType.DATA))
            .build();
    }

    private Boolean isRunningService() {
        return (datasetService != null && configuration != null);
    }
}
