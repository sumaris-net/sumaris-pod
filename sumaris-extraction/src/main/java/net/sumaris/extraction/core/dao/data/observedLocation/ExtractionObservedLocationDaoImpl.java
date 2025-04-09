package net.sumaris.extraction.core.dao.data.observedLocation;

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
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.specification.data.observedLocation.ObservedLocationSpecification;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.data.observedLocation.ExtractionObservedLocationContextVO;
import net.sumaris.extraction.core.vo.data.observedLocation.ExtractionObservedLocationFilterVO;
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
import java.text.ParseException;
import java.time.Year;
import java.util.*;
import java.util.stream.Stream;

@Repository("extractionObservedLocationDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@RequiredArgsConstructor
@Slf4j
public class ExtractionObservedLocationDaoImpl<C extends ExtractionObservedLocationContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl<C, F>
        implements ExtractionObservedLocationDao<C, F> {

    private static final String OL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.OL_SHEET_NAME + "_%s";
    private static final String VESSEL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.VESSEL_SHEET_NAME + "_%s";
    private static final String CATCH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.CATCH_SHEET_NAME + "_%s";
    private static final String CATCH_LOT_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.CATCH_LOT_SHEET_NAME + "_%s";
    private static final String CATCH_INDIVIDUAL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.CATCH_INDIVIDUAL_SHEET_NAME + "_%s";
    private static final String TRIP_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.TRIP_SHEET_NAME + "_%s";
    private static final String TRIP_CALENDAR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.TRIP_CALENDAR_SHEET_NAME + "_%s";
    private static final String OBSERVER_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ObservedLocationSpecification.OBSERVER_SHEET_NAME + "_%s";

    private final LocationRepository locationRepository;
    private final VesselSnapshotRepository vesselSnapshotRepository;
    private final PersonRepository personRepository;

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
            LiveExtractionTypeEnum.OBSERVED_LOCATION.setSheetNames(ObservedLocationSpecification.SHEET_NAMES_DEBUG);
        }
    }

    @Override
    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.OBSERVED_LOCATION);
    }

    @SneakyThrows
    @Override
    public <R extends C> R execute(F filter) {
        ExtractionObservedLocationFilterVO observedLocationFilter = toObservedLocationFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setObservedLocationFilter(observedLocationFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.OBSERVED_LOCATION);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        Long startTime = null;


        if (log.isInfoEnabled()) {
            startTime = System.currentTimeMillis();

            StringBuilder filterInfo = new StringBuilder();
            String filterStr = context.getObservedLocationFilter() != null ? context.getObservedLocationFilter().toString() : "";

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
            long rowCount = 0L;
            long t = 0L;
            // ObservedLocation table
            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.OL_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createObservedLocationTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} rows in {}", context.getObservedLocationTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.VESSEL_SHEET_NAME) && rowCount > 0) {
                // Vessel table
                t = System.currentTimeMillis();
                rowCount = createVesselTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getVesselTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.CATCH_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createCatchTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getCatchTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.CATCH_LOT_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createCatchLotTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getCatchLotTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.CATCH_INDIVIDUAL_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createCatchIndividualTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getCatchIndividualTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.TRIP_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createTripTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getTripTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.TRIP_CALENDAR_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createTripCalendarTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getTripCalendarTableName(), rowCount, TimeUtils.printDurationFrom(t));
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }


            if (filter != null && filter.getSheetNames().contains(ObservedLocationSpecification.OBSERVER_SHEET_NAME)) {
                t = System.currentTimeMillis();
                rowCount = createObserverTable(context);
                if (log.isDebugEnabled())
                    log.debug("{} created with {} in {}", context.getObserverTableName(), rowCount, TimeUtils.printDurationFrom(t));
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

    protected ExtractionObservedLocationFilterVO toObservedLocationFilterVO(@Nullable ExtractionFilterVO source) {
        ExtractionObservedLocationFilterVO
                target = new ExtractionObservedLocationFilterVO();
        if (source == null) {
            return target;
        }

        Beans.copyProperties(source, target);

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());

            // One value
            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
                    case ObservedLocationSpecification.COLUMN_YEAR:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setYear(Integer.valueOf(criterion.getValue()));
                        }
                        break;
                    case ObservedLocationSpecification.COLUMN_PROJECT:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getValue());
                        }
                        break;
                    case ObservedLocationSpecification.COLUMN_LOCATION_LABEL:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setLocationIds(
                                    Stream.of(
                                                    locationRepository.findByLabel(criterion.getValue())
                                                            .map(ReferentialVO::getId)
                                                            .orElse(null)
                                            )
                                            .filter(Objects::nonNull)
                                            .toArray(Integer[]::new)
                            );
                        }
                        break;
                    case ObservedLocationSpecification.COLUMN_LOCATION_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setLocationIds(ArrayUtils.toArray(Integer.valueOf(criterion.getValue())));
                        }
                        break;
                    case ObservedLocationSpecification.COLUMN_VESSEL_CODE:
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
                    case ObservedLocationSpecification.COLUMN_OBSERVER_NAME:
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
                    case ObservedLocationSpecification.COLUMN_OBSERVER_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setObserverPersonIds(ArrayUtils.toArray(Integer.valueOf(criterion.getValue())));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ObservedLocationSpecification.COLUMN_RECORDER_NAME:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRecorderPersonId(
                                    personRepository.findByFullName(criterion.getValue())
                                            .map(PersonVO::getId)
                                            .orElse(null)
                            );
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ObservedLocationSpecification.COLUMN_RECORDER_ID:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRecorderPersonId(Integer.valueOf(criterion.getValue()));
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
                    case ObservedLocationSpecification.COLUMN_INCLUDED_IDS:
                        target.setIncludedIds(Arrays.stream(criterion.getValues()).mapToInt(Integer::parseInt).boxed().toArray(Integer[]::new));
                        break;
                    case ObservedLocationSpecification.COLUMN_LOCATION_LABEL:
                        target.setLocationIds(
                                Arrays.stream(criterion.getValues()).map(
                                                label -> locationRepository.findByLabel(label).map(ReferentialVO::getId).orElse(null)
                                        )
                                        .filter(Objects::nonNull)
                                        .toArray(Integer[]::new)
                        );
                        break;
                    case ObservedLocationSpecification.COLUMN_LOCATION_ID:
                        target.setLocationIds(Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new));
                        break;
                    case ObservedLocationSpecification.COLUMN_VESSEL_CODE:
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
                    case ObservedLocationSpecification.COLUMN_OBSERVER_NAME:
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
                    case ObservedLocationSpecification.COLUMN_OBSERVER_ID:
                        target.setObserverPersonIds(
                                Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new)
                        );
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
//                    case ObservedLocationSpecification.COLUMN_RECORDER_NAME:
//                        target.setRecorderPersonId(
//                            Optional.ofNullable(
//                                    s -> personRepository.findByFullName(criterion.getValues()).stream()
//                                )
//                                .map(PersonVO::getId)
//                                .filter(Objects::nonNull)
//                                .distinct()
//                                .toArray(Integer[]::new)
//                        );
//                        // Clean the criterion (to avoid clean to exclude too many data)
//                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
//                        criterion.setValues(null);
//                        break;
//                    case ObservedLocationSpecification.COLUMN_RECORDER_ID:
//                        target.setRecorderPersonId(
//                            Arrays.stream(criterion.getValues()).map(Integer::valueOf).toArray(Integer[]::new)
//                        );
//                        // Clean the criterion (to avoid clean to exclude too many data)
//                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
//                        criterion.setValues(null);
//                        break;
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

    protected Class<? extends ExtractionObservedLocationContextVO> getContextClass() {
        return ExtractionObservedLocationContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionObservedLocationContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setObservedLocationTableName(formatTableName(OL_TABLE_NAME_PATTERN, context.getId()));
        context.setVesselTableName(formatTableName(VESSEL_TABLE_NAME_PATTERN, context.getId()));
        context.setCatchTableName(formatTableName(CATCH_TABLE_NAME_PATTERN, context.getId()));
        context.setCatchLotTableName(formatTableName(CATCH_LOT_TABLE_NAME_PATTERN, context.getId()));
        context.setCatchIndividualTableName(formatTableName(CATCH_INDIVIDUAL_TABLE_NAME_PATTERN, context.getId()));
        context.setTripTableName(formatTableName(TRIP_TABLE_NAME_PATTERN, context.getId()));
        context.setTripCalendarTableName(formatTableName(TRIP_CALENDAR_TABLE_NAME_PATTERN, context.getId()));
        context.setObserverTableName(formatTableName(OBSERVER_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet name
        context.setObservedLocationSheetName(ObservedLocationSpecification.OL_SHEET_NAME);
        context.setVesselSheetName(ObservedLocationSpecification.VESSEL_SHEET_NAME);
        context.setCatchSheetName(ObservedLocationSpecification.CATCH_SHEET_NAME);
        context.setCatchLotSheetName(ObservedLocationSpecification.CATCH_LOT_SHEET_NAME);
        context.setCatchIndividualSheetName(ObservedLocationSpecification.CATCH_INDIVIDUAL_SHEET_NAME);
        context.setTripSheetName(ObservedLocationSpecification.TRIP_SHEET_NAME);
        context.setTripCalendarSheetName(ObservedLocationSpecification.TRIP_CALENDAR_SHEET_NAME);
        context.setObserverSheetName(ObservedLocationSpecification.TRIP_CALENDAR_SHEET_NAME);

    }

    protected long createObservedLocationTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getObservedLocationTableName();

        // Create Observed location Table
        XMLQuery xmlQuery = createObservedLocationQuery(context);
        execute(context, xmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getObservedLocationSheetName());
        }

        context.addTableName(tableName,
                context.getObservedLocationSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());

        return count;
    }

    protected long createVesselTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getVesselTableName();

        // Create Result Table with an injection
        XMLQuery xmlQuery = createVesselQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getVesselSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getVesselSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("Monitoring table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected long createCatchTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getCatchTableName();

        XMLQuery xmlQuery = createCatchQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getCatchSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getCatchSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("Catch table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected long createCatchLotTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getCatchLotTableName();

        XMLQuery xmlQuery = createCatchLotQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getCatchLotSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getCatchLotSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("CatchLot table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected long createCatchIndividualTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getCatchIndividualTableName();

        XMLQuery xmlQuery = createCatchIndividualQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getCatchIndividualSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getCatchLotSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("CatchIndividual table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected long createTripTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getTripTableName();

        XMLQuery xmlQuery = createTripQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getTripSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getTripSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("Trip table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected long createTripCalendarTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getTripCalendarTableName();

        XMLQuery xmlQuery = createTripCalendarQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getTripCalendarSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getTripCalendarSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("Trip table: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected long createObserverTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getObserverTableName();

        XMLQuery xmlQuery = createObserverQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getObserverSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getObserverSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("ObserverTable: {} rows inserted", count);
        } else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected XMLQuery createObservedLocationQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
//        context.setStartDate(Dates.getFirstDayOfYear(year));
//        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createObservedLocationTable");
        xmlQuery.bind("observedLocationTableName", context.getObservedLocationTableName());

        // Location levels
        xmlQuery.bind("locationLevelHarbour", LocationLevelEnum.HARBOUR.getId());
        xmlQuery.bind("locationLevelDistrict", LocationLevelEnum.DISTRICT.getId());
        xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());

        // Program filter
        {
            List<String> programLabels = context.getProgramLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(programLabels);
            xmlQuery.setGroup("programFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("programLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));
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

        // Base port location
        {
            List<Integer> locationIds = context.getLocationIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(locationIds);
            xmlQuery.setGroup("locationFilter", enableFilter);
            xmlQuery.setGroup("!locationFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("locationIds", Daos.getSqlInNumbers(locationIds));
        }

        // Vessel code
//        {
//            List<Integer> vesselIds = context.getVesselIds();
//            boolean enableFilter = CollectionUtils.isNotEmpty(vesselIds);
//            xmlQuery.setGroup("vesselFilter", enableFilter);
//            xmlQuery.setGroup("!vesselFilter", !enableFilter);
//            if (enableFilter) xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(vesselIds));
//        }

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

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createVesselQuery(C context) throws PersistenceException {

        XMLQuery xmlQuery = createXMLQuery(context, "createVesselTable");
        xmlQuery.bind("vesselTableName", context.getVesselTableName());
        xmlQuery.bind("observedLocationTableName", context.getObservedLocationTableName());

        xmlQuery.bind("qualitativeValueRefusedSurveyYes", QualitativeValueEnum.REFUSED_SURVEY_YES.getId());
        xmlQuery.bind("pmfmRefusedSurvey", PmfmEnum.REFUSED_SURVEY.getId());
        xmlQuery.bind("pmfmVesselPortState", PmfmEnum.VESSEL_PORT_STATE.getId());

        // Program filter
        {
            List<String> programLabels = context.getProgramLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(programLabels);
            xmlQuery.setGroup("programFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("programLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));
        }

        // Year filter
        if (context.getYear() != null) {
            xmlQuery.setGroup("yearFilter", true);
            xmlQuery.bind("year", context.getYear());
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

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createCatchQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createCatchTable");
        xmlQuery.bind("catchTableName", context.getCatchTableName());

        // Pmfms
        xmlQuery.bind("gearPhysicalHookNumber", PmfmEnum.GEAR_PHYSICAL_HOOK_NUMBER.getId());
        xmlQuery.bind("gearPhysicalGearNumber", PmfmEnum.GEAR_PHYSICAL_GEAR_NUMBER.getId());
        xmlQuery.bind("conditionnementPmfm", PmfmEnum.AVERAGE_PIECES_PRICE.getId());
        xmlQuery.bind("locationFilter", LocationLevelEnum.COUNTRY.getId());

        // LocationLevelQuarter filter
        {
            xmlQuery.bind("locationLevelQuarter", LocationLevelEnum.DISTRICT.getId());
        }

        // LocationLevelRegion filter
        {
            xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());
        }

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
            List<Integer> extractId = context.getIncludedIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(extractId);
            xmlQuery.setGroup("extractsFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("extractIds", Daos.getSqlInNumbers(extractId));
        }

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createCatchLotQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createCatchLotTable");
        xmlQuery.bind("catchLotTableName", context.getCatchLotTableName());

        // Pmfms
        xmlQuery.bind("gearPhysicalHookNumber", PmfmEnum.GEAR_PHYSICAL_HOOK_NUMBER.getId());
        xmlQuery.bind("gearPhysicalGearNumber", PmfmEnum.GEAR_PHYSICAL_GEAR_NUMBER.getId());

        // LocationLevelQuarter filter
        {
            xmlQuery.bind("locationLevelQuarter", LocationLevelEnum.DISTRICT.getId());
        }

        // LocationLevelRegion filter
        {
            xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());
        }

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
            List<Integer> extractId = context.getIncludedIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(extractId);
            xmlQuery.setGroup("extractsFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("extractIds", Daos.getSqlInNumbers(extractId));
        }

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createCatchIndividualQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createCatchIndividualTable");
        xmlQuery.bind("catchIndividualTableName", context.getCatchIndividualTableName());

        // Pmfms
        {
            xmlQuery.bind("gearPhysicalHookNumber", PmfmEnum.GEAR_PHYSICAL_HOOK_NUMBER.getId());
            xmlQuery.bind("gearPhysicalGearNumber", PmfmEnum.GEAR_PHYSICAL_GEAR_NUMBER.getId());
            xmlQuery.bind("conditionnementPmfm", PmfmEnum.AVERAGE_PIECES_PRICE.getId());
            xmlQuery.bind("biologicalWeightPmfm", PmfmEnum.BIOLOGICAL_WEIGHT.getId());
            xmlQuery.bind("biologicalLengthPmfm", PmfmEnum.BIOLOGICAL_LENGTH.getId());
            xmlQuery.bind("locationFilter", LocationLevelEnum.COUNTRY.getId());
        }

        // LocationLevelQuarter filter
        {
            xmlQuery.bind("locationLevelQuarter", LocationLevelEnum.DISTRICT.getId());
        }

        // LocationLevelRegion filter
        {
            xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());
        }

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
            List<Integer> extractId = context.getIncludedIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(extractId);
            xmlQuery.setGroup("extractsFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("extractIds", Daos.getSqlInNumbers(extractId));
        }

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createTripQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createTripTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());

        // Pmfms
        {
            xmlQuery.bind("crewSizePmfm", PmfmEnum.CREW_SIZE.getId());
            xmlQuery.bind("durationAtSeaPmfm", PmfmEnum.DURATION_AT_SEA_DAYS.getId());
            xmlQuery.bind("gearPhysicalGearNumber", PmfmEnum.GEAR_PHYSICAL_GEAR_NUMBER.getId());
            xmlQuery.bind("gearPhysicalHookNumber", PmfmEnum.GEAR_PHYSICAL_HOOK_NUMBER.getId());
            xmlQuery.bind("declarationDocumentPmfm", PmfmEnum.DECLARATIVE_DOCUMENT.getId());
            xmlQuery.bind("seaStatePmfm", PmfmEnum.SEA_STATE.getId());
            xmlQuery.bind("surveyQualificationPmfm", PmfmEnum.SURVEY_QUALIFICATION.getId());
        }

        {
            xmlQuery.bind("programObsdeb", ProgramEnum.SIH_OBSDEB.getLabel());
            xmlQuery.bind("programOprdeb", ProgramEnum.SIH_OPRDEB.getLabel());
        }

        // LocationLevelQuarter filter
        {
            xmlQuery.bind("locationLevelQuarter", LocationLevelEnum.DISTRICT.getId());
        }

        // LocationLevelRegion filter
        {
            xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());
        }

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
            List<Integer> extractId = context.getIncludedIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(extractId);
            xmlQuery.setGroup("extractsFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("extractIds", Daos.getSqlInNumbers(extractId));
        }

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createTripCalendarQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createTripCalendarTable");
        xmlQuery.bind("tripCalendarTableName", context.getTripCalendarTableName());

        // Pmfms
        {
            xmlQuery.bind("inactivityReasonPmfm", PmfmEnum.INACTIVITY_REASON.getId());
            xmlQuery.bind("crewSizePmfm", PmfmEnum.CREW_SIZE.getId());
        }

        {
            xmlQuery.bind("programObsdeb", ProgramEnum.SIH_OBSDEB.getLabel());
            xmlQuery.bind("programOprdeb", ProgramEnum.SIH_OPRDEB.getLabel());
        }

        // LocationLevelQuarter filter
        {
            xmlQuery.bind("locationLevelQuarter", LocationLevelEnum.DISTRICT.getId());
        }

        // LocationLevelRegion filter
        {
            xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());
        }

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
            List<Integer> extractId = context.getIncludedIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(extractId);
            xmlQuery.setGroup("extractsFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("extractIds", Daos.getSqlInNumbers(extractId));
        }

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

    protected XMLQuery createObserverQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createObserverTable");
        xmlQuery.bind("observerTableName", context.getObserverTableName());

        // Pmfms
        {
            xmlQuery.bind("surveyQualification", PmfmEnum.SURVEY_QUALIFICATION.getId());
        }

        {
            xmlQuery.bind("programObsdeb", ProgramEnum.SIH_OBSDEB.getLabel());
            xmlQuery.bind("programOprdeb", ProgramEnum.SIH_OPRDEB.getLabel());
        }

        // LocationLevelQuarter filter
        {
            xmlQuery.bind("locationLevelQuarter", LocationLevelEnum.DISTRICT.getId());
        }

        // LocationLevelRegion filter
        {
            xmlQuery.bind("locationLevelRegion", LocationLevelEnum.REGION.getId());
        }

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

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }


}

