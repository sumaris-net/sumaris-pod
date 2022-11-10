package net.sumaris.rdf.server.http.rest.actuator;

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