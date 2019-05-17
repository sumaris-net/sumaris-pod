package net.sumaris.core.extraction.dao.trip.rdb;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.dao.trip.AggregationTripDao;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.live.ExtractionPmfmInfoVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripContextVO;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripVersion;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationRdbDao")
@Lazy
public class AggregationRdbDaoImpl<C extends ExtractionRdbTripContextVO> extends ExtractionBaseDaoImpl implements AggregationRdbDao<C>, AggregationTripDao {

    private static final Logger log = LoggerFactory.getLogger(AggregationRdbDaoImpl.class);

    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + TR_SHEET_NAME + "_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HH_SHEET_NAME + "_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + SL_SHEET_NAME + "_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + HL_SHEET_NAME + "_%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + CA_SHEET_NAME + "_%s";


    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected XMLQuery xmlQuery;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Override
    public <R extends C> R aggregate(C rawDataContext) {

        // Init context
        R context = createNewContext();
        context.setTripFilter(rawDataContext.getTripFilter());
        context.setFilter(rawDataContext.getFilter());
        context.setFormatName(RDB_FORMAT);
        context.setFormatVersion(ExtractionRdbTripVersion.VERSION_1_3.getLabel());
        context.setId(System.currentTimeMillis());

        // Compute table names
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(String.format(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSampleTableName(String.format(CA_TABLE_NAME_PATTERN, context.getId()));

        // Trip
        long rowCount;
        //rowCount = createTripTable(context);
        //if (rowCount == 0) throw new DataNotFoundException(t("sumaris.aggregation.noData"));

        // Station
        rowCount = createStationTable(rawDataContext, context);
        if (rowCount == 0) return context;

        // Species List
        //rowCount = createSpeciesListTable(context);
        //if (rowCount == 0) return context;

        // Species Length
        //createSpeciesLengthTable(context);

        return context;

    }


    /* -- protected methods -- */

    protected <R extends ExtractionRdbTripContextVO> R createNewContext() {
        Class<? extends ExtractionRdbTripContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.newInstance();
        } catch(Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected Class<? extends ExtractionRdbTripContextVO> getContextClass() {
        return ExtractionRdbTripContextVO.class;
    }

    protected List<ExtractionPmfmInfoVO> getPmfmInfos(ExtractionRdbTripContextVO context, MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId) {

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
        for (String acquisitionLevel: acquisitionLevels) {
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

    protected List<String> getTripProgramLabels(ExtractionRdbTripContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context,"distinctTripProgram");
        xmlQuery.bind("tableName", context.getTripTableName());

        return query(xmlQuery.getSQLQueryAsString(), String.class);
    }

    protected long createTripTable(ExtractionRdbTripContextVO context) {

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
            context.addTableName(context.getTripTableName(), TR_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Trip table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createTripQuery(ExtractionRdbTripContextVO context){
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

    protected long createStationTable(C rawDataContext, ExtractionRdbTripContextVO context) {

        XMLQuery xmlQuery = createStationQuery(rawDataContext, context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getStationTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getStationTableName(), context.getFilter(), HH_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getStationTableName(), HH_SHEET_NAME);
            log.debug(String.format("Station table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createStationQuery(C rawDataContext, ExtractionRdbTripContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");
        xmlQuery.bind("rawStationTableName", rawDataContext.getStationTableName());
        xmlQuery.bind("stationTableName", context.getStationTableName());

        return xmlQuery;
    }

    protected long createSpeciesListTable(ExtractionRdbTripContextVO context) {

        XMLQuery xmlQuery = createSpeciesListQuery(context, true/*exclude invalid station*/);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getSpeciesListTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getSpeciesListTableName(), context.getFilter(), SL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSpeciesListTableName(), SL_SHEET_NAME);
            log.debug(String.format("Species list table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createSpeciesListQuery(ExtractionRdbTripContextVO context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        // Bind some ids
        xmlQuery.bind("catchCategoryPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));

        // Exclude not valid station
        xmlQuery.setGroup("excludeInvalidStation", excludeInvalidStation);

        return xmlQuery;
    }


    protected long createSpeciesLengthTable(ExtractionRdbTripContextVO context) {

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
            context.addTableName(context.getSpeciesLengthTableName(), HL_SHEET_NAME);
            log.debug(String.format("Species length table: %s rows inserted", count));
        }
        return count;
    }

    protected XMLQuery createSpeciesLengthQuery(ExtractionRdbTripContextVO context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesLengthTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());
        xmlQuery.bind("speciesLengthTableName", context.getSpeciesLengthTableName());

        // Bind some ids
        xmlQuery.bind("sexPmfmId", String.valueOf(PmfmEnum.SEX.getId()));
        xmlQuery.bind("lengthTotalPmfmId", String.valueOf(PmfmEnum.LENGTH_TOTAL_CM.getId()));

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

    protected String getQueryFullName(ExtractionTripContextVO context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getFormatName());
        Preconditions.checkNotNull(context.getFormatVersion());

        return String.format("%s/v%s/aggregation/%s",
                context.getFormatName(),
                context.getFormatVersion().replaceAll("[.]", "_"),
                queryName);
    }

    protected XMLQuery createXMLQuery(ExtractionTripContextVO context, String queryName) {
        return createXMLQuery(getQueryFullName(context, queryName));
    }

    protected XMLQuery createXMLQuery(String queryName) {
        XMLQuery query = xmlQuery;
        query.setQuery(getXMLQueryClasspathURL(queryName));
        return query;
    }

    protected URL getXMLQueryURL(ExtractionTripContextVO context, String queryName) {
        return getXMLQueryClasspathURL(getQueryFullName(context, queryName));
    }

    protected URL getXMLQueryClasspathURL(String queryName) {
        Resource resource = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + XML_QUERY_PATH + "/" + queryName + ".xml");
        if (!resource.exists())
            throw new SumarisTechnicalException(t("sumaris.extraction.xmlQuery.notFound", queryName));
        try {
            return resource.getURL();
        }
        catch(IOException e) {
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
