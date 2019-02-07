package net.sumaris.core.extraction.dao.cost;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Dates;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.ExtractionUtilDao;
import net.sumaris.core.extraction.dao.table.ExtractionTableDao;
import net.sumaris.core.extraction.technical.XMLQuery;
import net.sumaris.core.extraction.vo.cost.ExtractionCostContextVO;
import net.sumaris.core.extraction.vo.cost.ExtractionPmfmInfoVO;
import net.sumaris.core.model.referential.pmfm.PmfmId;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

/**
 * @author peck7 on 17/12/2018.
 */
@Repository("extractionCostDao")
@Lazy
public class ExtractionCostDaoImpl implements ExtractionCostDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionCostDaoImpl.class);

    private static final String XML_QUERY_PATH = "xmlQuery";

    private static final String TABLE_NAME_PREFIX = "EXT_";
    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "TR%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "HH%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "SL%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "HL%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "CA%s";
    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "ST%s";

    @Autowired
    SumarisConfiguration configuration;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    ExtractionUtilDao utilDao;

    @Autowired
    XMLQuery xmlQuery;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public ExtractionCostContextVO execute(TripFilterVO filter) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Beginning extraction %s", filter == null ? "without filter" : "with filters:"));
            if (filter != null) {
                log.info(String.format("Program label: %s", filter.getProgramLabel()));
                log.info(String.format("  Location Id: %s", filter.getLocationId()));
                log.info(String.format("   Start date: %s", filter.getStartDate()));
                log.info(String.format("     End date: %s", filter.getEndDate()));
                log.info(String.format("    Vessel Id: %s", filter.getVesselId()));
                log.info(String.format("    RecDep Id: %s", filter.getRecorderDepartmentId()));
            }
        }

        // Init the extraction context
        ExtractionCostContextVO context = new ExtractionCostContextVO();
        context.setFilter(filter);
        context.setId(System.currentTimeMillis());
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(String.format(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSampleTableName(String.format(CA_TABLE_NAME_PATTERN, context.getId()));
        context.setSurvivalTestTableName(String.format(ST_TABLE_NAME_PATTERN, context.getId()));

        // Fill the trip table
        long rowCount = createTripTable(context);
        if (rowCount == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));
        log.debug(String.format("Trip table: %s rows inserted", rowCount));

        // Get programs
        List<Integer> programIds = getTripProgramIds(context);
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(programIds));
        log.debug("Detected program ids: " + programIds);

        // Get PMFMs, from program strategies
        MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId = new ArrayListValuedHashMap<>();
        for (Integer programId : programIds) {
            pmfmStrategiesByProgramId.putAll(programId, strategyService.getPmfmStrategies(programId));
        }
        List<ExtractionPmfmInfoVO> pmfmInfos = getPmfmInfos(context, pmfmStrategiesByProgramId);
        context.setPmfmInfos(pmfmInfos);

        // Create station table
        createStationTable(context);

        return context;

    }


    /**
     * Concat single quoted strings with ',' character, without parenthesis
     *
     * @param strings a {@link Collection} object.
     * @return concatenated strings
     */
    public static String getInStatementFromStringCollection(Collection<String> strings) {
        if (strings == null) return "";
        return Joiner.on(',').skipNulls().join(strings.stream().filter(Objects::nonNull).map(s -> "'" + s + "'").collect(Collectors.toSet()));
    }

    /**
     * Concat integers with ',' character, without parenthesis
     *
     * @param integers a {@link Collection} object.
     * @return concatenated integers
     */
    public static String getInStatementFromIntegerCollection(Collection<Integer> integers) {
        if (integers == null) return "";
        return Joiner.on(',').skipNulls().join(integers.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    /* -- private methods -- */


    private List<ExtractionPmfmInfoVO> getPmfmInfos(ExtractionCostContextVO context, MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId) {

        Map<String, String> acquisitionLevelAliases = buildAcquisitionLevelAliases(
                pmfmStrategiesByProgramId.values().stream().map(PmfmStrategyVO::getAcquisitionLevel).collect(Collectors.toSet()));


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

    private Map<String, String> buildAcquisitionLevelAliases(Set<String> acquisitionLevels) {
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

    private String buildAlias(String string, String separator) {
        StringBuilder result = new StringBuilder();
        Arrays.stream(string.split(separator)).forEach(part -> result.append(part, 0, 1));
        return result.toString();
    }

    private List<Integer> getTripProgramIds(ExtractionCostContextVO context) {

        XMLQuery xmlQuery = createXMLQuery("distinctProgram");
        xmlQuery.bind("tableName", context.getTripTableName());

        return utilDao.query(xmlQuery.getSQLQueryAsString(), Integer.class);
    }

    private long createTripTable(ExtractionCostContextVO context) {

        XMLQuery xmlQuery = createXMLQuery("cost/createTripTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());

        // Bind some PMFM ids
        xmlQuery.bind("nbOperationPmfmId", String.valueOf(PmfmId.NB_OPERATION.getId()));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Dates.formatDate(context.getStartDate(), "dd/MM/yyy"));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Dates.formatDate(context.getEndDate(), "dd/MM/yyy"));

        // Program filter
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", getInStatementFromStringCollection(context.getProgramLabels()));

        // Location Filter
        xmlQuery.setGroup("locationFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", getInStatementFromIntegerCollection(context.getLocationIds()));

        // Recorder Department filter
        xmlQuery.setGroup("departmentFilter", CollectionUtils.isNotEmpty(context.getRecorderDepartmentIds()));
        xmlQuery.bind("recDepIds", getInStatementFromIntegerCollection(context.getRecorderDepartmentIds()));

        // Vessel filter
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", getInStatementFromIntegerCollection(context.getVesselIds()));

        // execute insertion
        execute(xmlQuery);

        return countFrom(context.getTripTableName());
    }

    private long createStationTable(ExtractionCostContextVO context) {

        XMLQuery xmlQuery = createXMLQuery("cost/createStationTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("stationTableName", context.getStationTableName());

        // Bind some PMFM ids
        //xmlQuery.bind("fishingDepthPmfmId", String.valueOf(PmfmId.FISHING_DEPTH.getId()));
        xmlQuery.bind("mainWaterDepthPmfmId", String.valueOf(PmfmId.BOTTOM_DEPTH_M.getId()));


        // execute insertion
        execute(xmlQuery);

        return countFrom(context.getStationTableName());
    }

    private int execute(XMLQuery xmlQuery) {
        return utilDao.queryUpdate(xmlQuery.getSQLQueryAsString());
    }

    private long countFrom(String tableName) {
        XMLQuery xmlQuery = createXMLQuery("countFrom");
        xmlQuery.bind("tableName", tableName);
        return utilDao.queryCount(xmlQuery.getSQLQueryAsString());
    }

    private XMLQuery createXMLQuery(String queryName) {
        XMLQuery query = xmlQuery;
        query.setQuery(getXMLQueryFile(queryName));
        return query;
    }

    private URL getXMLQueryFile(String queryName) {
        URL fileURL = getClass().getClassLoader().getResource(XML_QUERY_PATH + "/" + queryName + ".xml");
        if (fileURL == null)
            throw new SumarisTechnicalException(t("sumaris.extraction.xmlQuery.notFound", queryName));
        return fileURL;
    }
}
