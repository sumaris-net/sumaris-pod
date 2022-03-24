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
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.QualitativeValueEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.administration.programStrategy.DenormalizedPmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyFetchOptions;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.ExtractionTripDao;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionRdbTripDao")
@Lazy
@Slf4j
public class ExtractionRdbTripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl
        implements ExtractionTripDao<C, F> {

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.SL_SHEET_NAME + "_%s";
    private static final String SL_RAW_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "RAW_" + RdbSpecification.SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.HL_SHEET_NAME + "_%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RdbSpecification.CA_SHEET_NAME + "_%s";

    protected static final String XML_QUERY_PATH = "xmlQuery";

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.RDB;
    }

    @Override
    public <R extends C> R execute(F filter) {
        ExtractionTripFilterVO tripFilter = toTripFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setTripFilter(tripFilter);
        context.setFilter(filter);
        context.setId(System.currentTimeMillis());
        context.setFormat(LiveFormatEnum.RDB);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

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
            log.info("Starting extraction {{}-{}} (raw data / trips)... {}", context.getLabel(), context.getId(), filterInfo.toString());
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --

        try {
            // If only CL expected: skip station/species aggregation
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
                    createSpeciesLengthTable(context);
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
                log.info("Extraction {{}-{}} finished in {}", context.getLabel(), context.getId(), TimeUtils.printDurationFrom(startTime));
            }
        }
    }

    @Override
    public void clean(C context) {
        dropTables(context);
    }

    /* -- protected methods -- */

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionRdbTripContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.newInstance();
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

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getTripTableName());

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

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program filter
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        // Location Filter
        xmlQuery.setGroup("locationFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getSqlInNumbers(context.getLocationIds()));

        // Recorder Department filter
        xmlQuery.setGroup("departmentFilter", CollectionUtils.isNotEmpty(context.getRecorderDepartmentIds()));
        xmlQuery.bind("recDepIds", Daos.getSqlInNumbers(context.getRecorderDepartmentIds()));

        // Vessel filter
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getSqlInNumbers(context.getVesselIds()));

        // Trip filter
        xmlQuery.setGroup("tripFilter", context.getTripId() != null);
        if (context.getTripId() != null) xmlQuery.bind("tripId", context.getTripId().toString());

        return xmlQuery;
    }

    protected long createStationTable(C context) {

        XMLQuery xmlQuery = createStationQuery(context);

        // aggregate insertion
        execute(xmlQuery);
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

        // Bind some PMFM ids
        xmlQuery.bind("meshSizePmfmId", String.valueOf(PmfmEnum.SMALLER_MESH_GAUGE_MM.getId()));
        xmlQuery.bind("mainFishingDepthPmfmId", String.valueOf(PmfmEnum.GEAR_DEPTH_M.getId()));
        xmlQuery.bind("mainWaterDepthPmfmId", String.valueOf(PmfmEnum.BOTTOM_DEPTH_M.getId()));
        xmlQuery.bind("selectionDevicePmfmId", String.valueOf(PmfmEnum.SELECTIVITY_DEVICE.getId()));
        xmlQuery.bind("normalProgressPmfmId", String.valueOf(PmfmEnum.TRIP_PROGRESS.getId()));

        xmlQuery.setGroup("gearType", true);
        xmlQuery.setGroup("date", true);
        xmlQuery.setGroup("time", true);
        xmlQuery.setGroup("fishingTime", true);

        return xmlQuery;
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
        execute(rawXmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            cleanRow(tableName, context.getFilter(), context.getSpeciesListSheetName());
        }

        // Add as a raw table (to be able to clean it later)
        context.addRawTableName(tableName);

        return count;
    }


    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = createXMLQuery(context, "createRawSpeciesListTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());

        // Bind some ids
        xmlQuery.bind("catchCategoryPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));
        xmlQuery.bind("landingQvId", String.valueOf(QualitativeValueEnum.LANDING.getId()));
        xmlQuery.bind("discardQvId", String.valueOf(QualitativeValueEnum.DISCARD.getId()));

        // Exclude not valid station
        xmlQuery.setGroup("excludeInvalidStation", excludeInvalidStation);
        xmlQuery.setGroup("weight", true);
        xmlQuery.setGroup("lengthCode", true);

        return xmlQuery;
    }

    protected long createSpeciesListTable(C context) {
        String tableName = context.getSpeciesListTableName();

        XMLQuery xmlQuery = createSpeciesListQuery(context);
        execute(xmlQuery);

        long count = countFrom(tableName);

        // Clean row using generic filter
        if (count > 0) {
            cleanRow(tableName, context.getFilter(), context.getSpeciesListSheetName());
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
        // Always disable injectionPoint group to avoid injection point staying on final xml query (if not used to inject pmfm)
        xmlQuery.setGroup("injectionPoint", false);

        return xmlQuery;
    }

    protected long createSpeciesLengthTable(C context) {
        String tableName = context.getSpeciesLengthTableName();

        XMLQuery xmlQuery = createSpeciesLengthQuery(context);
        execute(xmlQuery);

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
        xmlQuery.bind("lengthTotalCmPmfmId", String.valueOf(PmfmEnum.LENGTH_TOTAL_CM.getId()));
        xmlQuery.bind("lengthCarapaceCmPmfmId", String.valueOf(PmfmEnum.LENGTH_CARAPACE_CM.getId()));
        xmlQuery.bind("centimeterUnitId", String.valueOf(UnitEnum.CM.getId()));
        xmlQuery.bind("millimeterUnitId", String.valueOf(UnitEnum.MM.getId()));

        xmlQuery.setGroup("sex", true);
        xmlQuery.setGroup("lengthClass", true);
        xmlQuery.setGroup("numberAtLength", true);

        return xmlQuery;
    }

    protected long createLandingTable(C context) {
        // TODO create the landing query and table
        return 0;
    }

    protected int execute(XMLQuery xmlQuery) {
        String sql = xmlQuery.getSQLQueryAsString();
        return queryUpdate(sql);
    }

    protected long countFrom(String tableName) {
        XMLQuery xmlQuery = createXMLQuery("countFrom");
        xmlQuery.bind("tableName", tableName);
        return queryCount(xmlQuery.getSQLQueryAsString());
    }

    protected String getQueryFullName(@NonNull C context, String queryName) {
        return getQueryFullName(context.getLabel(), context.getVersion(), queryName);
    }

    protected String getQueryFullName(@NonNull String formatLabel,
                                      @NonNull String formatVersion,
                                      @NonNull String queryName) {
        return String.format("%s/v%s/%s",
            StringUtils.underscoreToChangeCase(formatLabel),
            formatVersion.replaceAll("[.]", "_"),
            queryName);
    }

    protected XMLQuery createXMLQuery(C context, String queryName) {
        return createXMLQuery(getQueryFullName(context, queryName));
    }

    protected XMLQuery createXMLQuery(String queryName) {
        XMLQuery query = createXMLQuery();
        query.setQuery(getXMLQueryClasspathURL(queryName));
        return query;
    }

    protected URL getXMLQueryURL(C context, String queryName) {
        return getXMLQueryClasspathURL(getQueryFullName(context, queryName));
    }

    protected URL getXMLQueryClasspathURL(String queryName) {
        Resource resource = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + XML_QUERY_PATH + "/" + queryName + ".xml");
        if (!resource.exists())
            throw new SumarisTechnicalException(t("sumaris.extraction.xmlQuery.notFound", queryName));
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * Fill the context's pmfm infos (e.g. used to generate
     * @param context
     */
    protected List<ExtractionPmfmColumnVO> loadPmfmColumns(C context,
                                                           List<String> programLabels,
                                                           AcquisitionLevelEnum... acquisitionLevels) {

        if (CollectionUtils.isEmpty(programLabels)) return Collections.emptyList(); // no selected programs: skip

        // Create the map that holds the result
        Map<String, List<ExtractionPmfmColumnVO>> pmfmColumns = context.getPmfmsCacheMap();
        if (pmfmColumns == null) {
            pmfmColumns = Maps.newHashMap();
            context.setPmfmsCacheMap(pmfmColumns);
        }

        List<Integer> acquisitionLevelIds = Beans.getStream(acquisitionLevels)
            .map(acquisitionLevelEnum -> acquisitionLevelEnum.getId())
            .collect(Collectors.toList());

        String cacheKey = acquisitionLevelIds.toString();

        // Already loaded: use the cached values
        if (pmfmColumns.containsKey(cacheKey)) return pmfmColumns.get(cacheKey);

        if (log.isDebugEnabled()) {
            log.debug("Loading PMFM for {program: {}, acquisitionLevel: {}} ...",
                programLabels,
                cacheKey
            );
        }

        // Load pmfm columns
        List<ExtractionPmfmColumnVO> result = strategyService.findDenormalizedPmfmsByFilter(
                PmfmStrategyFilterVO.builder()
                    .programLabels(programLabels.toArray(new String[programLabels.size()]))
                    .acquisitionLevelIds(acquisitionLevelIds.toArray(new Integer[acquisitionLevelIds.size()]))
                    .build(),
                PmfmStrategyFetchOptions.builder().withCompleteName(false).build()
            )
                .stream()
                .map(pmfmStrategy -> toPmfmColumnVO(pmfmStrategy, null))

                // Group by pmfmId
                .collect(Collectors.groupingBy(ExtractionPmfmColumnVO::getPmfmId))
                .values().stream().map(list -> list.get(0))

                // Sort by label
                // TODO sort by rankOrder ?
                .sorted(Comparator.comparing(ExtractionPmfmColumnVO::getLabel, String::compareTo))
                .collect(Collectors.toList());

        // save result into the context map
        pmfmColumns.put(cacheKey, result);

        return result;
    }

    protected List<String> getTripProgramLabels(C context) {

        XMLQuery xmlQuery = createXMLQuery(context, "distinctTripProgram");
        xmlQuery.bind("tableName", context.getTripTableName());

        return query(xmlQuery.getSQLQueryAsString(), String.class);
    }

    private List<ExtractionPmfmColumnVO> toPmfmColumnVO(List<DenormalizedPmfmStrategyVO> pmfmStrategies) {

        Set<String> acquisitionLevels = Beans.collectDistinctProperties(pmfmStrategies, DenormalizedPmfmStrategyVO.Fields.ACQUISITION_LEVEL);
        // Create prefix map, by acquisition level (used to generate the pmfm alias)
        Map<String, String> aliasPrefixesByAcquisitionLevel = buildAcquisitionLevelPrefixes(acquisitionLevels);

        return pmfmStrategies.stream().map(source ->
                toPmfmColumnVO(source, aliasPrefixesByAcquisitionLevel.get(source.getAcquisitionLevel()))
            )
            .collect(Collectors.toList());
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
}
