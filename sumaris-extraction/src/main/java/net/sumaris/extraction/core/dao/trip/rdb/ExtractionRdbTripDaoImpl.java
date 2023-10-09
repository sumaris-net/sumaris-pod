package net.sumaris.extraction.core.dao.trip.rdb;

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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.model.referential.taxon.TaxonGroupTypeEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.service.data.denormalize.DenormalizedBatchService;
import net.sumaris.core.service.data.denormalize.DenormalizedOperationService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.programStrategy.DenormalizedPmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyFetchOptions;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.OperationFilterVO;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.trip.ExtractionTripDao;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionRdbTripDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@Slf4j
public class ExtractionRdbTripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl<C, F>
        implements ExtractionTripDao<C, F> {

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.SL_SHEET_NAME + "_%s";
    private static final String SL_RAW_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "RAW_" + RdbSpecification.SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.HL_SHEET_NAME + "_%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.CA_SHEET_NAME + "_%s";


    protected Splitter splitter = Splitter.on(",").trimResults();

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ExtractionConfiguration extractionConfiguration;

    @Autowired
    protected DenormalizedBatchService denormalizedBatchService;
    @Autowired
    protected DenormalizedOperationService denormalizedOperationService;

    protected boolean enableTripSamplingMethodColumn = true;

    protected boolean enableRecordTypeColumn = true;

    @Override
    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.RDB);
    }

    @Override
    public <R extends C> R execute(F filter) {
        ExtractionTripFilterVO tripFilter = toTripFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setTripFilter(tripFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.RDB);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        // Fill context table names
        fillContextTableNames(context);

        // Start log
        Long startTime = null;
        if (log.isInfoEnabled()) {
            startTime = System.currentTimeMillis();
            StringBuilder filterInfo = new StringBuilder();
            String filterStr = filter != null ? tripFilter.toString("\n - ") : null;
            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter:").append(filterStr);
            }
            else {
                filterInfo.append("(without filter)");
            }
            filterInfo.append("\n - Batch denormalization: " + extractionConfiguration.enableBatchDenormalization());
            log.info("Starting extraction #{} (trips)... {}", context.getId(), filterInfo);
        }

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --

        try {
            // If only CL expected: skip station/species extraction
            boolean hasSomeRow = false;
            if (!RdbSpecification.CL_SHEET_NAME.equals(sheetName)) {
                // Trip
                long rowCount = createTripTable(context);
                hasSomeRow = rowCount > 0;
                if (sheetName != null && context.hasSheet(sheetName)) return context;

                // Station
                if (rowCount != 0) {
                    rowCount = createStationTable(context);
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }

                // Execute batch denormalization (if enable)
                if (rowCount != 0 && enableBatchDenormalization(context)) {
                    denormalizeBatches(context);
                }

                // Species Raw table
                if (rowCount != 0) {
                    rowCount = createRawSpeciesListTable(context, tripFilter.isExcludeInvalidStation());
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }

                // Species List
                if (rowCount != 0) {
                    rowCount = createSpeciesListTable(context);
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }

                // Species Length
                if (rowCount != 0) {
                    rowCount = createSpeciesLengthTable(context);
                    if (sheetName != null && context.hasSheet(sheetName)) return context;
                }
            }

            // Landing table
            {
                long rowCount = createLandingTable(context);
                hasSomeRow = hasSomeRow || rowCount > 0;
                if (sheetName != null && context.hasSheet(sheetName)) return context;
            }

            // No data
            if (!hasSomeRow) throw new DataNotFoundException(t("sumaris.extraction.noData"));

            return context;
        }
        catch (PersistenceException e) {
            // If error, clean created tables first, then rethrow the exception
            clean(context);

            startTime = null; // Avoid log

            throw e;
        }
        finally {
            if (startTime != null) {
                log.info("Extraction #{} finished in {}", context.getId(), TimeUtils.printDurationFrom(startTime));
            }
        }
    }


    /* -- protected methods -- */

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionRdbTripContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected Class<? extends ExtractionRdbTripContextVO> getContextClass() {
        return ExtractionRdbTripContextVO.class;
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setTripTableName(formatTableName(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(formatTableName(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setRawSpeciesListTableName(formatTableName(SL_RAW_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(formatTableName(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(formatTableName(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSampleTableName(formatTableName(CA_TABLE_NAME_PATTERN, context.getId()));

        // Set sheetname
        context.setTripSheetName(RdbSpecification.TR_SHEET_NAME);
        context.setStationSheetName(RdbSpecification.HH_SHEET_NAME);
        context.setSpeciesListSheetName(RdbSpecification.SL_SHEET_NAME);
        context.setSpeciesLengthSheetName(RdbSpecification.HL_SHEET_NAME);
        context.setSampleSheetName(RdbSpecification.CA_SHEET_NAME);
    }

    protected long createTripTable(C context) {

        XMLQuery xmlQuery = createTripQuery(context);

        // execute insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getTripTableName());

        // Update self sampling columns
        if (count > 0 && enableTripSamplingMethodColumn) {
            updateTripSamplingMethod(context);
        }

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getTripTableName(), context.getFilter(), context.getTripSheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getTripTableName(),
                    context.getTripSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Trip table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getTripTableName());
        }
        return count;
    }

    protected XMLQuery createTripQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createTripTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());

        // Bind some referential ids
        xmlQuery.bind("nbOperationPmfmId", String.valueOf(PmfmEnum.NB_OPERATION.getId()));
        Integer countryLocationLevelId = LocationLevelEnum.COUNTRY.getLabel() != null ? getReferentialIdByUniqueLabel(LocationLevel.class, LocationLevelEnum.COUNTRY.getLabel()) : LocationLevelEnum.COUNTRY.getId();
        xmlQuery.bind("countryLocationLevelId", String.valueOf(countryLocationLevelId));
        xmlQuery.bind("samplingMethod", StringUtils.trimToEmpty(ProgramPropertyEnum.TRIP_EXTRACTION_SAMPLING_METHOD.getDefaultValue()));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program filter
        {
            List<String> programLabels = context.getProgramLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(programLabels);
            xmlQuery.setGroup("programFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));
        }

        // Location Filter
        {
            List<Integer> locationIds = context.getLocationIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(locationIds);
            xmlQuery.setGroup("locationFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("locationIds", Daos.getSqlInNumbers(locationIds));
        }

        // Recorder Department filter
        {
            List<Integer> recorderDepartmentIds = context.getRecorderDepartmentIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(recorderDepartmentIds);
            xmlQuery.setGroup("departmentFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("recDepIds", Daos.getSqlInNumbers(recorderDepartmentIds));
        }

        // Vessel filter
        {
            List<Integer> vesselIds = context.getVesselIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(vesselIds);
            xmlQuery.setGroup("vesselFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(vesselIds));
        }

        // Trip filter
        {
            List tripIds = context.getTripIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(tripIds);
            xmlQuery.setGroup("tripFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("tripIds", Daos.getSqlInNumbers(tripIds));
        }

        // Record type
        xmlQuery.setGroup("recordType", enableRecordTypeColumn);

        return xmlQuery;
    }

    protected long createStationTable(C context) {

        XMLQuery xmlQuery = createStationQuery(context);

        // execute insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getStationTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getStationTableName(), context.getFilter(), context.getStationSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(context.getStationTableName(),
                    context.getStationSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Station table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getStationTableName());
        }


        return count;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("stationTableName", context.getStationTableName());

        // Bind location level ids
        xmlQuery.bind("areaLocationLevelIds", Daos.getSqlInNumbers(getAreaLocationLevelIds(context)));
        xmlQuery.bind("rectangleLocationLevelIds", Daos.getSqlInNumbers(getRectangleLocationLevelIds(context)));

        // Bind some PMFM ids
        xmlQuery.bind("meshSizePmfmId", String.valueOf(PmfmEnum.SMALLER_MESH_GAUGE_MM.getId()));
        xmlQuery.bind("mainFishingDepthPmfmId", String.valueOf(PmfmEnum.GEAR_DEPTH_M.getId()));
        xmlQuery.bind("mainWaterDepthPmfmId", String.valueOf(PmfmEnum.BOTTOM_DEPTH_M.getId()));
        xmlQuery.bind("selectionDevicePmfmIds", Daos.getSqlInNumbers(getSelectivityDevicePmfmIds()));
        xmlQuery.bind("tripProgressPmfmId", String.valueOf(PmfmEnum.TRIP_PROGRESS.getId()));
        xmlQuery.bind("nationalTaxonGroupTypeId", String.valueOf(TaxonGroupTypeEnum.NATIONAL.getId()));
        xmlQuery.bind("ueLevel5TaxonGroupTypeId", String.valueOf(TaxonGroupTypeEnum.DCF_METIER_LVL_5.getId()));

        // ENable some columns, using groups
        xmlQuery.setGroup("gearType", true);
        xmlQuery.setGroup("date", true);
        xmlQuery.setGroup("time", true);
        xmlQuery.setGroup("fishingTime", true);
        xmlQuery.setGroup("selectionDevice", true);

        // Record type
        xmlQuery.setGroup("recordType", enableRecordTypeColumn);

        // Operation filter
        {
            List operationIds = context.getOperationIds();
            boolean enableFilter = CollectionUtils.isNotEmpty(operationIds);
            xmlQuery.setGroup("operationIdsFilter", enableFilter);
            xmlQuery.setGroup("!operationIdsFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("operationIds", Daos.getSqlInNumbers(operationIds));
        }

        // Compute groupBy
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected DenormalizedBatchOptions createDenormalizedBatchOptions(String programLabel) {
        return denormalizedOperationService.createOptionsByProgramLabel(programLabel);
    }
    protected void denormalizeBatches(C context) {
        String stationsTableName = context.getStationTableName();
        Set<String> programLabels = getTripProgramLabels(context);
        programLabels.forEach(programLabel -> {
            // Get all operation ids
            String sql = String.format("SELECT distinct CAST(%s AS INT) from %s where %s='%s'",
                    RdbSpecification.COLUMN_STATION_ID, stationsTableName, RdbSpecification.COLUMN_PROJECT, programLabel);
            Number[] operationIds = query(sql, Number.class).toArray(Number[]::new);

            DenormalizedBatchOptions options = createDenormalizedBatchOptions(programLabel);
            // DEBUG
            //options.setEnableRtpWeight(false);
            //if (!this.production) options.setForce(true);

            int pageSize = 500;
            long pageCount = Math.round((double)(operationIds.length / pageSize) + 0.5); // Get page count
            for (int page = 0; page < pageCount; page++) {
                int from = page * pageSize;
                int to = Math.min(operationIds.length, from + pageSize);
                Integer[] pageOperationIds = Arrays.stream(Arrays.copyOfRange(operationIds, from, to))
                    .mapToInt(Number::intValue)
                    .mapToObj(Integer::valueOf)
                    .toArray(Integer[]::new);

                denormalizedOperationService.denormalizeByFilter(OperationFilterVO.builder()
                    .programLabel(programLabel)
                    .includedIds(pageOperationIds)
                    .hasNoChildOperation(true)
                    .needBatchDenormalization(!options.isForce())
                    .build(), options);
            }
        });
    }

    /**
     * Create raw table (with hidden columns used by sub table - e.g. SAMPLE_ID)
     * @param context
     * @param excludeInvalidStation
     * @return
     */
    protected long createRawSpeciesListTable(C context, boolean excludeInvalidStation) {
        String tableName = context.getRawSpeciesListTableName();

        XMLQuery rawXmlQuery = createRawSpeciesListQuery(context, excludeInvalidStation);
        execute(context, rawXmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getSpeciesListSheetName());
        }

        if (this.enableCleanup && !this.production) {
            // Add as a raw table (to be able to clean it later)
            context.addRawTableName(tableName);
        }
        // Keep raw table (for DEBUG only)
        else {
            context.addTableName(tableName, RdbSpecification.SL_RAW_SHEET_NAME, rawXmlQuery.getHiddenColumnNames(), rawXmlQuery.hasDistinctOption());
        }

        return count;
    }


    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        String queryName = enableBatchDenormalization(context)
            ? "createRawSpeciesListDenormalizeTable"
            : "createRawSpeciesListTable";

        XMLQuery xmlQuery = createXMLQuery(context, queryName);
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());

        // Bind some constants (e.g. should be RDB defaults, but can be overridden by configuration)
        xmlQuery.bind("defaultLandingCategory", configuration.getExtractionDefaultLandingCategory());
        xmlQuery.bind("defaultCommercialSizeCategoryScale", configuration.getExtractionDefaultCommercialSizeCategoryScale());

        // Bind some ids
        xmlQuery.bind("catchCategoryPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));
        xmlQuery.bind("landingQvId", String.valueOf(QualitativeValueEnum.LANDING.getId()));
        xmlQuery.bind("discardQvId", String.valueOf(QualitativeValueEnum.DISCARD.getId()));
        xmlQuery.bind("landingCategoryPmfmId", String.valueOf(PmfmEnum.LANDING_CATEGORY.getId()));
        xmlQuery.bind("sizeCategoryPmfmIds", Daos.getSqlInNumbers(getSizeCategoryPmfmIds()));
        xmlQuery.bind("subsamplingCategoryPmfmId", String.valueOf(PmfmEnum.BATCH_SORTING.getId()));
        xmlQuery.bind("lengthPmfmIds", Daos.getSqlInNumbers(getSpeciesLengthPmfmIds()));


        // Exclude not valid station
        xmlQuery.setGroup("excludeInvalidStation", excludeInvalidStation);

        // Set defaults
        xmlQuery.setGroup("excludeNoWeight", true);
        xmlQuery.setGroup("weight", true);
        xmlQuery.setGroup("lengthCode", true);

        // Enable Landing/discard
        xmlQuery.setGroup("hasLandingOrDiscardPmfm", true);
        xmlQuery.setGroup("!hasLandingOrDiscardPmfm", false);

        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);


        return xmlQuery;
    }

    protected long createSpeciesListTable(C context) {
        String tableName = context.getSpeciesListTableName();

        XMLQuery xmlQuery = createSpeciesListQuery(context);
        execute(context, xmlQuery);

        long count = countFrom(tableName);

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getSpeciesListSheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    context.getSpeciesListSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Species list table: %s rows inserted", count));
        }
        else {
            // Add as a raw table (to be able to clean it later)
            context.addRawTableName(tableName);
        }


        return count;
    }

    protected XMLQuery createSpeciesListQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        xmlQuery.setGroup("weight", true);
        xmlQuery.setGroup("lengthCode", true);

        // Record type
        xmlQuery.setGroup("recordType", enableRecordTypeColumn);

        // Always disable injectionPoint group to avoid injection point staying on final xml query (if not used to inject pmfm)
        xmlQuery.setGroup("injectionPoint", false);

        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected long createSpeciesLengthTable(C context) {
        String tableName = context.getSpeciesLengthTableName();

        XMLQuery xmlQuery = createSpeciesLengthQuery(context);
        execute(context, xmlQuery);

        long count = countFrom(tableName);

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getSpeciesLengthSheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    context.getSpeciesLengthSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Species length table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(tableName);
        }
        return count;
    }


    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesLengthTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());
        xmlQuery.bind("speciesLengthTableName", context.getSpeciesLengthTableName());

        // Bind some ids
        xmlQuery.bind("sexPmfmId", String.valueOf(PmfmEnum.SEX.getId()));
        xmlQuery.bind("lengthPmfmIds", Daos.getSqlInNumbers(getSpeciesLengthPmfmIds()));
        xmlQuery.bind("centimeterUnitId", String.valueOf(UnitEnum.CM.getId()));
        xmlQuery.bind("millimeterUnitId", String.valueOf(UnitEnum.MM.getId()));

        xmlQuery.setGroup("sex", true);
        xmlQuery.setGroup("lengthClass", true);
        xmlQuery.setGroup("numberAtLength", true);

        // Record type
        xmlQuery.setGroup("recordType", enableRecordTypeColumn);

        // Taxon disabled by default (RDB format has only one HL.SPECIES column)
        // But group can be enabled by subsclasses (e.g. see PMFM_TRIP format)
        xmlQuery.setGroup("taxon", this.enableSpeciesLengthTaxon(context));


        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected long createLandingTable(C context) {
        // TODO create the landing query and table
        return 0;
    }

    protected Collection<Integer> getSpeciesLengthPmfmIds() {
        List<Integer> configList = configuration.getExtractionSpeciesLengthPmfmIds();
        if (CollectionUtils.isNotEmpty(configList)) {
            return configList;
        }

        // Default list
        return ImmutableSet.of(
            PmfmEnum.LENGTH_TOTAL_CM.getId(),
            PmfmEnum.LENGTH_TOTAL_MM.getId(),
            PmfmEnum.LENGTH_CARAPACE_CM.getId(),
            PmfmEnum.LENGTH_CARAPACE_MM.getId(),
            PmfmEnum.LENGTH_MANTLE_CM.getId(),
            PmfmEnum.SEGMENT_LENGTH_MM.getId(),
            PmfmEnum.HEIGHT_MM.getId(),
            PmfmEnum.LENGTH_LM_FORK_CM.getId(),
            PmfmEnum.LENGTH_FORK_CM.getId(),
            PmfmEnum.LENGTH_PRE_SUPRA_CAUDAL_CM.getId(),
            PmfmEnum.DOM_HALF_CM.getId(),
            PmfmEnum.WIDTH_CARAPACE_MM.getId()
        );
    }

    protected Set<Integer> getSpeciesListExcludedPmfmIds() {
        return ImmutableSet.<Integer>builder()
            .add(
                PmfmEnum.BATCH_CALCULATED_WEIGHT.getId(),
                PmfmEnum.BATCH_MEASURED_WEIGHT.getId(),
                PmfmEnum.BATCH_ESTIMATED_WEIGHT.getId(),
                PmfmEnum.BATCH_CALCULATED_WEIGHT_LENGTH_SUM.getId(),
                PmfmEnum.BATCH_CALCULATED_WEIGHT_LENGTH.getId(),
                PmfmEnum.DISCARD_OR_LANDING.getId(),
                PmfmEnum.BATCH_SORTING.getId(),
                PmfmEnum.CATCH_WEIGHT.getId(),
                PmfmEnum.DISCARD_WEIGHT.getId()
            )
            .addAll(getSizeCategoryPmfmIds())
            .build();
    }

    protected Set<Integer> getSizeCategoryPmfmIds() {
        return ImmutableSet.of(
            PmfmEnum.SIZE_CATEGORY.getId(),
            PmfmEnum.SIZE_UNLI_CAT.getId()
        );
    }

    protected Set<Integer> getSelectivityDevicePmfmIds() {
        return ImmutableSet.<Integer>builder()
            .add(
                PmfmEnum.SELECTIVITY_DEVICE.getId(),
                PmfmEnum.SELECTIVITY_DEVICE_APASE.getId()
            )
            .build();
    }

    protected Collection<Integer> getAreaLocationLevelIds(C context) {
        List<Integer> configList = configuration.getExtractionAreaLocationLevelIds();
        if (CollectionUtils.isNotEmpty(configList)) {
            return configList;
        }

        Set<String> programLabels = getTripProgramLabels(context);

        return programLabels.stream().flatMap(programLabel -> {
            String strValue = this.programService.getPropertyValueByProgramLabel(programLabel, ProgramPropertyEnum.TRIP_EXTRACTION_AREA_LOCATION_LEVEL_IDS);
            if (StringUtils.isBlank(strValue)) {
                // Default values
                return Stream.of(
                    // Sous-Division CIEM (cf issue #416)
                    LocationLevelEnum.SUB_DIVISION_ICES.getId(),
                    // Sous-division GFCM - à valider
                    LocationLevelEnum.SUB_DIVISION_GFCM.getId()
                );
            }
            return splitter.splitToStream(strValue).map(Integer::parseInt);
        }).collect(Collectors.toSet());

    }

    protected Set<Integer> getRectangleLocationLevelIds(C context) {
        Set<String> programLabels = getTripProgramLabels(context);

        return programLabels.stream().flatMap(programLabel -> {
            String strValue = this.programService.getPropertyValueByProgramLabel(programLabel, ProgramPropertyEnum.TRIP_OPERATION_FISHING_AREA_LOCATION_LEVEL_IDS);
            if (StringUtils.isBlank(strValue)) {
                // Default values
                return Stream.of(
                    LocationLevelEnum.RECTANGLE_ICES.getId(),
                    LocationLevelEnum.RECTANGLE_GFCM.getId()
                );
            }
            return splitter.splitToStream(strValue).map(Integer::parseInt);
        }).collect(Collectors.toSet());

    }

    /**
     * Fill the context's pmfm infos
     * @param context
     */
    protected List<DenormalizedPmfmStrategyVO> loadPmfms(C context,
                                                           Set<String> programLabels,
                                                           AcquisitionLevelEnum... acquisitionLevels) {

        if (CollectionUtils.isEmpty(programLabels)) return Collections.emptyList(); // no selected programs: skip

        // Create the map that holds the result
        Map<String, List<DenormalizedPmfmStrategyVO>> pmfms = context.getPmfmsCacheMap();
        List<Integer> acquisitionLevelIds = Beans.getStream(acquisitionLevels)
            .map(AcquisitionLevelEnum::getId)
            .toList();

        String cacheKey = programLabels.toString() + acquisitionLevelIds;

        // Already loaded: use the cached values
        if (pmfms.containsKey(cacheKey)) return pmfms.get(cacheKey);

        if (log.isTraceEnabled()) {
            log.trace("Loading PMFM for programs {} and acquisitionLevels {} ...",
                programLabels,
                acquisitionLevels
            );
        }

        // Load denormalized pmfm
        List<DenormalizedPmfmStrategyVO> result = strategyService.findDenormalizedPmfmsByFilter(
                PmfmStrategyFilterVO.builder()
                    .programLabels(programLabels.toArray(new String[0]))
                    .acquisitionLevelIds(acquisitionLevelIds.toArray(new Integer[0]))
                    .build(),
                PmfmStrategyFetchOptions.builder().withCompleteName(false).build()
            );

        // save result into the context map
        pmfms.put(cacheKey, result);

        return result;
    }

    /**
     * Fill the context's pmfm infos (e.g. used to generate
     * @param context
     */
    protected List<ExtractionPmfmColumnVO> loadPmfmColumns(C context,
                                                           Set<String> programLabels,
                                                           AcquisitionLevelEnum... acquisitionLevels) {

        if (CollectionUtils.isEmpty(programLabels)) return Collections.emptyList(); // no selected programs: skip

        // Create the map that holds the result
        Map<String, List<ExtractionPmfmColumnVO>> pmfmColumns = context.getPmfmsColumnsCacheMap();

        List<Integer> acquisitionLevelIds = Beans.getStream(acquisitionLevels)
            .map(AcquisitionLevelEnum::getId)
            .toList();

        String cacheKey = programLabels.toString() + acquisitionLevelIds.toString();

        // Already loaded: use the cached values
        if (pmfmColumns.containsKey(cacheKey)) return pmfmColumns.get(cacheKey);

        if (log.isDebugEnabled()) {
            log.debug("Loading PMFM columns for programs {} and acquisitionLevels: {} ...",
                programLabels,
                acquisitionLevels
            );
        }

        // Load pmfm columns
        List<ExtractionPmfmColumnVO> result = loadPmfms(context, programLabels, acquisitionLevels)
                .stream()
                .map(pmfm -> toPmfmColumnVO(pmfm, null))

                // Group by pmfmId
                .collect(Collectors.groupingBy(ExtractionPmfmColumnVO::getPmfmId))
                .values().stream().map(list -> list.get(0))

                // Sort by rankOrder
                .sorted(Comparator.comparing(ExtractionPmfmColumnVO::getRankOrder, Integer::compareTo))
                .toList();

        // save result into the context map
        pmfmColumns.put(cacheKey, result);

        return result;
    }

    protected Set<String> getTripProgramLabels(C context) {

        Set<String> result = context.getTripProgramLabels();
        if (result == null) {

            XMLQuery xmlQuery = createXMLQuery(context, "distinctTripProgram");
            xmlQuery.bind("tableName", context.getTripTableName());

            result = queryToSet(xmlQuery.getSQLQueryAsString(), String.class);

            // Fill cache
            context.setTripProgramLabels(result);
        }

        return result;
    }

    private ExtractionPmfmColumnVO toPmfmColumnVO(DenormalizedPmfmStrategyVO source, String aliasPrefix) {
        ExtractionPmfmColumnVO target = new ExtractionPmfmColumnVO();

        target.setAcquisitionLevel(source.getAcquisitionLevel());
        target.setPmfmId(source.getId());
        target.setLabel(source.getLabel());
        target.setRankOrder(source.getRankOrder());
        target.setType(PmfmValueType.fromString(source.getType()));

        target.setAlias(
                (aliasPrefix != null ? aliasPrefix : "T_")
                + target.getPmfmId());
        return target;
    }

    /**
     * Build a map of unique alias, by acquisition level.
     * Will use the first letter if unique, or two letters, or tree, etc.
     * @param acquisitionLevels
     * @return
     */
    private Map<String, String> buildAcquisitionLevelPrefixes(Set<String> acquisitionLevels) {
        Map<String, String> aliases = new HashMap<>();
        for (String acquisitionLevel : acquisitionLevels) {
            String alias = buildAlias(acquisitionLevel, "_");
            if (aliases.values().contains(alias)) {
                int index = 1;
                String aliasToTest = alias + index;
                while (aliases.values().contains(aliasToTest)) {
                    aliasToTest = alias + ++index;
                }
                alias = aliasToTest;
            }
            aliases.put(acquisitionLevel, alias);
        }
        return aliases;
    }

    protected String buildAlias(String string, String separator) {
        StringBuilder result = new StringBuilder();
        Arrays.stream(string.split(separator)).forEach(part -> result.append(part, 0, 1));
        return result.toString();
    }

    /**
     * Read the trip table, and modify 'sampling_method' depending on the program properties found
     * @param context
     * @return
     */
    protected void updateTripSamplingMethod(C context) {
        try {
            Set<String> programLabels = getTripProgramLabels(context);
            String tripTableName = context.getTripTableName();
            SumarisTableMetadata table = databaseMetadata.getTable(tripTableName);
            if (table.hasColumn(RdbSpecification.COLUMN_SAMPLING_METHOD)) {
                programLabels.forEach(programLabel -> {
                    String samplingMethod = StringUtils.trimToNull(this.programService.getPropertyValueByProgramLabel(programLabel, ProgramPropertyEnum.TRIP_EXTRACTION_SAMPLING_METHOD));
                    if (samplingMethod != null && !Objects.equals(samplingMethod, ProgramPropertyEnum.TRIP_EXTRACTION_SAMPLING_METHOD.getDefaultValue())) {
                        String sql = String.format("UPDATE %s SET %s='%s' WHERE PROJECT='%s'",
                            tripTableName,
                            RdbSpecification.COLUMN_SAMPLING_METHOD,
                            samplingMethod,
                            programLabel);
                        execute(context, sql);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error while updating TR 'sampling_method' column: " + e.getMessage(), e);
            // Continue
        }
    }

    protected boolean enableSpeciesLengthTaxon(C context) {
        Set<String> programLabels = getTripProgramLabels(context);

        // Check if samples have been enabled:
        // - by program properties
        // - or by pmfm strategies
        return programLabels.stream()
            .anyMatch(label -> {
                boolean showBatchTaxonName = this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_BATCH_TAXON_NAME_ENABLE, Boolean.TRUE.toString());
                boolean showBatchLengthTaxonName = !showBatchTaxonName && this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_BATCH_MEASURE_INDIVIDUAL_TAXON_NAME_ENABLE, Boolean.TRUE.toString());
                return showBatchLengthTaxonName;
            });
    }


    protected Set<String> getTaxonGroupNoWeights(C context) {
        Set<String> result = context.getTaxonGroupNoWeights();
        if (result == null) {

            result = getTripProgramLabels(context).stream()
                .flatMap(programLabel -> {
                    String values = programService.getPropertyValueByProgramLabel(programLabel, ProgramPropertyEnum.TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT);
                    if (StringUtils.isBlank(values)) {
                        // No default value
                        return Stream.empty();
                    }
                    return splitter.splitToStream(values);
                }).collect(Collectors.toSet());

            // Fill cache
            context.setTaxonGroupNoWeights(result);
        }

        return result;
    }

    protected boolean enableBatchDenormalization(C context) {
        Boolean result = context.getEnableBatchDenormalization();
        if (result == null) {

            result = extractionConfiguration.enableBatchDenormalization() // Enable denormalization for all programs
                || getTripProgramLabels(context).stream() // Check if denormalization has been enabled on program
                .anyMatch(programLabel -> {
                    String value = programService.getPropertyValueByProgramLabel(programLabel, ProgramPropertyEnum.TRIP_EXTRACTION_BATCH_DENORMALIZATION_ENABLE);
                    if (StringUtils.isBlank(value)) return false; // = default value
                    return Boolean.parseBoolean(value);
                });

            // Fill cache
            context.setEnableBatchDenormalization(result);
        }

        return result;
    }
}
