package net.sumaris.extraction.core.dao.trip.pmfm;

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.pmfm.ExtractionPmfmTripContextVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionPmfmTripDao")
@Lazy
@Slf4j
public class ExtractionPmfmTripDaoImpl<C extends ExtractionPmfmTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements PmfmTripSpecification {

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ST_SHEET_NAME + "_%s";
    private static final String RL_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + RL_SHEET_NAME + "_%s";

    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.PMFM_TRIP);
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);


        List<String> programLabels = getTripProgramLabels(context);

        boolean hasSamples = programLabels.stream()
            .anyMatch(label -> this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_OPERATION_ENABLE_SAMPLE, Boolean.TRUE.toString()));

        if (hasSamples) {
            context.setSurvivalTestTableName(formatTableName(ST_TABLE_NAME_PATTERN, context.getId()));
            context.setReleaseTableName(formatTableName(RL_TABLE_NAME_PATTERN, context.getId()));

            // Stop here, if sheet already filled
            String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
            if (sheetName != null && context.hasSheet(sheetName)) return context;
            try {
                // Survival test table
                long rowCount = createSurvivalTestTable(context);
                if (rowCount == 0) return context;
                if (sheetName != null && context.hasSheet(sheetName)) return context;

                // Release table
                createReleaseTable(context);
            } catch (PersistenceException e) {
                // If error,clean created tables first, then rethrow the exception
                clean(context);
                throw e;
            }
        }

        context.setType(LiveExtractionTypeEnum.PMFM_TRIP);

        return context;
    }

    /* -- protected methods -- */
    @Override
    protected Class<? extends ExtractionRdbTripContextVO> getContextClass() {
        return ExtractionPmfmTripContextVO.class;
    }

    protected XMLQuery createTripQuery(C context) {

        XMLQuery xmlQuery = super.createTripQuery(context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

        // Get columns, BEFORE to add pmfms columns
        String groupbyColumns = String.join(",", xmlQuery.getAllColumnNames());

        // Add PMFM from program, if on program has been set
        String programLabel = context.getTripFilter().getProgramLabel();
        if (StringUtils.isNotBlank(programLabel)) {
            injectPmfmColumns(context, xmlQuery,
                    Collections.singletonList(programLabel),
                    AcquisitionLevelEnum.TRIP,
                    // Excluded PMFM (already exists as RDB format columns)
                    PmfmEnum.NB_OPERATION.getId()
                );
        }

        xmlQuery.bind("groupByColumns", groupbyColumns);

        return xmlQuery;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        List<String> programLabels = getTripProgramLabels(context);

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        // Bind groupBy columns
        Set<String> excludedColumns = ImmutableSet.of(RdbSpecification.COLUMN_GEAR_TYPE);
        Set<String> groupByColumns = xmlQuery.getColumnNames(e -> !xmlQuery.hasGroup(e, "agg")
            && !excludedColumns.contains(xmlQuery.getAttributeValue(e, "alias", true)));
        xmlQuery.bind("groupByColumns", String.join(",", groupByColumns));

        // Inject physical gear pmfms
        injectPmfmColumns(context, xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.PHYSICAL_GEAR,
            // Excluded Pmfms (already exists as RDB format columns)
            PmfmEnum.SMALLER_MESH_GAUGE_MM.getId(),
            PmfmEnum.SELECTIVITY_DEVICE.getId()
        );

        // Compute list of pmfms, depending of acquisition levels used
        List<ExtractionPmfmColumnVO> pmfmColumns;
        URL injectionQuery;
        boolean hasProgramAllowParent = programLabels.stream()
            .anyMatch(label -> this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_OPERATION_ALLOW_PARENT, Boolean.TRUE.toString()));
        if (!hasProgramAllowParent) {
            pmfmColumns = loadPmfmColumns(context, programLabels, AcquisitionLevelEnum.OPERATION);
            injectionQuery = getInjectionQueryByAcquisitionLevel(context, AcquisitionLevelEnum.OPERATION);
        }
        else {
            pmfmColumns = loadPmfmColumns(context, programLabels, AcquisitionLevelEnum.OPERATION, AcquisitionLevelEnum.CHILD_OPERATION);
            injectionQuery = getInjectionQueryByAcquisitionLevel(context, AcquisitionLevelEnum.CHILD_OPERATION);
        }

        injectPmfmColumns(context, xmlQuery,
            pmfmColumns,
            injectionQuery,
            null,
            // Excluded PMFM (already exists as RDB format columns)
            PmfmEnum.GEAR_DEPTH_M.getId(),
            PmfmEnum.BOTTOM_DEPTH_M.getId(),
            PmfmEnum.TRIP_PROGRESS.getId()
        );

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionStationTable"));
        xmlQuery.setGroup("allowParent", hasProgramAllowParent);

        return xmlQuery;
    }

    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = super.createRawSpeciesListQuery(context, excludeInvalidStation);

        xmlQuery.bind("groupByColumns", String.join(",", xmlQuery.getColumnNames(e ->
                true)));

        injectPmfmColumns(context, xmlQuery,
                getTripProgramLabels(context),
                AcquisitionLevelEnum.SORTING_BATCH,
                // Excluded PMFM (already exists as RDB format columns)
                PmfmEnum.BATCH_CALCULATED_WEIGHT.getId(),
                PmfmEnum.BATCH_MEASURED_WEIGHT.getId(),
                PmfmEnum.BATCH_ESTIMATED_WEIGHT.getId(),
                PmfmEnum.DISCARD_OR_LANDING.getId()
        );

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionRawSpeciesListTable"));
        return xmlQuery;
    }

    protected XMLQuery createSpeciesListQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesListQuery(context);

        String groupByColumns = injectPmfmColumns(context, xmlQuery,
                getTripProgramLabels(context),
                AcquisitionLevelEnum.SORTING_BATCH,
                "injectionSpeciesListPmfm",
                "afterSexInjection",
                PmfmEnum.BATCH_CALCULATED_WEIGHT.getId(),
                PmfmEnum.BATCH_MEASURED_WEIGHT.getId(),
                PmfmEnum.BATCH_ESTIMATED_WEIGHT.getId(),
                PmfmEnum.DISCARD_OR_LANDING.getId());

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesListTable"));

        xmlQuery.bind("groupByColumns", groupByColumns);
        xmlQuery.setGroup("addGroupBy", StringUtils.isNotBlank(groupByColumns));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // Special case for COST format:

        // - Hide sex columns, then replace by a new columns
        xmlQuery.setGroup("sex", false);
        xmlQuery.setGroup("lengthClass", false);
        xmlQuery.setGroup("numberAtLength", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));
        return xmlQuery;
    }

    protected long createSurvivalTestTable(C context) {

        log.debug(String.format("Extraction #%s > Creating survival tests table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createSurvivalTestTable");

        injectPmfmColumns(context, xmlQuery,
                getTripProgramLabels(context),
                AcquisitionLevelEnum.SAMPLE);

        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("survivalTestTableName", context.getSurvivalTestTableName());

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getSurvivalTestTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getSurvivalTestTableName(), context.getFilter(), ST_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getSurvivalTestTableName(),
                    ST_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > Survival test table: %s rows inserted", context.getId(), count));
        } else {
            context.addRawTableName(context.getSurvivalTestTableName());
        }
        return count;
    }

    protected long createReleaseTable(C context) {

        log.debug(String.format("Extraction #%s > Creating releases table...", context.getId()));
        XMLQuery xmlQuery = createXMLQuery(context, "createReleaseTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("releaseTableName", context.getReleaseTableName());

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getReleaseTableName());

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(context.getReleaseTableName(), context.getFilter(), RL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getReleaseTableName(),
                    RL_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > Release table: %s rows inserted", context.getId(), count));
        } else {
            context.addRawTableName(context.getReleaseTableName());
        }
        return count;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionTripPmfm":
            case "injectionPhysicalGearPmfm":
            case "injectionOperationPmfm":
            case "injectionParentOperationPmfm":
            case "injectionRawSpeciesListPmfm":
            case "injectionSpeciesListPmfm":
            case "injectionSurvivalTestPmfm":
            case "injectionTripTable":
            case "injectionStationTable":
            case "injectionRawSpeciesListTable":
            case "injectionSpeciesListTable":
            case "injectionSpeciesLengthTable":
            case "createReleaseTable":
            case "createSurvivalTestTable":
                return getQueryFullName(PmfmTripSpecification.FORMAT, PmfmTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected String injectPmfmColumns(C context,
                                       XMLQuery xmlQuery,
                                       List<String> programLabels,
                                       AcquisitionLevelEnum acquisitionLevel,
                                       Integer... excludedPmfmIds) {
        return injectPmfmColumns(context, xmlQuery, programLabels, acquisitionLevel, null, null, excludedPmfmIds);
    }


    protected String injectPmfmColumns(C context,
                                       XMLQuery xmlQuery,
                                       List<String> programLabels,
                                       AcquisitionLevelEnum acquisitionLevel,
                                       @Nullable String injectionQueryName,
                                       @Nullable String injectionPointName,
                                       Integer... excludedPmfmIds) {

        // Load PMFM columns to inject
        List<ExtractionPmfmColumnVO> pmfmColumns = loadPmfmColumns(context, programLabels, acquisitionLevel);

        if (CollectionUtils.isEmpty(pmfmColumns)) return ""; // Skip if empty

        // Compute the injection query
        URL injectionQuery = StringUtils.isBlank(injectionQueryName)
                ? getInjectionQueryByAcquisitionLevel(context, acquisitionLevel)
                : getXMLQueryURL(context, injectionQueryName);
        if (injectionQuery == null) {
            log.warn("No XML query found, for Pmfm injection on acquisition level: " + acquisitionLevel.name());
            return "";
        }

        return injectPmfmColumns(context, xmlQuery, pmfmColumns, injectionQuery, injectionPointName, excludedPmfmIds);
    }

    protected String injectPmfmColumns(C context,
                                       XMLQuery xmlQuery,
                                       List<ExtractionPmfmColumnVO> pmfmColumns,
                                       URL injectionQuery,
                                       @Nullable String injectionPointName,
                                       Integer... excludedPmfmIds) {

        if (CollectionUtils.isEmpty(pmfmColumns)) return ""; // Skip if empty

        List<Integer> excludedPmfmIdsList = Arrays.asList(excludedPmfmIds);

        pmfmColumns.stream()
            .filter(pmfm -> !excludedPmfmIdsList.contains(pmfm.getPmfmId()))
            .forEach(pmfm -> injectPmfmColumn(context, xmlQuery, injectionQuery, injectionPointName, pmfm));

        return pmfmColumns.stream().filter(pmfm -> !excludedPmfmIdsList.contains(pmfm.getPmfmId()))
            .map(ExtractionPmfmColumnVO::getLabel).collect(Collectors.joining(","));
    }

    protected URL getInjectionQueryByAcquisitionLevel(C context, AcquisitionLevelEnum acquisitionLevel) {
        switch (acquisitionLevel) {
            case TRIP:
                return getXMLQueryURL(context, "injectionTripPmfm");
            case OPERATION:
                return getXMLQueryURL(context, "injectionOperationPmfm");
            case CHILD_OPERATION:
                return getXMLQueryURL(context, "injectionParentOperationPmfm");
            case PHYSICAL_GEAR:
                return getXMLQueryURL(context, "injectionPhysicalGearPmfm");
            case SORTING_BATCH:
                return getXMLQueryURL(context, "injectionRawSpeciesListPmfm");
            case SAMPLE:
                return getXMLQueryURL(context, "injectionSurvivalTestPmfm");
            default:
                return null;
        }
    }

    protected void injectPmfmColumn(C context,
                                    XMLQuery xmlQuery,
                                    URL injectionPmfmQuery,
                                    @Nullable String injectionPointName,
                                    ExtractionPmfmColumnVO pmfm
    ) {
        // Have to be lower case due to postgres compatibility
        String pmfmAlias = this.databaseType == DatabaseType.postgresql ? pmfm.getAlias().toLowerCase() : pmfm.getAlias();
        String pmfmLabel = this.databaseType == DatabaseType.postgresql ? pmfm.getLabel().toLowerCase() : pmfm.getLabel();

        if (StringUtils.isBlank(injectionPointName)) {
            xmlQuery.injectQuery(injectionPmfmQuery, "%pmfmalias%", pmfmAlias);
        } else {
            xmlQuery.injectQuery(injectionPmfmQuery, "%pmfmalias%", pmfmAlias, injectionPointName);
        }

        xmlQuery.bind("pmfmId" + pmfmAlias, String.valueOf(pmfm.getPmfmId()));
        xmlQuery.bind("pmfmlabel" + pmfmAlias, pmfmLabel);

        // Disable groups of unused pmfm type
        for (PmfmValueType enumType : PmfmValueType.values()) {
            boolean active = enumType == pmfm.getType();
            xmlQuery.setGroup(enumType.name().toLowerCase() + pmfmAlias, active);
        }
    }
}
