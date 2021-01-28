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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.config.SumarisConfigurationOption;
import net.sumaris.core.dao.technical.model.annotation.EntityEnums;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.event.entity.EntityDeleteEvent;
import net.sumaris.core.event.entity.AbstractEntityEvent;
import net.sumaris.core.event.entity.EntityInsertEvent;
import net.sumaris.core.event.entity.EntityUpdateEvent;
import net.sumaris.core.event.schema.SchemaEvent;
import net.sumaris.core.event.schema.SchemaReadyEvent;
import net.sumaris.core.event.schema.SchemaUpdatedEvent;
import net.sumaris.core.dao.technical.model.annotation.EntityEnum;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.DenyDeletionException;
import net.sumaris.core.service.schema.DatabaseSchemaService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.SoftwareVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ApplicationConfigHelper;
import org.nuiton.config.ApplicationConfigProvider;
import org.nuiton.config.ConfigOptionDef;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component("configurationService")
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Log log = LogFactory.getLog(ConfigurationServiceImpl.class);

    private final SumarisConfiguration configuration;
    private final String currentSoftwareLabel;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SoftwareService softwareService;

    @Autowired
    private DatabaseSchemaService databaseSchemaService;

    @Autowired
    private ApplicationEventPublisher publisher;

    private Version dbVersion;

    @Autowired
    public ConfigurationServiceImpl(SumarisConfiguration configuration) {
        this.configuration = configuration;
        this.currentSoftwareLabel = configuration.getAppName();
        Preconditions.checkNotNull(currentSoftwareLabel);
    }

    @Override
    public SoftwareVO getCurrentSoftware() {
        return softwareService.getByLabel(currentSoftwareLabel);
    }

    /* -- event listeners -- */

    @Async
    @EventListener({SchemaUpdatedEvent.class, SchemaReadyEvent.class})
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    protected void onSchemaUpdatedOrReady(SchemaEvent event) {
        if (this.dbVersion == null || !this.dbVersion.equals(event.getSchemaVersion())) {
            this.dbVersion = event.getSchemaVersion();

            // Apply software config
            applySoftwareConfig();

            // Publish ready event
            if (event instanceof SchemaReadyEvent) {
                publisher.publishEvent(new ConfigurationReadyEvent(configuration));
            }
            // Publish update event
            else {
                publisher.publishEvent(new ConfigurationUpdatedEvent(configuration));
            }

        }
    }

    @Async
    @TransactionalEventListener(
            value = {EntityInsertEvent.class, EntityUpdateEvent.class},
            phase = TransactionPhase.AFTER_COMMIT,
            condition = "#event.entityName=='Software'")
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    protected void onSoftwareChanged(AbstractEntityEvent event) {
        SoftwareVO software = (SoftwareVO)event.getData();

        // Test if same as the current software
        boolean isCurrent = (software != null && this.currentSoftwareLabel.equals(software.getLabel()));
        if (isCurrent) {

            // Apply to config
            applySoftwareConfig();

            // Publish update event
            publisher.publishEvent(new ConfigurationUpdatedEvent(configuration));

        }
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            condition = "#event.entityName=='Software'")
    @Transactional(propagation = Propagation.REQUIRED)
    protected void beforeDeleteSoftware(EntityDeleteEvent event) {
        Preconditions.checkNotNull(event.getId());
        SoftwareVO currentSoftware = event.getData() != null ? (SoftwareVO)event.getData() : getCurrentSoftware();

        // Test if same as the current software
        boolean isCurrent = (currentSoftware != null && currentSoftware.getId().equals(event.getId()));

        // Avoid deletion of current software
        if (isCurrent) {
            throw new DenyDeletionException("Cannot delete the current software", ImmutableList.of(String.valueOf(currentSoftware.getId())));
        }
    }

    /* -- protected methods -- */

    protected void applySoftwareConfig() {

        boolean newDatabase = false;

        // Resolved db version, if need
        if (this.dbVersion == null) {
            this.dbVersion = databaseSchemaService.getSchemaVersion().orElse(null);
            newDatabase = this.dbVersion == null;
        }

        // if new database or version > 0.9.5, then apply current software properties to config
        // else skip (because software tables not exists)
        Version minVersion = VersionBuilder.create("0.9.5").build();
        if (newDatabase || minVersion.beforeOrequals(dbVersion)) {
            applySoftwareProperties(configuration.getApplicationConfig(), getCurrentSoftware());
        }
        else {
            log.warn(String.format("Skip using software properties as config options, because schema version < %s. Waiting schema update...", minVersion.toString()));
        }

        // Refresh model enumerations, using config
        updateModelEnumerations();
    }


    protected void applySoftwareProperties(ApplicationConfig appConfig, SoftwareVO software) {
        if (software == null) {
            log.info(String.format("No configuration for {%s} found in database. to enable configuration override from database, make sure to set the option '%s' to an existing row of the table SOFTWARE (column LABEL).", currentSoftwareLabel, SumarisConfigurationOption.APP_NAME.getKey()));
            return; // skip
        }

        Preconditions.checkNotNull(software.getLabel());

        Map<String, String> properties = software.getProperties();
        if (MapUtils.isEmpty(properties)) return; // Skip if empty - TODO: applying defaults ?


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
                if (info) log.info(String.format(" - Skipping unknown configuration option {%s=%s}", key, value));
            } else if (transientOptionKeys.contains(key)) {
                if (info) log.info(String.format(" - Skipping transient configuration option {%s=%s}", key, value));
            } else {
                if (info) log.info(String.format(" - Applying option {%s=%s}", key, value));
                appConfig.setOption(key, value);
            }
        });
    }

    protected void updateModelEnumerations() {

        ApplicationConfig appConfig = configuration.getApplicationConfig();

        boolean debug = log.isDebugEnabled();
        log.info("Updating model enumerations...");
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        // For each enum classes
        EntityEnums.getEntityEnumClasses(configuration).forEach(enumClass -> {
            if (debug) log.debug(String.format("- Processing %s ...", enumClass.getSimpleName()));

            // Get annotation detail
            final EntityEnum annotation = enumClass.getAnnotation(EntityEnum.class);
            final String entityClassName = annotation.entity().getSimpleName();
            final String[] joinAttributes = annotation.joinAttributes();

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

                // Try to resolve, using each join attributes
                Optional<? extends IEntity> entity = Stream.of(joinAttributes).map(joinAttribute -> {
                    Object joinValue = Beans.getProperty(enumValue, joinAttribute);

                    if (joinValue == null) return null; // Skip this attribute

                    // If there is a config option, use it as join value
                    String configOptionKey = configPrefix + StringUtils.doting(entityClassName, enumValue.toString(), joinAttribute);
                    boolean hasConfigOption = appConfig.hasOption(configOptionKey);
                    if (hasConfigOption) {
                        if (joinValue != null) {
                            joinValue = appConfig.getOption(joinValue.getClass(), configOptionKey);
                        }
                        else {
                            joinValue = appConfig.getOption(configOptionKey);
                        }
                    }

                    // Find entities that match the attribute
                    List<? extends IEntity> matchEntities = entityManager.createQuery(String.format(queryPattern, joinAttribute), annotation.entity())
                            .setParameter(1, joinValue)
                            .getResultList();

                    int size = matchEntities.size();
                    if (size == 1) {
                        return matchEntities.get(0);
                    }
                    else {
                        if (IEntity.Fields.ID.equals(joinAttribute)) {
                            enumContentBuilder.append(", ").append(joinAttribute).append(": ").append(joinValue);
                        }
                        else {
                            enumContentBuilder.append(", ").append(joinAttribute).append(": '").append(joinValue).append("'");
                        }
                        configKeysBuilder.append(", '").append(configOptionKey).append("'");
                        return null;
                    }
                })
                        .filter(Objects::nonNull)
                        .findFirst();
                if (entity.isPresent()) {
                    successCounter.incrementAndGet();
                    if (debug) log.debug(String.format("Updating %s with %s", enumValue, entity));

                    // Update the enum
                    Beans.copyProperties(entity.get(), enumValue);
                }
                else {
                    errorCounter.incrementAndGet();
                    log.warn(String.format(" - Missing %s{%s}. Please add it in database, or set configuration option %s",
                            entityClassName,
                            enumContentBuilder.substring(2),
                            configKeysBuilder.substring(2)));
                    Beans.setProperty(enumValue, IEntity.Fields.ID, -1);
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




}
