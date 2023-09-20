package net.sumaris.core.service.technical;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.event.config.ConfigurationEventListener;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.AbstractEntityEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.schema.SchemaEvent;
import net.sumaris.core.event.schema.SchemaReadyEvent;
import net.sumaris.core.event.schema.SchemaUpdatedEvent;
import net.sumaris.core.exception.DenyDeletionException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.annotation.EntityEnum;
import net.sumaris.core.model.annotation.EntityEnums;
import net.sumaris.core.service.schema.DatabaseSchemaService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ApplicationConfigHelper;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ConfigOptionDef;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component("configurationService")
@Slf4j
public class ConfigurationServiceImpl implements ConfigurationService {

    private final SumarisConfiguration configuration;
    private final String currentSoftwareLabel;
    private boolean ready;

    private final EntityManager entityManager;

    private final SoftwareService softwareService;

    private final DatabaseSchemaService databaseSchemaService;

    private final ApplicationEventPublisher publisher;

    private Version dbVersion;

    private final List<ConfigurationEventListener> listeners = new CopyOnWriteArrayList<>();

    public ConfigurationServiceImpl(SumarisConfiguration configuration,
                                    EntityManager entityManager,
                                    SoftwareService softwareService,
                                    DatabaseSchemaService databaseSchemaService,
                                    ApplicationEventPublisher publisher) {
        this.configuration = configuration;
        this.entityManager = entityManager;
        this.softwareService = softwareService;
        this.databaseSchemaService = databaseSchemaService;
        this.publisher = publisher;
        this.currentSoftwareLabel = configuration.getAppName();
        Preconditions.checkNotNull(currentSoftwareLabel);
        this.ready = !configuration.enableConfigurationDbPersistence(); // Mark as ready, if configuration not loaded from DB
    }

    @Override
    public SumarisConfiguration getConfiguration() {
        return configuration;
    }


    @Override
    public SoftwareVO getCurrentSoftware() {
        return softwareService.getByLabel(currentSoftwareLabel);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    /* -- event listeners -- */

    @Async
    @EventListener({SchemaUpdatedEvent.class, SchemaReadyEvent.class})
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {PersistenceException.class})
    public void onSchemaUpdatedOrReady(SchemaEvent event) {
        if (this.dbVersion == null || !this.dbVersion.equals(event.getSchemaVersion())) {
            this.dbVersion = event.getSchemaVersion();

            // Configuration override disabled (.e.g UNit test)
            if (!configuration.enableConfigurationDbPersistence()) {
                // Publish ready event
                if (event instanceof SchemaReadyEvent) {
                    publishReadyEvent();
                }
            }

            else {
                // Version < 1.10.0 => Skip applying software properties (will fail on SOFTWARE, because of missing columns)
                if (dbVersion.beforeOrequals(VersionBuilder.create("1.10.0").build())) {
                    log.warn("DB version is prior to 1.10.0 - Cannot applying software properties. Please restart pod, after DB upgrade");
                }
                else {
                    // Update the config, from the software properties
                    applySoftwareProperties();
                }

                // Publish ready event
                if (event instanceof SchemaReadyEvent) {
                    publishReadyEvent();
                }
                // Publish update event
                else {
                    publishUpdateEvent();
                }
            }

            // Mark as ready
            ready = true;
        }
    }

    protected void publishReadyEvent() {
        publishEvent(new ConfigurationReadyEvent(configuration));
    }

    protected void publishUpdateEvent() {
        publishEvent(new ConfigurationUpdatedEvent(configuration));
    }

    @Async
    @TransactionalEventListener(
            value = {EntityInsertEvent.class, EntityUpdateEvent.class},
            phase = TransactionPhase.AFTER_COMMIT,
            condition = "#event.entityName=='Software'")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void onSoftwareChanged(AbstractEntityEvent event) {

        if (!configuration.enableConfigurationDbPersistence()) return; // Skip

        // Check if should be applied into configuration
        SoftwareVO software = (SoftwareVO) event.getData();
        boolean isCurrentSoftware = (software != null && this.currentSoftwareLabel.equals(software.getLabel()));

        if (isCurrentSoftware) {
            ready = false;

            // Restore defaults
            configuration.restoreDefaults();

            // Update the config, from the software properties
            applySoftwareProperties();

            // Clean config cache
            configuration.cleanCache();

            // Publish update event
            publishEvent(new ConfigurationUpdatedEvent(configuration));

            // Mark as ready
            ready = true;
        }
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            condition = "#event.entityName=='Software'")
    @Transactional(propagation = Propagation.REQUIRED)
    public void beforeDeleteSoftware(EntityDeleteEvent event) {
        Preconditions.checkNotNull(event.getId());
        Serializable currentSoftwareId = getCurrentSoftware().getId();
        Serializable deletedSoftwareId = null;
        if (event.getData() != null && event.getData() instanceof IReferentialVO<?>) {
            deletedSoftwareId = ((IReferentialVO) event.getData()).getId();
        }

        // Test if same as the current software
        boolean isCurrent = deletedSoftwareId != null && deletedSoftwareId.equals(currentSoftwareId);

        // Avoid deletion of current software
        if (isCurrent) {
            throw new DenyDeletionException("Cannot delete the current software", ImmutableList.of(String.valueOf(currentSoftwareId)));
        }
    }

