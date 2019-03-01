package net.sumaris.core.extraction.dao.trip.ices;

import com.google.common.base.Preconditions;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFormat;
import net.sumaris.core.util.Dates;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.technical.Daos;
import net.sumaris.core.extraction.technical.XMLQuery;
import net.sumaris.core.extraction.vo.trip.ices.ExtractionIcesContextVO;
import net.sumaris.core.extraction.vo.trip.ExtractionPmfmInfoVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripContextVO;
import net.sumaris.core.extraction.vo.trip.ices.ExtractionIcesVersion;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.PmfmStrategyVO;
import net.sumaris.core.vo.filter.TripFilterVO;
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

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionIcesDao")
@Lazy
public class ExtractionIcesDaoImpl<C extends ExtractionIcesContextVO> extends ExtractionBaseDaoImpl implements ExtractionIcesDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionIcesDaoImpl.class);

    public static final String FORMAT = StringUtils.underscoreToChangeCase(ExtractionTripFormat.ICES.name());
    private static final String TR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "TR_%s";
    private static final String HH_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "HH_%s";
    private static final String SL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "SL_%s";
    private static final String HL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "HL_%s";
    private static final String CA_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + "CA_%s";

    protected static final String XML_QUERY_PATH = "xmlQuery";

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected XMLQuery xmlQuery;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Override
    public C execute(ExtractionTripFilterVO filter) {

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

        // Init context
        C context = createNewContext();
        context.setFilter(filter);
        context.setFormatName(FORMAT);
        context.setFormatVersion(ExtractionIcesVersion.VERSION_1_3.getLabel());
        context.setId(System.currentTimeMillis());
        context.setTripTableName(String.format(TR_TABLE_NAME_PATTERN, context.getId()));
        context.setStationTableName(String.format(HH_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesListTableName(String.format(SL_TABLE_NAME_PATTERN, context.getId()));
        context.setSpeciesLengthTableName(String.format(HL_TABLE_NAME_PATTERN, context.getId()));
        context.setSampleTableName(String.format(CA_TABLE_NAME_PATTERN, context.getId()));

        // Trip
        long rowCount = createTripTable(context);
        if (rowCount == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));
        log.debug(String.format("Trip table: %s rows inserted", rowCount));

        // Stop here
        if (filter != null && filter.isFirstTableOnly()) return context;

        // Get programs from trips
        List<Integer> programIds = getTripProgramIds(context);
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(programIds));
        log.debug("Detected program ids: " + programIds);

        // Get PMFMs from strategies
        MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId = new ArrayListValuedHashMap<>();
        for (Integer programId : programIds) {
            pmfmStrategiesByProgramId.putAll(programId, strategyService.getPmfmStrategies(programId));
        }
        List<ExtractionPmfmInfoVO> pmfmInfos = getPmfmInfos(context, pmfmStrategiesByProgramId);
        context.setPmfmInfos(pmfmInfos);

        // Station
        createStationTable(context);

        // Species List
        createSpeciesListTable(context);

        // Species Length
        createSpeciesLengthTable(context);

        return context;

    }


    /* -- protected methods -- */

    protected <R extends ExtractionIcesContextVO> R createNewContext() {
        Class<? extends ExtractionIcesContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.newInstance();
        } catch(Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected Class<? extends ExtractionIcesContextVO> getContextClass() {
        return ExtractionIcesContextVO.class;
    }

    protected List<ExtractionPmfmInfoVO> getPmfmInfos(ExtractionIcesContextVO context, MultiValuedMap<Integer, PmfmStrategyVO> pmfmStrategiesByProgramId) {

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

    protected List<Integer> getTripProgramIds(ExtractionIcesContextVO context) {

        XMLQuery xmlQuery = createXMLQuery("distinctProgram");
        xmlQuery.bind("tableName", context.getTripTableName());

        return query(xmlQuery.getSQLQueryAsString(), Integer.class);
    }

    protected long createTripTable(ExtractionIcesContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createTripTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());

        // Bind some referential ids
        xmlQuery.bind("nbOperationPmfmId", String.valueOf(PmfmEnum.NB_OPERATION.getId()));
        xmlQuery.bind("countryLocationLevelId", String.valueOf(getReferentialIdByUniqueLabel(LocationLevel.class, LocationLevelEnum.COUNTRY.getLabel())));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Dates.formatDate(context.getStartDate(), "dd/MM/yyy"));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Dates.formatDate(context.getEndDate(), "dd/MM/yyy"));

        // Program filter
        xmlQuery.setGroup("programFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("progLabels", Daos.getInStatementFromStringCollection(context.getProgramLabels()));

        // Location Filter
        xmlQuery.setGroup("locationFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getInStatementFromIntegerCollection(context.getLocationIds()));

        // Recorder Department filter
        xmlQuery.setGroup("departmentFilter", CollectionUtils.isNotEmpty(context.getRecorderDepartmentIds()));
        xmlQuery.bind("recDepIds", Daos.getInStatementFromIntegerCollection(context.getRecorderDepartmentIds()));

        // Vessel filter
        xmlQuery.setGroup("vesselFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("vesselIds", Daos.getInStatementFromIntegerCollection(context.getVesselIds()));

        // TODO bind sort, offset, etc ?

        // execute insertion
        execute(xmlQuery);

        long count = countFrom(context.getTripTableName());
        if (count > 0) {
            context.addTableName(context.getTripTableName(), "TR");
        }
        return count;
    }

    protected long createStationTable(ExtractionIcesContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createStationTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("stationTableName", context.getStationTableName());

        // Bind some PMFM ids
        xmlQuery.bind("meshSizePmfmId", String.valueOf(PmfmEnum.SMALLER_MESH_GAUGE_MM.getId()));
        xmlQuery.bind("mainFishingDepthPmfmId", String.valueOf(PmfmEnum.GEAR_DEPTH_M.getId()));
        xmlQuery.bind("mainWaterDepthPmfmId", String.valueOf(PmfmEnum.BOTTOM_DEPTH_M.getId()));


        // execute insertion
        execute(xmlQuery);

        long count = countFrom(context.getStationTableName());
        if (count > 0) {
            context.addTableName(context.getStationTableName(), "HH");
        }
        return count;
    }

    protected long createSpeciesListTable(ExtractionIcesContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesListTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesListTableName", context.getSpeciesListTableName());

        // Bind some ids
        xmlQuery.bind("landingPmfmId", String.valueOf(PmfmEnum.DISCARD_OR_LANDING.getId()));

        // execute insertion
        execute(xmlQuery);


        long count = countFrom(context.getSpeciesListTableName());
        if (count > 0) {
            context.addTableName(context.getSpeciesListTableName(), "SL");
        }
        return count;
    }

    protected long createSpeciesLengthTable(ExtractionIcesContextVO context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createSpeciesLengthTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("speciesLengthTableName", context.getSpeciesLengthTableName());

        // execute insertion
        execute(xmlQuery);

        long count = countFrom(context.getSpeciesLengthTableName());
        if (count > 0) {
            context.addTableName(context.getSpeciesLengthTableName(), "HL");
        }
        return count;
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

        return String.format("%s/v%s/%s",
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
}
