package net.sumaris.extraction.core.dao.data.activityCalendar;

/*-
 * #%L
 * SUMARiS:: Core Extraction
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
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.data.ActivityCalendarDirectSurveyInvestigationEnum;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.Programs;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.specification.data.activityCalendar.ActivityMonitoringSpecification;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterCriterionVO;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.data.activityCalendar.ExtractionActivityCalendarFilterVO;
import net.sumaris.extraction.core.vo.data.activityCalendar.ExtractionActivityMonitoringContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.text.ParseException;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.Stream;

@Repository("extractionActivityMonitoringDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@RequiredArgsConstructor
@Slf4j
public class ExtractionActivityMonitoringDaoImpl<C extends ExtractionActivityMonitoringContextVO, F extends ExtractionFilterVO>
    extends ExtractionBaseDaoImpl<C, F>
    implements ExtractionActivityMonitoringDao<C, F> {

    private static final String AC_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ActivityMonitoringSpecification.AC_SHEET_NAME + "_%s";
    private static final String AM_RAW_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ActivityMonitoringSpecification.AM_RAW_SHEET_NAME + "_%s";
    private static final String AM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ActivityMonitoringSpecification.AM_SHEET_NAME + "_%s";

    private final LocationRepository locationRepository;
    private final VesselSnapshotRepository vesselSnapshotRepository;
    private final PersonRepository personRepository;
    private final ProgramRepository programRepository;

    private boolean enableAdagioOptimization = false;
    private String adagioSchema;

    @PostConstruct()
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        // Read some config options
        String adagioSchema = this.configuration.getAdagioSchema();
        boolean enableAdagioOptimization = StringUtils.isNotBlank(adagioSchema)
                                           && this.configuration.enableAdagioOptimization()
                                           && this.databaseType == DatabaseType.oracle;

        // Check if there is some changes
        boolean hasChanges = !Objects.equals(this.adagioSchema, adagioSchema)
                             || this.enableAdagioOptimization != enableAdagioOptimization;

        // Apply changes if need
        if (hasChanges) {
            this.adagioSchema = adagioSchema;
            this.enableAdagioOptimization = enableAdagioOptimization;

            if (this.enableAdagioOptimization) {
                log.info("Enabled extraction {}, using optimization for schema '{}'", VesselSpecification.FORMAT, this.adagioSchema);
            } else {
                log.info("Enabled extraction {} (without schema optimization)", VesselSpecification.FORMAT);
            }

        }
    }

    @Override
    public void init() {
        super.init();

        // -- for DEV only
        // set RAW_SL as a visible sheet
        if (!this.enableCleanup && !this.production) {
            LiveExtractionTypeEnum.ACTIVITY_MONITORING.setSheetNames(ActivityMonitoringSpecification.SHEET_NAMES_DEBUG);
        }
    }

    @Override
    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.ACTIVITY_MONITORING);
    }

    @SneakyThrows
    @Override
    public <R extends C> R execute(F filter) {
        ExtractionActivityCalendarFilterVO activityCalendarFilter = toActivityCalendarFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setActivityCalendarFilter(activityCalendarFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.ACTIVITY_MONITORING);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        Long startTime = null;


        if (log.isInfoEnabled()) {
            startTime = System.currentTimeMillis();

            StringBuilder filterInfo = new StringBuilder();
            String filterStr = context.getActivityCalendarFilter() != null ? context.getActivityCalendarFilter().toString() : "";

            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter: ").append(filterStr);
            } else {
                filterInfo.append("without filter");
            }
            log.info("Starting extraction {}... {}", context.getFormat(), filterInfo);
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --
        try {
            // Calendar table
            long t = System.currentTimeMillis();
            long rowCount = createCalendarTable(context);
            if (log.isDebugEnabled()) log.debug("{} created in {}", context.getCalendarTableName(), TimeUtils.printDurationFrom(t));
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Raw monitoring table
            if (rowCount > 0) {
                t = System.currentTimeMillis();
                rowCount = createRawMonitoringTable(context);
                if (log.isDebugEnabled()) log.debug("{} created in {}", context.getRawMonitoringTableName(), TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            // Monitoring table
            if (rowCount > 0) {
                t = System.currentTimeMillis();
                rowCount = createMonitoringTable(context);
                if (log.isDebugEnabled()) log.debug("{} created in {}", context.getMonitoringTableName(), TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            return context;
        } catch (PersistenceException | ParseException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            startTime = null; // Avoid log

            throw e;
        } finally {
            if (startTime != null) {
                log.info("Extraction #{} finished in {}", context.getId(), TimeUtils.printDurationFrom(startTime));
            }
        }
    }
    /* -- protected methods -- */

    protected ExtractionActivityCalendarFilterVO toActivityCalendarFilterVO(@Nullable ExtractionFilterVO source) {
        ExtractionActivityCalendarFilterVO
            target = new ExtractionActivityCalendarFilterVO();
        if (source == null) {
            return target;
        }

        // get program from filter
        ProgramVO program = Beans.getStream(source.getCriteria())
                .filter(criterion -> ActivityMonitoringSpecification.COLUMN_PROJECT.equalsIgnoreCase(criterion.getName()))
                .map(ExtractionFilterCriterionVO::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst().flatMap(programRepository::findByLabel)
                .orElseThrow(() -> new IllegalArgumentException("Missing project criterion"));
        Integer[] basePortLocationLevelIds = Programs.getPropertyAsIntegers(program, ProgramPropertyEnum.ACTIVITY_CALENDAR_BASE_PORT_LOCATION_LEVEL_IDS);
        Integer[] registrationLocationLevelIds = Programs.getPropertyAsIntegers(program, ProgramPropertyEnum.ACTIVITY_CALENDAR_REGISTRATION_LOCATION_LEVEL_IDS);

        Beans.copyProperties(source, target);

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());

            // One value
            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
                    case ActivityMonitoringSpecification.COLUMN_YEAR:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setYear(Integer.valueOf(criterion.getValue()));
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_PROJECT:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getValue());
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_INCLUDED_IDS:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            try {
                                Integer id = Integer.parseInt(criterion.getValue());
                                target.setIncludedIds(ArrayUtils.toArray(id));
                            } catch (NumberFormatException e) {
                                // Skip
                            }
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_BASE_PORT_LOCATION_LABEL:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            Integer[] basePortLocationIds = Beans.collectIds(
                                locationRepository.findAll(LocationFilterVO.builder()
                                    .levelIds(basePortLocationLevelIds)
                                    .label(criterion.getValue())
                                    .build())
                            ).toArray(Integer[]::new);
                            target.setBasePortLocationIds(basePortLocationIds);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_BASE_PORT_LOCATION_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setBasePortLocationIds(ArrayUtils.toArray(Integer.valueOf(criterion.getValue())));
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_REGISTRATION_LOCATION_LABEL:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            Integer[] registrationLocationIds = Beans.collectIds(
                                locationRepository.findAll(LocationFilterVO.builder()
                                    .levelIds(registrationLocationLevelIds)
                                    .label(criterion.getValue())
                                    .build())
                            ).toArray(Integer[]::new);
                            target.setRegistrationLocationIds(registrationLocationIds);
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_REGISTRATION_LOCATION_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRegistrationLocationIds(ArrayUtils.toArray(Integer.valueOf(criterion.getValue())));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_VESSEL_CODE:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            vesselSnapshotRepository.findAll(
                                    VesselFilterVO.builder()
                                        .searchText(criterion.getValue())
                                        .build(),
                                    VesselFetchOptions.builder()
                                        .withVesselFeatures(false)
                                        .withVesselRegistrationPeriod(false)
                                        .build()
                                ).stream()
                                .findFirst()
                                .map(VesselSnapshotVO::getVesselId)
                                .ifPresent(vesselId -> {
                                    target.setVesselIds(ArrayUtils.toArray(vesselId));
                                    // Clean the criterion (to avoid clean to exclude too many data)
                                    criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                                    criterion.setValue(null);
                                });
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_VESSEL_TYPE_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            try {
                                Integer vesselTypeId = Integer.parseInt(criterion.getValue());
                                target.setVesselTypeIds(ArrayUtils.toArray(vesselTypeId));
                            } catch (NumberFormatException e) {
                                // Skip
                            }
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_OBSERVER_NAME:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setObserverPersonIds(
                                Stream.of(
                                        personRepository.findByFullName(criterion.getValue())
                                            .map(PersonVO::getId)
                                            .orElse(null)
                                    )
                                    .filter(Objects::nonNull)
                                    .toArray(Integer[]::new)
                            );
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_OBSERVER_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setObserverPersonIds(ArrayUtils.toArray(Integer.valueOf(criterion.getValue())));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_RECORDER_NAME:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRecorderPersonIds(
                                Stream.of(
                                        personRepository.findByFullName(criterion.getValue())
                                            .map(PersonVO::getId)
                                            .orElse(null)
                                    )
                                    .filter(Objects::nonNull)
                                    .toArray(Integer[]::new)
                            );
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_RECORDER_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRecorderPersonIds(ArrayUtils.toArray(Integer.valueOf(criterion.getValue())));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_DIRECT_SURVEY_INVESTIGATION:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setDirectSurveyInvestigation(
                                    ActivityCalendarDirectSurveyInvestigationEnum.findByLabel(criterion.getValue())
                                            .map(ActivityCalendarDirectSurveyInvestigationEnum::getId)
                                            .orElse(null));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                }
            }

            // many values with the operator IN
            else if (operator == ExtractionFilterOperatorEnum.IN && ArrayUtils.isNotEmpty(criterion.getValues())) {
                switch (criterion.getName().toLowerCase()) {
                    case ActivityMonitoringSpecification.COLUMN_INCLUDED_IDS:
                        target.setIncludedIds(Arrays.stream(criterion.getValues()).mapToInt(Integer::parseInt).boxed().toArray(Integer[]::new));
                        break;
                    case ActivityMonitoringSpecification.COLUMN_BASE_PORT_LOCATION_LABEL:
                        target.setBasePortLocationIds(
                            Arrays.stream(criterion.getValues()).map(
                                    label -> locationRepository.findAll(
                                            LocationFilterVO.builder().levelIds(basePortLocationLevelIds).label(label).build()
                                    ).stream().findFirst().map(ReferentialVO::getId).orElse(null)
                                )
                                .toArray(Integer[]::new)
                        );
                        break;
                    case ActivityMonitoringSpecification.COLUMN_BASE_PORT_LOCATION_ID:
                        target.setBasePortLocationIds(Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new));
                        break;
                    case ActivityMonitoringSpecification.COLUMN_REGISTRATION_LOCATION_LABEL:
                        target.setRegistrationLocationIds(
                            Arrays.stream(criterion.getValues()).map(
                                            label -> locationRepository.findAll(
                                                    LocationFilterVO.builder().levelIds(registrationLocationLevelIds).label(label).build()
                                            ).stream().findFirst().map(ReferentialVO::getId).orElse(null)
                                )
                                .toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_REGISTRATION_LOCATION_ID:
                        target.setRegistrationLocationIds(
                            Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_VESSEL_CODE:
                        target.setVesselIds(
                            Arrays.stream(criterion.getValues()).flatMap(
                                    code -> vesselSnapshotRepository.findAll(
                                            VesselFilterVO.builder()
                                                .searchText(code)
                                                .build(),
                                            VesselFetchOptions.builder()
                                                .withVesselFeatures(false)
                                                .withVesselRegistrationPeriod(false)
                                                .build()
                                        )
                                        .stream()
                                )
                                .map(VesselSnapshotVO::getVesselId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_VESSEL_TYPE_ID:
                            target.setVesselTypeIds(
                                Arrays.stream(criterion.getValues()).map(value -> {
                                    try {
                                        return Integer.parseInt(value);
                                    } catch (NumberFormatException e) {
                                        // Skip
                                        return null;
                                    }
                                }).filter(Objects::nonNull).toArray(Integer[]::new));
                        break;
                    case ActivityMonitoringSpecification.COLUMN_OBSERVER_NAME:
                        target.setObserverPersonIds(
                            Arrays.stream(criterion.getValues()).flatMap(
                                    s -> personRepository.findByFullName(s).stream()
                                )
                                .map(PersonVO::getId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_OBSERVER_ID:
                        target.setObserverPersonIds(
                            Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_RECORDER_NAME:
                        target.setRecorderPersonIds(
                            Arrays.stream(criterion.getValues()).flatMap(
                                    s -> personRepository.findByFullName(s).stream()
                                )
                                .map(PersonVO::getId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_RECORDER_ID:
                        target.setRecorderPersonIds(
                            Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                }
            }
        });

        // If year is not set, set by default current year -1
        if (target.getYear() == null) {
            target.setYear(Year.now().getValue() - 1);
        }

        // Clean criteria, to avoid reapply on cleanRow
        if (CollectionUtils.size(source.getCriteria()) == 1) {
            source.getCriteria().clear();
        }

        return target;
    }

    protected Class<? extends ExtractionActivityMonitoringContextVO> getContextClass() {
        return ExtractionActivityMonitoringContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionActivityMonitoringContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setCalendarTableName(formatTableName(AC_TABLE_NAME_PATTERN, context.getId()));
        context.setRawMonitoringTableName(formatTableName(AM_RAW_TABLE_NAME_PATTERN, context.getId()));
        context.setMonitoringTableName(formatTableName(AM_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet name
        context.setCalendarSheetName(ActivityMonitoringSpecification.AC_SHEET_NAME);
        context.setRawMonitoringSheetName(ActivityMonitoringSpecification.AM_RAW_SHEET_NAME);
        context.setMonitoringSheetName(ActivityMonitoringSpecification.AM_SHEET_NAME);
    }

    protected long createCalendarTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getCalendarTableName();

        // Create Calendar Table
        XMLQuery xmlQuery = createCalendarQuery(context);
        execute(context, xmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getCalendarSheetName());
        }

        context.addTableName(tableName,
            context.getCalendarSheetName(),
            xmlQuery.getHiddenColumnNames(),
            xmlQuery.hasDistinctOption());

        return count;
    }

    protected long createRawMonitoringTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getRawMonitoringTableName();

        // Create Raw Data Table
        XMLQuery xmlQuery = createRawMonitoringQuery(context);
        execute(context, xmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getRawMonitoringSheetName());
        }

        // Add as a raw table (to be able to clean it later)
        if (count == 0 || this.production || this.enableCleanup) {
            context.addRawTableName(tableName);
        }
        // Keep raw table (for DEBUG only)
        else {
            context.addTableName(tableName,
                context.getRawMonitoringSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());
        }

        return count;
    }

    protected long createMonitoringTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getMonitoringTableName();

        // Create Result Table with an injection
        XMLQuery xmlQuery = createMonitoringQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getMonitoringSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                context.getMonitoringSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());
            log.debug("Monitoring table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected XMLQuery createCalendarQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createCalendarTable");
        xmlQuery.bind("calendarTableName", context.getCalendarTableName());

        // Pmfms
        xmlQuery.bind("surveyQualificationPmfmId", PmfmEnum.SURVEY_QUALIFICATION.getId());

        // Quality flag
        xmlQuery.bind("qualityFlagNotQualified", QualityFlagEnum.NOT_QUALIFIED.getId());

        // Date Filter
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program filter
        {
            List<String> programLabels = context.getProgramLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(programLabels);
            xmlQuery.setGroup("programFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));
        }

        // Year filter
        if (year != null) {
            xmlQuery.setGroup("yearFilter", true);
            xmlQuery.bind("year", year);
        } else {
            xmlQuery.setGroup("filterYear", false);
        }

        // Ids
        {
            List<Integer> includedIds = context.getIncludedIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(includedIds);
            xmlQuery.setGroup("includedIds", enableFilter);
            xmlQuery.setGroup("!includedIds", !enableFilter);
            if (enableFilter) xmlQuery.bind("includedIds", Daos.getSqlInNumbers(includedIds));
        }

        // Registration location
        {
            List<Integer> registrationLocationIds = context.getRegistrationLocationIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(registrationLocationIds);
            xmlQuery.setGroup("registrationLocationFilter", enableFilter);
            xmlQuery.setGroup("!registrationLocationFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("registrationLocationIds", Daos.getSqlInNumbers(registrationLocationIds));
        }

        // Base port location
        {
            List<Integer> basePortLocationIds = context.getBasePortLocationIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(basePortLocationIds);
            xmlQuery.setGroup("basePortLocationFilter", enableFilter);
            xmlQuery.setGroup("!basePortLocationFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("basePortLocationIds", Daos.getSqlInNumbers(basePortLocationIds));
        }

        // Vessel code
        {
            List<Integer> vesselIds = context.getVesselIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(vesselIds);
            xmlQuery.setGroup("vesselFilter", enableFilter);
            xmlQuery.setGroup("!vesselFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(vesselIds));
        }

        // Vessel type
        {
            List<Integer> vesselTypeIds = context.getVesselTypeIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(vesselTypeIds);
            xmlQuery.setGroup("vesselTypeFilter", enableFilter);
            xmlQuery.setGroup("!vesselTypeFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("vesselTypeIds", Daos.getSqlInNumbers(vesselTypeIds));
        }

        // Observers
        {
            List<Integer> observerPersonIds = context.getObserverPersonIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(observerPersonIds);
            xmlQuery.setGroup("observersFilter", enableFilter);
            xmlQuery.setGroup("!observersFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("observerPersonIds", Daos.getSqlInNumbers(observerPersonIds));
        }

        // Recorders
        {
            List<Integer> recorderPersonIds = context.getRecorderPersonIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(recorderPersonIds);
            xmlQuery.setGroup("recordersFilter", enableFilter);
            xmlQuery.setGroup("!recordersFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("recorderPersonIds", Daos.getSqlInNumbers(recorderPersonIds));
        }

        // DirectSurveyInvestigation
        {
            boolean enableFilter = context.getDirectSurveyInvestigation() != null;
            xmlQuery.setGroup("directSurveyInvestigationFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("directSurveyInvestigation", context.getDirectSurveyInvestigation());
        }

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createRawMonitoringQuery(C context) throws PersistenceException {

        XMLQuery xmlQuery = createXMLQuery(context, "createRawMonitoringTable");
        xmlQuery.bind("rawMonitoringTableName", context.getRawMonitoringTableName());
        xmlQuery.bind("calendarTableName", context.getCalendarTableName());

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createMonitoringQuery(C context) throws PersistenceException {

        XMLQuery xmlQuery = createXMLQuery(context, "createMonitoringTable");
        xmlQuery.bind("rawMonitoringTableName", context.getRawMonitoringTableName());
        xmlQuery.bind("monitoringTableName", context.getMonitoringTableName());

        // Date Filter
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        URL injectionQuery = getXMLQueryURL(context, "injectionMonitoringMonthTable");

        // Create a column for each month
        for (Month month : Month.values()) {
            injectMonitoringMonthQuery(context, xmlQuery, injectionQuery, month.getValue());
        }

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected void injectMonitoringMonthQuery(C context,
                                              XMLQuery xmlQuery,
                                              URL injectionQuery, int month) throws PersistenceException {

        String tableAlias = (ActivityMonitoringSpecification.COLUMN_MONTH_PREFIX + month).toUpperCase();
        xmlQuery.injectQuery(injectionQuery, "%suffix%", tableAlias);
        xmlQuery.bind("month" + tableAlias, month);
    }
}

