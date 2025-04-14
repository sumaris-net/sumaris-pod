package net.sumaris.core.config;

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

import net.sumaris.core.dao.technical.hibernate.spatial.dialect.HSQLSpatialDialect;
import net.sumaris.core.util.Geometries;
import org.nuiton.config.ConfigOptionDef;
import org.nuiton.version.Version;

import javax.persistence.LockModeType;
import java.io.File;
import java.net.URL;
import java.util.Locale;

import static org.nuiton.i18n.I18n.n;

/**
 * All application configuration options.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public enum SumarisConfigurationOption implements ConfigOptionDef {

    // ------------------------------------------------------------------------//
    // -- READ-ONLY OPTIONS ---------------------------------------------------//
    // ------------------------------------------------------------------------//

    APP_NAME(
        "sumaris.name",
        n("sumaris.config.option.app.name.description"),
        "SUMARiS",
        String.class),

    VERSION(
        "sumaris.version",
        n("sumaris.config.option.app.version.description"),
        "1.0.0",
        Version.class),

    BASEDIR(
        "sumaris.basedir",
        n("sumaris.config.option.basedir.description"),
        "${user.home}/.${sumaris.name}",
        File.class),

    DATA_DIRECTORY(
        "sumaris.data.directory",
        n("sumaris.config.option.data.directory.description"),
        "${sumaris.basedir}/data",
        File.class),

    I18N_DIRECTORY(
        "sumaris.i18n.directory",
        n("sumaris.config.option.i18n.directory.description"),
        "${sumaris.basedir}/i18n",
        File.class),

    TMP_DIRECTORY(
        "sumaris.tmp.directory",
        n("sumaris.config.option.tmp.directory.description"),
        "${sumaris.data.directory}/temp",
        File.class),

    CACHE_DIRECTORY(
        "sumaris.cache.directory",
        n("sumaris.config.option.cache.directory.description"),
        "${sumaris.data.directory}/cache",
        File.class),

    DB_DIRECTORY(
        "sumaris.persistence.db.directory",
        n("sumaris.config.option.persistence.db.directory.description"),
        "${sumaris.data.directory}/db",
        File.class),

    DB_NAME(
        "sumaris.persistence.db.name",
        n("sumaris.config.option.persistence.db.name.description"),
        "sumaris",
        String.class),

    DB_HOST(
        "sumaris.persistence.db.host",
        n("sumaris.config.option.persistence.db.host.description"),
        "",
        String.class),

    DB_PORT(
        "sumaris.persistence.db.port",
        n("sumaris.config.option.persistence.db.port.description"),
        "",
        String.class),

    DB_VALIDATION_QUERY(
        "sumaris.persistence.db.validation-query",
        n("sumaris.persistence.db.validation-query.description"),
        "SELECT COUNT(*) FROM SYSTEM_VERSION",
        String.class),

    DB_TIMEZONE(
        "sumaris.persistence.db.timezone", // /!\ key used by sumaris-app - DO NOT CHANGED
        n("sumaris.config.option.db.timezone.description"),
        "${user.timezone}",
        String.class,
        true),

    DB_CREATE_SCRIPT_PATH(
        "sumaris.persistence.db.script",
        n("sumaris.config.option.db.script.description"),
        "classpath:net/sumaris/core/db/changelog/${spring.sql.init.platform}/sumaris.script",
        String.class,
        false),

    JDBC_USERNAME(
        "spring.datasource.username",
        n("sumaris.config.option.spring.datasource.username.description"),
        "sa",
        String.class),

    JDBC_PASSWORD(
        "spring.datasource.password",
        n("sumaris.config.option.spring.datasource.password.description"),
        "",
        String.class),

    JDBC_URL(
        "spring.datasource.url",
        n("sumaris.config.option.spring.datasource.url.description"),
        "jdbc:hsqldb:file:${sumaris.persistence.db.directory}/${sumaris.persistence.db.name}",
        String.class),

    JDBC_CATALOG(
        "spring.jpa.properties.hibernate.default_catalog",
        n("sumaris.config.option.hibernate.default_catalog.description"),
        "PUBLIC",
        String.class),

    JDBC_SCHEMA(
        "spring.jpa.properties.hibernate.default_schema",
        n("sumaris.config.option.hibernate.default_schema.description"),
        "PUBLIC",
        String.class),

    JDBC_DRIVER(
        "spring.datasource.driver-class-name",
        n("sumaris.config.option.spring.datasource.driver-class-name.description"),
        "org.hsqldb.jdbc.JDBCDriver",
        Class.class),

    JDBC_BATCH_SIZE(
        "sumaris.persistence.jdbc.batch-size",
        n("sumaris.config.option.persistence.jdbc.batch-size.description"),
        "15",
        Integer.class),

    HIBERNATE_DIALECT(
        "spring.jpa.database-platform",
        n("sumaris.config.option.spring.jpa.database-platform.description"),
        HSQLSpatialDialect.class.getName(),
        Class.class),

    HIBERNATE_ENTITIES_PACKAGE(
        "sumaris.persistence.hibernate.entities.package",
        n("sumaris.config.option.persistence.hibernate.entities.package.description"),
        "net.sumaris.core.model",
        Class.class),

    HIBERNATE_JDBC_TIMEZONE(
        "spring.jpa.properties.hibernate.jdbc.time_zone",
        n("sumaris.config.option.spring.jpa.properties.hibernate.jdbc.time_zone.description"),
        "${sumaris.persistence.db.timezone}", // Redirection to db timezone
        String.class,
        false),

    DEBUG_ENTITY_LOAD(
        "sumaris.persistence.hibernate.load.debug",
        n("sumaris.config.option.persistence.hibernate.load.debug.description"),
        Boolean.FALSE.toString(),
        boolean.class),

    SITE_URL(
        "sumaris.site.url",
        n("sumaris.config.option.site.url.description"),
        "https://www.sumaris.net",
        URL.class),

    SITE_DOC_URL(
        "sumaris.site.doc.url",
        n("sumaris.config.option.site.doc.url.description"),
        "http://doc.e-is.pro/sumaris",
        URL.class),

    ORGANIZATION_NAME(
        "sumaris.organizationName",
        n("sumaris.config.option.organizationName.description"),
        "SUMARiS consortium, E-IS.pro",
        String.class),

    INCEPTION_YEAR(
        "sumaris.inceptionYear",
        n("sumaris.config.option.inceptionYear.description"),
        "2011",
        Integer.class),

    // ------------------------------------------------------------------------//
    // -- REFERENTIAL CONSTANTS --------------------------------------------------//
    // ------------------------------------------------------------------------//

    REFERENTIAL_DEFAULT_CACHE_ENABLE("sumaris.referential.cache.enable",
        n("sumaris.config.option.referential.cache.enable.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    // ------------------------------------------------------------------------//
    // -- DATA CONSTANTS --------------------------------------------------//
    // ------------------------------------------------------------------------//

    DATA_VESSEL_TYPE_IDS(
        "sumaris.data.vessel.type.ids",
        n("sumaris.config.option.data.vessel.type.ids.description"),
        null, // "${sumaris.enumeration.VesselType.FISHING_VESSEL.id}",
        String.class,
        false),

    DATA_FILE_LOCK_ENABLE(
        "sumaris.data.file.lock.enable",
        n("sumaris..config.option.data.file.lock.description"),
        Boolean.TRUE.toString(), Boolean.class, false),

    MEASUREMENT_FILE_DIRECTORY(
        "sumaris.data.measurement.directory",
        n("sumaris.config.option.data.measurement.directory.description"),
        "${sumaris.data.directory}/meas_files",
        File.class),

    DATA_IMAGES_ENABLE("sumaris.data.images.enable",
        n("sumaris.config.option.data.images.enable.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    DATA_IMAGES_DIRECTORY(
        "sumaris.data.images.directory",
        n("sumaris.config.option.data.images.directory.description"),
        "${sumaris.data.directory}/photos",
        File.class),

    PARAMETER_LABEL_LENGTH(
         "sumaris.list.parameter.length.labels",
            n("sumaris.config.option.list.parameter.length.labels.description"),
            null,
            String.class,
            false),

    DATA_IMAGES_DIRECTORY_ENABLE("sumaris.data.images.directory.enable",
        n("sumaris.config.option.data.images.directory.enable.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    IMPORT_NB_YEARS_DATA_HISTORY(
        "sumaris.synchro.import.nbYearDataHistory",
        n("sumaris.config.option.synchro.import.nbYearDataHistory.description"),
        "2",
        Integer.class,
        false),

    IMPORT_DATA_MAX_ROOT_ROW_COUNT(
        "sumaris.synchro.import.data.maxRootRowCount",
        n("sumaris.config.option.synchro.import.data.maxRootRowCount.description"),
        "-1",
        Integer.class,
        false),

    EXPORT_DATA_UPDATE_DATE_DELAY(
        "sumaris.synchro.export.updateDate.offset",
        n("sumaris.config.option.synchro.export.data.updateDate.offset.description"),
        String.valueOf(5 * 60), /* 5 min */
        Integer.class,
        false),

    EXPORT_DATA_UPDATE_DATE_SHORT_DELAY(
        "sumaris.synchro.export.updateDate.offset.short",
        n("sumaris.config.option.synchro.export.data.updateDate.offset.short.description"),
        String.valueOf(30), /* 30 sec */
        Integer.class,
        false),

    IMPORT_REFERENTIAL_UPDATE_DATE_OFFSET(
        "sumaris.synchro.import.updateDate.offset",
        n("sumaris.config.option.synchro.import.referential.updateDate.offset.description"),
        String.valueOf(-60), /* -1 min */
        Integer.class,
        false),

    IMPORT_REFERENTIAL_STATUS_INCLUDES(
        "sumaris.synchro.import.referential.status.includes",
        n("sumaris.config.option.synchro.import.referential.status.includes.description"),
        null, /* all */
        String.class,
        false),

    SYNCHRO_PROGRAM_CODES_INCLUDES(
        "sumaris.synchro.program.codes",
        n("sumaris.config.option.synchro.program.codes.description"),
        "",
        String.class,
        false),

    TIMEZONE(
        "sumaris.timezone",
        n("sumaris.config.option.timezone.description"),
        "",
        String.class,
        false),

    // ------------------------------------------------------------------------//
    // -- READ-WRITE OPTIONS --------------------------------------------------//
    // ------------------------------------------------------------------------//

    DB_BACKUP_DIRECTORY(
        "sumaris.persistence.db.backup.directory",
        n("sumaris.config.option.persistence.db.backup.directory.description"),
        "${sumaris.data.directory}/db-backup",
        File.class,
        false),
    TRASH_DIRECTORY(
        "sumaris.persistence.trash.directory",
        n("sumaris.config.option.persistence.trash.directory.description"),
        "${sumaris.data.directory}/trash",
        File.class,
        false),

    ENABLE_ENTITY_TRASH(
        "sumaris.persistence.trash.enable",
        n("sumaris.config.option.persistence.trash.enable.description"),
        Boolean.TRUE.toString(),
        boolean.class,
        false),

    HIBERNATE_SHOW_SQL(
        "spring.jpa.show-sql",
        n("sumaris.config.option.spring.jpa.show-sql.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    HIBERNATE_USE_SQL_COMMENT(
        "sumaris.persistence.hibernate.useSqlComment",
        n("sumaris.config.option.persistence.hibernate.useSqlComment.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    HIBERNATE_FORMAT_SQL(
        "spring.jpa.properties.hibernate.format_sql",
        n("sumaris.config.option.spring.jpa.properties.hibernate.format_sql.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    HIBERNATE_QUERY_CACHE(
        "spring.jpa.properties.hibernate.cache.use_query_cache",
        n("sumaris.config.option.spring.jpa.properties.hibernate.cache.use_query_cache.description"),
        Boolean.TRUE.toString(),
        boolean.class,
        false),

    HIBERNATE_SECOND_LEVEL_CACHE(
        "spring.jpa.properties.hibernate.cache.use_second_level_cache",
        n("sumaris.config.option.spring.jpa.properties.hibernate.cache.use_second_level_cache.description"),
        Boolean.TRUE.toString(),
        boolean.class,
        false),

    HIBERNATE_CLIENT_QUERIES_FILE(
        "sumaris.persistence.hibernate.queriesFile",
        n("sumaris.config.option.persistence.hibernate.queriesFile.description"),
        "queries-failsafe.hbm.xml",
        String.class,
        false),

    /* -- Cache options-- */

    CACHE_ENABLED(
        "spring.cache.enabled",
        n("sumaris.config.option.spring.cache.enabled.description"),
        Boolean.TRUE.toString(),
        boolean.class,
        false),

    /* -- Liquibase options-- */

    LIQUIBASE_ENABLED(
        "spring.liquibase.enabled",
        n("sumaris.config.option.spring.liquibase.enabled.description"),
        Boolean.TRUE.toString(),
        boolean.class,
        false),

    LIQUIBASE_RUN_COMPACT(
        "spring.liquibase.compact.enabled",
        n("sumaris.config.option.liquibase.should.compact.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    LIQUIBASE_CHANGE_LOG_PATH(
        "spring.liquibase.change-log",
        n("sumaris.config.option.liquibase.changelog.path.description"),
        "classpath:net/sumaris/core/db/changelog/db-changelog-master.xml",
        String.class,
        false),

    LIQUIBASE_DIFF_TYPES(
        "sumaris.persistence.liquibase.diff.types",
        n("sumaris.config.option.liquibase.diff.types.description"),
        null,
        String.class,
        false),

    ENABLE_CONFIGURATION_DB_PERSISTENCE(
        "sumaris.persistence.configuration.enabled",
        n("sumaris.config.option.persistence.configuration.enabled.description"),
        Boolean.TRUE.toString(),
        boolean.class,
        false),

    /* -- Geometry (JTS) -- */

    GEOMETRY_SRID(
        "sumaris.geometry.srid",
        n("sumaris.config.option.sumaris.geometry.srid.description"),
        Geometries.SRID.NONE.toString(),
        Class.class,
        false),

    /* -- Active MQ options-- */

    ACTIVEMQ_POOL_ENABLED(
        "spring.activemq.pool.enabled",
        n("sumaris.config.option.spring.activemq.pool.enabled.description"),
        "false",
        Boolean.class,
        false),

    ACTIVEMQ_BROKER_URL(
        "spring.activemq.broker-url",
        n("sumaris.config.option.spring.activemq.broker-url.description"),
        "vm://embedded?broker.persistent=true",
        String.class,
        false),

    ACTIVEMQ_USERNAME(
        "spring.activemq.user",
        n("sumaris.config.option.spring.activemq.user.description"),
        "",
        String.class),

    ACTIVEMQ_PASSWORD(
        "spring.activemq.password",
        n("sumaris.config.option.spring.activemq.password.description"),
        "",
        String.class),

    /* -- Functional features options-- */

    ENABLE_ANALYTIC_REFERENCES(
        "sumaris.analyticReferences.enable",
        n("sumaris.config.option.analyticReferences.enable.description"),
        Boolean.FALSE.toString(),
        boolean.class,
        false),

    ANALYTIC_REFERENCES_SERVICE_URL(
        "sumaris.analyticReferences.service.url",
        n("sumaris.config.option.analyticReferences.service.url.description"),
        "http://vsap7-dgw.ifremer.fr:8001/sap/opu/odata/sap/ZUI5_WS_EOTP_SRV/EotpSet/?$format=json",
        String.class,
        false),

    ANALYTIC_REFERENCES_SERVICE_AUTH(
        "sumaris.analyticReferences.service.auth",
        n("sumaris.config.option.analyticReferences.service.auth.description"),
        "ws_eotp:eotp-sap-20201022",
        String.class,
        false),

    ANALYTIC_REFERENCES_SERVICE_DELAY(
        "sumaris.analyticReferences.service.delay",
        n("sumaris.config.option.analyticReferences.service.delay.description"),
        String.valueOf(30), /* 30 days */
        Integer.class,
        false),

    ANALYTIC_REFERENCES_SERVICE_FILTER(
        "sumaris.analyticReferences.service.filter",
        n("sumaris.config.option.analyticReferences.service.filter.description"),
        ".*-MS", /* regexp */
        String.class,
        false),


    JOB_ENABLED(
        "sumaris.job.service.enabled",
        n("sumaris.config.option.job.service.enabled.description"),
        Boolean.TRUE.toString(),
        Boolean.class,
        false),

    /*
     * CLI options
     */
    CLI_DAEMONIZE(
        "sumaris.cli.daemon",
        n("sumaris.config.option.cli.daemon.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    CLI_OUTPUT_FILE(
        "sumaris.cli.output.file",
        n("sumaris.config.option.cli.output.file.description"),
        null,
        File.class,
        false),

    CLI_FORCE_OUTPUT(
        "sumaris.cli.output.force",
        n("sumaris.config.option.cli.output.force.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    CLI_FILTER_YEAR(
        "sumaris.cli.filter.year",
        n("sumaris.config.option.cli.filter.year.description"),
        "-1",
        Integer.class,
        false),

    CLI_FILTER_PROGRAM_LABEL(
        "sumaris.cli.filter.programLabel",
        n("sumaris.config.option.cli.filter.programLabel.description"),
        "",
        String.class,
        false),
    CLI_FILTER_TRIP_IDS(
        "sumaris.cli.filter.tripIds",
        n("sumaris.config.option.cli.filter.tripIds.description"),
        "",
        Integer.class,
        false),
    CLI_FILTER_OPERATION_IDS(
        "sumaris.cli.filter.operationIds",
        n("sumaris.config.option.cli.filter.operationIds.description"),
        "",
        Integer.class,
        false),
    CSV_SEPARATOR(
        "sumaris.csv.separator",
        n("sumaris.config.option.csv.separator.description"),
        ";",
        String.class,
        false),
    VALUE_SEPARATOR(
        "sumaris.value.separator",
        n("sumaris.config.option.value.separator.description"),
        ",",
        String.class,
        false),
    ATTRIBUTE_SEPARATOR(
        "sumaris.attribute.separator",
        n("sumaris.config.option.attribute.separator.description"),
        ".",
        String.class,
        false),

    /*
     * Application options
     */

    I18N_LOCALE(
        "sumaris.i18n.locale",
        n("sumaris.config.option.i18n.locale.description"),
        Locale.FRANCE.getCountry(),
        Locale.class,
        false),

    LAUNCH_MODE(
        "sumaris.launch.mode",
        n("sumaris.config.option.launch.mode.description"),
        LaunchModeEnum.development.name(),
        String.class),

    DEFAULT_QUALITY_FLAG(
        "sumaris.persistence.qualityFlagId.default",
        n("sumaris.config.option.persistence.qualityFlagId.default.description"),
        String.valueOf(0),
        Integer.class),

    SEQUENCE_START_WITH(
        "sumaris.persistence.sequence.startWith",
        n("sumaris.config.option.persistence.sequence.startWith.description"),
        String.valueOf(1),
        Integer.class),

    SEQUENCE_INCREMENT(
        "sumaris.persistence.sequence.increment",
        n("sumaris.config.option.persistence.sequence.increment.description"),
        null, // null as default to let Hibernate use allocationSize in model (@see javax.persistence.SequenceGenerator.allocationSize)
        Integer.class),

    SEQUENCE_SUFFIX(
        "sumaris.persistence.sequence.suffix",
        n("sumaris.config.option.persistence.sequence.suffix.description"),
        "_SEQ",
        String.class),

    LOCK_TIMEOUT(
        "javax.persistence.lock.timeout",
        n("sumaris.config.option.javax.persistence.lock.timeout.description"),
        "0",
        Integer.class),

    LOCK_MODE_TYPE(
        "javax.persistence.lock.mode",
        n("sumaris.config.option.javax.persistence.lock.mode.description"),
        LockModeType.PESSIMISTIC_WRITE.name(),
        String.class),

    ENABLE_TECHNICAL_TABLES_UPDATE(
        "sumaris.persistence.technicalTables.update",
        n("sumaris.config.option.persistence.technicalTables.update.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    ENABLE_BATCH_HASH_OPTIMIZATION(
        "sumaris.persistence.batch.hashOptimization",
        n("sumaris.config.option.persistence.batch.hashOptimization.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    ENABLE_SAMPLE_HASH_OPTIMIZATION(
        "sumaris.persistence.sample.hashOptimization",
        n("sumaris.config.option.persistence.sample.hashOptimization.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    ENABLE_SAMPLE_UNIQUE_TAG(
        "sumaris.persistence.sample.uniqueTag",
        n("sumaris.config.option.persistence.sample.uniqueTag.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    ENABLE_PHYSICAL_GEAR_HASH_OPTIMIZATION(
        "sumaris.persistence.physicalGear.hashOptimization",
        n("sumaris.config.option.persistence.physicalGear.hashOptimization.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    ENABLE_ACTIVITY_CALENDAR_HASH_OPTIMIZATION(
        "sumaris.persistence.activityCalendar.hashOptimization",
        n("sumaris.config.option.persistence.activityCalendar.hashOptimization.description"),
        Boolean.TRUE.toString(), // TODO change to false
        Boolean.class,
        false),

    ENABLE_VESSEL_USE_FEATURES_HASH_OPTIMIZATION(
        "sumaris.persistence.vesselUseFeatures.hashOptimization",
        n("sumaris.config.option.persistence.vesselUseFeatures.hashOptimization.description"),
        "${sumaris.persistence.activityCalendar.hashOptimization}",
        Boolean.class,
        false),
    ENABLE_GEAR_USE_FEATURES_HASH_OPTIMIZATION(
        "sumaris.persistence.gearUseFeatures.hashOptimization",
        n("sumaris.config.option.persistence.gearUseFeatures.hashOptimization.description"),
        "${sumaris.persistence.activityCalendar.hashOptimization}",
        Boolean.class,
        false),

    ENABLE_GEAR_PHYSICAL_FEATURES_HASH_OPTIMIZATION(
        "sumaris.persistence.gearPhysicalFeatures.hashOptimization",
        n("sumaris.config.option.persistence.gearPhysicalFeatures.hashOptimization.description"),
        "${sumaris.persistence.activityCalendar.hashOptimization}",
        Boolean.class,
        false),

    VESSEL_DEFAULT_PROGRAM_LABEL(
        "sumaris.persistence.vessel.defaultProgram.label",
        n("sumaris.config.option.persistence.vessel.defaultProgram.label.description"),
        "SIH",
        String.class,
        false),

    VESSEL_REGISTRATION_CODE_NATURAL_ORDER(
        "sumaris.persistence.vessel.registrationCode.naturalOrder.enable",
        n("sumaris.config.option.persistence.vessel.registrationCode.naturalOrder.enable.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    VESSEL_REGISTRATION_CODE_SEARCH_AS_PREFIX(
        "sumaris.persistence.vessel.registrationCode.searchAsPrefix",
        n("sumaris.config.option.persistence.vessel.registrationCode.searchAsPrefix.description"),
        Boolean.TRUE.toString(),
        Boolean.class,
        false),

    DB_ADAGIO_SCHEMA(
        "sumaris.persistence.adagio.schema",
        n("sumaris.config.option.persistence.adagio.schema.description"),
        "SIH2_ADAGIO_DBA",
        String.class,
        false),

    ENABLE_ADAGIO_OPTIMIZATION(
        "sumaris.persistence.adagio.optimization",
        n("sumaris.config.option.persistence.adagio.optimization.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false),

    AUTH_TOKEN_ENABLED(
        "sumaris.auth.token.enabled",
        n("sumaris.config.option.sumaris.account.token.enable"),
        "false",
        Boolean.class,
        false),

    ELASTICSEARCH_URIS(
        "spring.elasticsearch.uris",
        n("sumaris.config.option.spring.elasticsearch.uris.description"),
        "http://localhost:9200",
        String.class,
        false
    ),

    ELASTICSEARCH_USERNAME(
        "spring.elasticsearch.username",
        n("sumaris.config.option.spring.elasticsearch.username.description"),
        null,
        String.class,
        false
    ),

    ELASTICSEARCH_PASSWORD(
        "spring.elasticsearch.password",
        n("sumaris.config.option.spring.elasticsearch.password.description"),
        null,
        String.class,
        false
    ),

    ELASTICSEARCH_ENABLED(
        "spring.elasticsearch.enabled",
        n("sumaris.config.option.spring.elasticsearch.enabled.description"),
        Boolean.FALSE.toString(),
        Boolean.class,
        false
    ),

    ELASTICSEARCH_INDEX_PREFIX(
        "spring.elasticsearch.index.prefix",
        n("sumaris.config.option.spring.elasticsearch.index.prefix.description"),
        "",
        String.class,
        false
    ),

    ELASTICSEARCH_VESSEL_SNAPSHOT_ENABLED(
        "sumaris.elasticsearch.vessel.snapshot.enabled",
        n("sumaris.config.option.elasticsearch.vessel.snapshot.enabled.description"),
        "${spring.elasticsearch.enabled}",
        Boolean.class,
        false),
    ;

    /**
     * Configuration key.
     */
    private final String key;

    /**
     * I18n key of option description
     */
    private final String description;

    /**
     * Type of option
     */
    private final Class<?> type;

    /**
     * Default value of option.
     */
    private String defaultValue;

    /**
     * Flag to not keep option value on disk
     */
    private final boolean isTransient;

    /**
     * Flag to not allow option value modification
     */
    private final boolean isFinal;

    SumarisConfigurationOption(String key,
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

    SumarisConfigurationOption(String key,
                               String description,
                               String defaultValue,
                               Class<?> type) {
        this(key, description, defaultValue, type, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransient() {
        return isTransient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransient(boolean newValue) {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFinal(boolean newValue) {
        // not used
    }
}
