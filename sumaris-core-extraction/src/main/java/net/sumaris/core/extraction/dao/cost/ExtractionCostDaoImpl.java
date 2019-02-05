package net.sumaris.core.extraction.dao.cost;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Dates;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.ExtractionDao;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("extractionCostDao")
public class ExtractionCostDaoImpl implements ExtractionCostDao {

    private static final Log LOG = LogFactory.getLog(ExtractionCostDaoImpl.class);

    private static final String XML_QUERY_PATH = "xmlQuery";

    private static final String TABLE_NAME_PREFIX = "EXT_";
    private static final String BASE_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "B%s";
    private static final String PMFM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "P%s_%s";
    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "TR%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "HH%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "SL%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "HL%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "CA%s";

    @Autowired
    SumarisConfiguration configuration;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    ExtractionDao extractionDao;

    @Autowired
    XMLQuery xmlQuery;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public ExtractionCostContextVO execute(TripFilterVO filter) {

        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Beginning extraction %s", filter == null ? "without filter" : "with filters:"));
            if (filter != null) {
                LOG.info(String.format("Program label: %s", filter.getProgramLabel()));
                LOG.info(String.format("  Location Id: %s", filter.getLocationId()));
                LOG.info(String.format("   Start date: %s", filter.getStartDate()));
                LOG.info(String.format("     End date: %s", filter.getEndDate()));
                LOG.info(String.format("    Vessel Id: %s", filter.getVesselId()));
                LOG.info(String.format("    RecDep Id: %s", filter.getRecorderDepartmentId()));
            }
        }

        ExtractionCostContextVO context = new ExtractionCostContextVO();
        context.setFilter(filter);
        context.setId(System.currentTimeMillis());
        context.setBaseTableName(String.format(BASE_TABLE_NAME_PATTERN, context.getId()));
        context.setTRTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));

        long nbRowsInserted = createBaseTable(context);

        LOG.debug(String.format("Base table: %s rows inserted", nbRowsInserted));
        if (nbRowsInserted == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));

        // get programs
        List<Integer> programIds = getProgramIds(context);
        LOG.debug("Detected program ids: " + programIds);
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(programIds));

        // Get pmfm strategies
        MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId = new ArrayListValuedHashMap<>();
        for (Integer programId : programIds) {
            pmfmStrategiesByProgramId.putAll(programId, strategyService.getPmfmStrategies(programId));
        }

        // Get pmfms
        buildPmfmInfos(context, pmfmStrategiesByProgramId);


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


    private void buildPmfmInfos(ExtractionCostContextVO context, MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId) {

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
                pmfmInfo.setTableName(String.format(PMFM_TABLE_NAME_PATTERN, context.getId(), pmfmInfo.getAlias()));
                pmfmInfos.add(pmfmInfo);
            }
        }

        context.setPmfmInfos(pmfmInfos);
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

    private List<Integer> getProgramIds(ExtractionCostContextVO context) {

        XMLQuery xmlQuery = createXMLQuery("distinctProgram");
        xmlQuery.bind("tableName", context.getBaseTableName());

        return extractionDao.query(xmlQuery.getSQLQueryAsString(), Integer.class);
    }

    private long createBaseTable(ExtractionCostContextVO context) {

        XMLQuery xmlQuery = createXMLQuery("cost/createBaseTable");
        xmlQuery.bind("baseTableName", context.getBaseTableName());

        // Bind usefull PmfmIds
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

        return countFrom(context.getBaseTableName());
    }

    private long createPmfmTables(ExtractionCostContextVO context) {

        long rowCount = 0;
        for (ExtractionPmfmInfoVO pmfmInfo: context.getPmfmInfos()) {

            XMLQuery xmlQuery = createXMLQuery("cost/createPmfmTable");
            xmlQuery.bind("pmfmTableName", pmfmInfo.getTableName());
            xmlQuery.bind("baseTableName", context.getBaseTableName());
            //xmlQuery.bind("parentId", pmfmInfo.isSurvey() ? "SURVEY_ID" : "SAMPLING_OPER_ID");
            xmlQuery.bind("pmfmId", String.valueOf(pmfmInfo.getPmfmId()));
            //xmlQuery.bind("isSurveyMeas", pmfmInfo.isSurvey() ? "1" : "0");
            //xmlQuery.bind("measIndivId", pmfmInfo.isIndividual() ? "NOT NULL" : "NULL");

            execute(xmlQuery);

            // Count inserted data
            long nbRowsInserted = countFrom(pmfmInfo.getTableName());
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("%s rows of pmfm raw data inserted into %s", nbRowsInserted, pmfmInfo.getTableName()));
            }
            rowCount += nbRowsInserted;

        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("%s rows of pmfm raw data inserted", rowCount));
        }

        return rowCount;
    }

    private int execute(XMLQuery xmlQuery) {
        return extractionDao.queryUpdate(xmlQuery.getSQLQueryAsString());
    }

    private long countFrom(String tableName) {
        XMLQuery xmlQuery = createXMLQuery("countFrom");
        xmlQuery.bind("tableName", tableName);
        return extractionDao.queryCount(xmlQuery.getSQLQueryAsString());
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