    @Override
    public void addListener(ConfigurationEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(ConfigurationEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void applySoftwareProperties() {

        boolean newDatabase = false;

        // Resolved db version, if need
        if (this.dbVersion == null) {
            this.dbVersion = databaseSchemaService.getSchemaVersion().orElse(null);
            newDatabase = this.dbVersion == null;
        }

        // if new database or version > 0.9.5, then apply current software properties to config
        // else skip (because software tables not exists)
        Version minVersion = VersionBuilder.create("0.9.5").build();
        if (newDatabase || dbVersion.after(minVersion)) {
            applySoftwareProperties(configuration.getApplicationConfig(), getCurrentSoftware());
        }
        else {
            log.warn(String.format("Skip using software properties as config options, because schema version < %s. Waiting schema update...", minVersion.toString()));
        }

        // Refresh model enumerations, using config
        updateModelEnumerations();
    }

    /* -- protected methods -- */

    protected void applySoftwareProperties(ApplicationConfig appConfig, SoftwareVO software) {
        if (software == null) {
            log.info(String.format("No configuration for {%s} found in database. to enable configuration override from database, make sure to set the option '%s' to an existing row of the table SOFTWARE (column LABEL).", currentSoftwareLabel, SumarisConfigurationOption.APP_NAME.getKey()));
            return; // skip
        }

        Preconditions.checkNotNull(software.getLabel());

        Map<String, String> properties = software.getProperties();
        if (MapUtils.isEmpty(properties)) return; // Skip if empty

        log.info(String.format("Applying {%s} software properties, as config options...", software.getLabel()));

        // Load options from configuration providers
        Set<ApplicationConfigProvider> providers =
                ApplicationConfigHelper.getProviders(null,
                        null,
                        null,
                        true);
        Set<String> optionKeys = providers.stream()
                .map(ApplicationConfigProvider::getOptions)
                .flatMap(Stream::of)
                .map(ConfigOptionDef::getKey)
                .collect(Collectors.toSet());
        Set<String> transientOptionKeys = providers.stream()
                .map(ApplicationConfigProvider::getOptions)
                .flatMap(Stream::of)
                .filter(ConfigOptionDef::isTransient)
                .map(ConfigOptionDef::getKey)
                .collect(Collectors.toSet());

        boolean info = log.isInfoEnabled();

        properties.forEach((key, value) -> {
            if (!optionKeys.contains(key)) {
                if (info) log.debug(String.format(" - Skipping unknown configuration option {%s=%s}", key, value));
            } else if (transientOptionKeys.contains(key)) {
                if (info) log.warn(String.format(" - Skipping transient configuration option {%s=%s}", key, value));
            } else {
                if (info) log.info(String.format(" - Applying option {%s=%s}", key, value));
                appConfig.setOption(key, value);
            }
        });
    }

    protected void updateModelEnumerations() {

        ApplicationConfig appConfig = configuration.getApplicationConfig();

        log.info("Updating model enumerations...");
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        // For each enum classes
        EntityEnums.getEntityEnumClasses(configuration).forEach(enumClass -> {
            log.debug("- Processing {} ...", enumClass.getSimpleName());

            // Get annotation detail
            final EntityEnum annotation = enumClass.getAnnotation(EntityEnum.class);
            final String entityClassName = annotation.entity().getSimpleName();
            final String[] resolveAttributes = annotation.resolveAttributes();
            final String[] configAttributes = annotation.configAttributes();
            // Create a sorted array, with first
            final Set<String> sortedAttributes = Sets.newLinkedHashSet();
            if (ArrayUtils.isNotEmpty(configAttributes)) sortedAttributes.addAll(Arrays.asList(configAttributes));
            if (ArrayUtils.isNotEmpty(resolveAttributes)) sortedAttributes.addAll(Arrays.asList(resolveAttributes));

            // Compute a option key (e.g. 'sumaris.enumeration.MyEntity.MY_ENUM_VALUE.id')
            String tempConfigPrefix = StringUtils.defaultIfBlank(annotation.configPrefix(), "");
            if (tempConfigPrefix.lastIndexOf(".") != tempConfigPrefix.length() - 1) {
                // Add trailing point
                tempConfigPrefix += ".";
            }
            final String configPrefix = tempConfigPrefix;

            final String queryPattern = String.format("from %s where %s = ?1",
                    entityClassName,
                    "%s");

            StringBuilder enumContentBuilder = new StringBuilder();
            StringBuilder configKeysBuilder = new StringBuilder();

            // For each enum values
            Arrays.stream(enumClass.getEnumConstants()).forEach(enumValue -> {
                counter.incrementAndGet();
                // Reset log buffer
                enumContentBuilder.setLength(0);
                configKeysBuilder.setLength(0);

                // Try to resolve, using each attributes
                Optional<? extends IEntity> entity = sortedAttributes.stream().map(attribute -> {
                    Object joinValue = Beans.getProperty(enumValue, attribute);

                    // If there is a config option, use it as join value
                    String configOptionKey = configPrefix + StringUtils.doting(entityClassName, enumValue.toString(), attribute);
                    boolean enableConfigOverride = ArrayUtils.isEmpty(configAttributes) || ArrayUtils.contains(configAttributes, attribute);
                    if (enableConfigOverride) {
                        boolean hasConfigOption = appConfig.hasOption(configOptionKey);
                        if (hasConfigOption) {
                            if (joinValue != null) {
                                joinValue = appConfig.getOption(joinValue.getClass(), configOptionKey);
                            }
                            else {
                                joinValue = appConfig.getOption(configOptionKey);
                            }
                        }
                        // Nothing in the config option, and not a resolve attribute => skip
                        else if (!ArrayUtils.contains(resolveAttributes, attribute)) {
                            return null;
                        }
                    }

                    if (joinValue == null) return null; // Skip this attribute

                    // Find entities that match the attribute
                    List<? extends IEntity> matchEntities;
                    try {
                        matchEntities = entityManager.createQuery(String.format(queryPattern, attribute), annotation.entity())
                            .setParameter(1, joinValue)
                            .getResultList();
                    }
                    catch (PersistenceException e) {
                        if (log.isDebugEnabled()) {
                            log.error("Unable to load entities for class {}: {}", entityClassName, e.getMessage(), e);
                        }
                        else {
                            log.error("Unable to load entities for class {}: {}", entityClassName, e.getMessage());
                        }
                        return null;
                    }

                    if (CollectionUtils.size(matchEntities) == 1) {
                        return matchEntities.get(0);
                    }
                    else {
                        if (IEntity.Fields.ID.equals(attribute)) {
                            enumContentBuilder.append(", ").append(attribute).append(": ").append(joinValue);
                        }
                        else {
                            enumContentBuilder.append(", ").append(attribute).append(": '").append(joinValue).append("'");
                        }
                        if (enableConfigOverride) configKeysBuilder.append(", '").append(configOptionKey).append("'");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
                if (entity.isPresent()) {
                    successCounter.incrementAndGet();
                    log.debug("Updating {} with {}", enumValue, entity.get());

                    // Update the enum
                    Beans.copyProperties(entity.get(), enumValue);
                }
                else {
                    errorCounter.incrementAndGet();
                    log.warn(String.format(" - Missing %s{%s}. Please add it in database, or set configuration option %s",
                            entityClassName,
                            enumContentBuilder.length() > 2 ? enumContentBuilder.substring(2) : "null",
                            configKeysBuilder.length() > 2 ? configKeysBuilder.substring(2) : "<unknown>"));
                    Beans.setProperty(enumValue, IEntity.Fields.ID, EntityEnums.UNRESOLVED_ENUMERATION_ID);
                }
            });
        });

        String logMessage = String.format("Model enumerations updated (%s enumerations: %s updates, %s errors)",
                counter.get(),
                successCounter.get(),
                errorCounter.get());
        if (errorCounter.get() > 0) log.error(logMessage);
        else log.info(logMessage);
    }


    protected void publishEvent(ConfigurationUpdatedEvent event) {
        // Emit to Spring event bus
        publisher.publishEvent(event);

        // Emit to registered listeners
        for (ConfigurationEventListener listener: listeners) {
            try {
                listener.onUpdated(event);
            } catch (Throwable t) {
                log.error("Error on configuration updated event listener: " + t.getMessage(), t);
            }
        }
    }

    protected void publishEvent(ConfigurationReadyEvent event) {
        try {
            // Emit to Spring event bus
            publisher.publishEvent(event);
        } catch (Throwable t) {
            log.error("Error after publishing configuration ready event: " + t.getMessage(), t);
        }

        // Emit to registered listeners
        for (ConfigurationEventListener listener: listeners) {
            try {
                listener.onReady(event);
            } catch (Throwable t) {
                log.error("Error on configuration ready event listener: " + t.getMessage(), t);
            }
        }
    }

}
