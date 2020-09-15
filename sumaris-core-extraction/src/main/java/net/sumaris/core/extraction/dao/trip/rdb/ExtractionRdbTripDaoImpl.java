package net.sumaris.core.extraction.dao.trip.rdb;

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
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionPmfmInfoVO;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripVersion;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.referential.pmfm.UnitEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;

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
public class ExtractionRdbTripDaoImpl<C extends ExtractionRdbTripContextVO> extends ExtractionBaseDaoImpl implements ExtractionRdbTripDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionRdbTripDaoImpl.class);

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + SL_SHEET_NAME + "_%s";
    private static final String SL_RAW_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "RAW_" + SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + CA_SHEET_NAME + "_%s";

    protected static final String XML_QUERY_PATH = "xmlQuery";

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public C execute(ExtractionFilterVO filter) {
        ExtractionTripFilterVO tripFilter = toTripFilterVO(filter);

        // Init context
        C context = createNewContext();
        context.setTripFilter(tripFilter);
        context.setFilter(filter);
        context.setFormatName(RDB_FORMAT);
        context.setFormatVersion(ExtractionRdbTripVersion.VERSION_1_3.getLabel());
        context.setId(System.currentTimeMillis());

        if (log.isInfoEnabled()) {
            StringBuilder filterInfo = new StringBuilder();
            if (filter != null) {
                filterInfo.append("with filter:").append(tripFilter.toString("\n - "));
            }
            else {
                filterInfo.append("(without filter)");
            }
            log.info(String.format("Starting extraction #%s (raw data / trips)... %s", context.getFormatName(), context.getId(), filterInfo.toString()));
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --

        try {
            // Trip
            long rowCount = createTripTable(context);
            if (rowCount == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Get programs from trips
            List<String> programLabels = getTripProgramLabels(context);
            Preconditions.checkArgument(CollectionUtils.isNotEmpty(programLabels));
            log.debug("Detected programs: " + programLabels);

            // Get PMFMs from strategies
            final MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId = new ArrayListValuedHashMap<>();
            programLabels.stream()
                    .map(programService::getByLabel)
                    .map(ProgramVO::getId)
                    .forEach(programId -> {
                        Collection<PmfmStrategyVO> pmfms = strategyService.findPmfmStrategiesByProgram(programId, true);
                        pmfmStrategiesByProgramId.putAll(programId, pmfms);
                    });
            List<ExtractionPmfmInfoVO> pmfmInfos = getPmfmInfos(context, pmfmStrategiesByProgramId);
            context.setPmfmInfos(pmfmInfos);

            // Station
            rowCount = createStationTable(context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Species List
            rowCount = createSpeciesListTable(context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Species Length
            createSpeciesLengthTable(context);

            return context;
        }
        catch (PersistenceException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            throw e;
        }
    }

    @Override
    public void clean(ExtractionRdbTripContextVO context) {
        Set<String> tableNames = ImmutableSet.<String>builder()
                .addAll(context.getTableNames())
                .addAll(context.getRawTableNames())
                .build();

        if (CollectionUtils.isEmpty(tableNames)) return;

        tableNames.stream()
            // Keep only tables with EXT_ prefix
            .filter(tableName -> tableName != null && tableName.startsWith("EXT_"))
            .forEach(tableName -> {
                try {
                    extractionTableDao.dropTable(tableName);
                }
                catch (SumarisTechnicalException e) {
                    log.error(e.getMessage());
                    // Continue
                }
            });
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
        if (context.getTripTableName() == null) context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        if (context.getStationTableName() == null) context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        if (context.getRawSpeciesListTableName() == null) context.setRawSpeciesListTableName(String.format(SL_RAW_TABLE_NAME_PATTERN, context.getId()));
        if (context.getSpeciesListTableName() == null) context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));
        if (context.getSpeciesLengthTableName() == null) context.setSpeciesLengthTableName(String.format(HL_TABLE_NAME_PATTERN, context.getId()));
        if (context.getSampleTableName() == null) context.setSampleTableName(String.format(CA_TABLE_NAME_PATTERN, context.getId()));
    }

    protected List<ExtractionPmfmInfoVO> getPmfmInfos(C context, MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId) {

        Map<String, String> acquisitionLevelAliases = buildAcquisitionLevelAliases(
                pmfmStrategiesByProgramId.values().stream()
                        .map(PmfmStrategyVO::getAcquisitionLevel)
                        .collect(Collectors.toSet()));


        List<ExtractionPmfmInfoVO> pmfmInfos = new ArrayList<>();
        for (Integer programId : pmfmStrategiesByProgramId.keySet()) {
            for (PmfmStrategyVO pmfmStrategy : pmfmStrategiesByProgramId.get(programId)) {
                ExtractionPmfmInfoVO pmfmInfo = new ExtractionPmfmInfoVO();
                pmfmInfo.setProgramId(programId);
                pmfmInfo.setAcquisitionLevel(pmfmStrategy.getAcquisitionLevel());
                pmfmInfo.setPmfmId(pmfmStrategy.getPmfmId());
                pmfmInfo.setRankOrder(pmfmStrategy.getRankOrder());

                pmfmInfo.setAlias(acquisitionLevelAliases.get(pmfmInfo.getAcquisitionLevel()) + pmfmInfo.getPmfmId());
                //pmfmInfo.setTableName(String.format(PMFM_TABLE_NAME_PATTERN, context.getId(), pmfmInfo.getAlias()));
                pmfmInfos.add(pmfmInfo);
            }
        }

        return pmfmInfos;
    }

    protected Map<String, String> buildAcquisitionLevelAliases(Set<String> acquisitionLevels) {
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

    protected List<String> getTripProgramLabels(C context) {

        XMLQuery xmlQuery = createXMLQuery(context, "distinctTripProgram");
        xmlQuery.bind("tableName", context.getTripTableName());

        return query(xmlQuery.getSQLQueryAsString(), String.class);
    }

    protected long createTripTable(C context) {

        XMLQuery xmlQuery = createTripQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getTripTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getTripTableName(), context.getFilter(), TR_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getTripTableName(),
                    TR_SHEET_NAME,
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
        Integer countryLocationLevelId = getReferentialIdByUniqueLabel(LocationLevel.class, LocationLevelEnum.COUNTRY.getLabel());
        xmlQuery.bind("countryLocationLevelId", String.valueOf(countryLocationLevelId));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program tripFilter
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getSqlInValueFromStringCollection(context.getProgramLabels()));

        // Location Filter
        xmlQuery.setGroup("locationFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getSqlInValueFromIntegerCollection(context.getLocationIds()));

        // Recorder Department tripFilter
        xmlQuery.setGroup("departmentFilter", CollectionUtils.isNotEmpty(context.getRecorderDepartmentIds()));
        xmlQuery.bind("recDepIds", Daos.getSqlInValueFromIntegerCollection(context.getRecorderDepartmentIds()));

        // Vessel tripFilter
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getSqlInValueFromIntegerCollection(context.getVesselIds()));

        return xmlQuery;
    }

    protected long createStationTable(C context) {

        XMLQuery xmlQuery = createStationQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getStationTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getStationTableName(), context.getFilter(), HH_SHEET_NAME);
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(context.getStationTableName(),
                    HH_SHEET_NAME,
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

        return xmlQuery;
    }

    protected long createSpeciesListTable(C context) {

        // Create raw table (with hidden columns used by sub table - e.g. SAMPLE_ID)
        XMLQuery rawXmlQuery = createRawSpeciesListQuery(context, true/*exclude invalid station*/);
        execute(rawXmlQuery);

        // Add the raw table
        context.addRawTableName(context.getRawSpeciesListTableName());

        // Clean row using generic filter
        cleanRow(context.getRawSpeciesListTableName(), context.getFilter(), SL_SHEET_NAME);


        // Create the final table (with distinct), without hidden columns
        String tableName = context.getSpeciesListTableName();
        XMLQuery xmlQuery = createSpeciesListQuery(context);
        execute(xmlQuery);

        long count = countFrom(tableName);

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    SL_SHEET_NAME,
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

    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = createXMLQuery(context, "createRawSpeciesListTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());

        // Bind some ids
        xmlQuery.bind("catchCategoryPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));

        // Exclude not valid station
        xmlQuery.setGroup("excludeInvalidStation", excludeInvalidStation);

        return xmlQuery;
    }

    protected XMLQuery createSpeciesListQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");
        xmlQuery.bind("rawSpeciesListTableName", context.getRawSpeciesListTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        return xmlQuery;
    }

    protected long createSpeciesLengthTable(C context) {

        XMLQuery xmlQuery = createSpeciesLengthQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getSpeciesLengthTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getSpeciesLengthTableName(), context.getFilter(), HL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSpeciesLengthTableName(),
                    HL_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Species length table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getSpeciesLengthTableName());
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

        return xmlQuery;
    }

    protected int execute(XMLQuery xmlQuery) {
        return queryUpdate(xmlQuery.getSQLQueryAsString());
    }

    protected long countFrom(String tableName) {
        XMLQuery xmlQuery = createXMLQuery("countFrom");
        xmlQuery.bind("tableName", tableName);
        return queryCount(xmlQuery.getSQLQueryAsString());
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatName());
        Preconditions.checkNotNull(context.getFormatVersion());

        return String.format("%s/v%s/%s",
                context.getFormatName(),
                context.getFormatVersion().replaceAll("[.]", "_"),
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

    protected int cleanRow(String tableName, ExtractionFilterVO filter, String sheetName) {
        Preconditions.checkNotNull(tableName);
        if (filter == null) return 0;

        SumarisTableMetadata table = databaseMetadata.getTable(tableName.toLowerCase());
        Preconditions.checkNotNull(table);

        String whereClauseContent = SumarisTableMetadatas.getSqlWhereClauseContent(table, filter, sheetName, table.getAlias());
        if (StringUtils.isBlank(whereClauseContent)) return 0;

        String deleteQuery = table.getDeleteQuery(String.format("NOT(%s)", whereClauseContent));
        return queryUpdate(deleteQuery);
    }


}
