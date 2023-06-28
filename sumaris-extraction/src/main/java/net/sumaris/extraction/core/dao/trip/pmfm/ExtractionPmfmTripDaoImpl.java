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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.pmfm.ExtractionPmfmTripContextVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.xml.query.XMLQuery;
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

    public ExtractionPmfmTripDaoImpl() {
        super();
        // Hide RECORD_TYPE columns
        this.enableRecordTypeColumn = false;
    }

    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.PMFM_TRIP);
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        try {
            boolean enableSamples = this.isSamplesEnabled(context);

            if (enableSamples) {
                context.setSampleTableName(formatTableName(ST_TABLE_NAME_PATTERN, context.getId()));
                context.setReleaseTableName(formatTableName(RL_TABLE_NAME_PATTERN, context.getId()));

                // Stop here, if sheet already filled
                String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
                if (sheetName != null && context.hasSheet(sheetName)) return context;

                // Sample table
                long rowCount = createSampleTable(context);
                if (rowCount == 0) return context;
                if (sheetName != null && context.hasSheet(sheetName)) return context;

                // Release table
                createReleaseTable(context);
            }

            return context;
        }
        catch (PersistenceException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            throw e;
        }
        finally {
            context.setType(LiveExtractionTypeEnum.PMFM_TRIP);
        }
    }

    /* -- protected methods -- */

    protected boolean isSamplesEnabled(C context) {
        Set<String> programLabels = getTripProgramLabels(context);

        // Check if samples have been enabled:
        // - by program properties
        // - or by pmfm strategies
        return programLabels.stream()
            .anyMatch(label ->
                this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_OPERATION_ENABLE_SAMPLE, Boolean.TRUE.toString()))
            || programLabels.stream().anyMatch(label -> this.programService.hasAcquisitionLevelByLabel(label, AcquisitionLevelEnum.SAMPLE));
    }

    protected boolean enableParentOperation(C context) {
        Set<String> programLabels = getTripProgramLabels(context);
        return programLabels.stream()
            .anyMatch(label -> this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_OPERATION_ALLOW_PARENT, Boolean.TRUE.toString()));
    }

    protected boolean enableSpeciesListTaxon(C context) {
        Set<String> programLabels = getTripProgramLabels(context);

        return programLabels.stream()
            .anyMatch(label ->
                this.programService.hasPropertyValueByProgramLabel(label, ProgramPropertyEnum.TRIP_BATCH_TAXON_NAME_ENABLE, Boolean.TRUE.toString()));
    }

    protected boolean enableStationGearPmfms(C context) {
        return true;
    }



    @Override
    protected Class<? extends ExtractionRdbTripContextVO> getContextClass() {
        return ExtractionPmfmTripContextVO.class;
    }

    protected XMLQuery createTripQuery(C context) {

        XMLQuery xmlQuery = super.createTripQuery(context);

        xmlQuery.setGroup("departureDateTime", false);
        xmlQuery.setGroup("returnDateTime", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

        // Add PMFM from program, if on program has been set
        String programLabel = context.getTripFilter().getProgramLabel();
        if (StringUtils.isNotBlank(programLabel)) {
            injectPmfmColumns(context, xmlQuery,
                    Collections.singleton(programLabel),
                    AcquisitionLevelEnum.TRIP,
                    // Excluded PMFM (already exists as RDB format columns)
                    PmfmEnum.NB_OPERATION.getId()
                );
        }

        computeAndBindGroupBy(xmlQuery, GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        Set<String> programLabels = getTripProgramLabels(context);
        boolean enableGearPmfms = enableStationGearPmfms(context);
        boolean enableParentOperation = enableParentOperation(context);

        // Add some join, to keep only child operation, if parent/child are allowed (in program properties)
        // + Add operation and gear comments
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionStationTable"));

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        // Inject physical gear pmfms
        if (enableGearPmfms) {
            injectPmfmColumns(context, xmlQuery,
                programLabels,
                AcquisitionLevelEnum.PHYSICAL_GEAR,
                // Excluded Pmfms (already exists as RDB format columns)
                PmfmEnum.SMALLER_MESH_GAUGE_MM.getId(),
                PmfmEnum.SELECTIVITY_DEVICE.getId()
            );
        }

        // Compute list of pmfms, depending of acquisition levels used
        List<ExtractionPmfmColumnVO> pmfmColumns;
        URL injectionQuery;
        if (!enableParentOperation) {
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

        xmlQuery.setGroup("allowParent", enableParentOperation);

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = super.createRawSpeciesListQuery(context, excludeInvalidStation);

        injectPmfmColumns(context, xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.SORTING_BATCH,
            null,
            "pmfmsInjection",
            // Excluded PMFM (already exists as RDB format columns)
            getSpeciesListExcludedPmfmIds().toArray(new Integer[0])
        );

        boolean enableBatchDenormalization = enableBatchDenormalization(context);
        xmlQuery.injectQuery(getXMLQueryURL(context, enableBatchDenormalization ? "injectionRawSpeciesListDenormalizeTable" : "injectionRawSpeciesListTable"));

        // Enable taxon columns, if enable by program (e.g. in the SUMARiS program)
        boolean enableTaxonColumns = this.enableSpeciesListTaxon(context) || this.enableSpeciesLengthTaxon(context);
        xmlQuery.setGroup("taxon", enableTaxonColumns);

        // Enable individual count, if there is som taxon group no weight in program's options
        boolean hasTaxonGroupNoWeights = CollectionUtils.isNotEmpty(getTaxonGroupNoWeights(context));
        xmlQuery.setGroup("individualCount", hasTaxonGroupNoWeights);
        boolean excludeNoWeight = !hasTaxonGroupNoWeights;
        xmlQuery.setGroup("excludeNoWeight", excludeNoWeight);
        xmlQuery.setGroup("!excludeNoWeight", !excludeNoWeight);

        xmlQuery.setGroup("pmfmsJoin", !enableBatchDenormalization);

        boolean hasLandingOrDiscardPmfm = PmfmEnum.DISCARD_OR_LANDING.getId() != -1 &&
            loadPmfms(context, getTripProgramLabels(context), AcquisitionLevelEnum.SORTING_BATCH)
                .stream()
                .anyMatch(c -> c.getId() == PmfmEnum.DISCARD_OR_LANDING.getId());
        xmlQuery.setGroup("hasLandingOrDiscardPmfm", hasLandingOrDiscardPmfm);
        xmlQuery.setGroup("!hasLandingOrDiscardPmfm", !hasLandingOrDiscardPmfm);

        return xmlQuery;
    }

    protected XMLQuery createSpeciesListQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesListQuery(context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesListTable_afterSpecies"), "afterSpeciesInjection");
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesListTable_afterSex"), "afterSexInjection");

        String pmfmsColumns = injectPmfmColumns(context, xmlQuery,
                getTripProgramLabels(context),
                AcquisitionLevelEnum.SORTING_BATCH,
                "injectionSpeciesListPmfm",
                "afterSexInjection",
            // Excluded PMFM (already exists as RDB format columns)
            getSpeciesListExcludedPmfmIds().toArray(new Integer[0])
        );

        // Add group by pmfms
        xmlQuery.setGroup("pmfms", StringUtils.isNotBlank(pmfmsColumns));

        // Enable taxon columns, if enable by program (e.g. in the SUMARiS program)
        xmlQuery.setGroup("taxon", this.enableSpeciesListTaxon(context));

        // Enable individual count, if there is som taxon group no weight in program's options
        Set<String> taxonGroupNoWeights = getTaxonGroupNoWeights(context);
        xmlQuery.setGroup("individualCount", CollectionUtils.isNotEmpty(taxonGroupNoWeights));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // - Hide sex columns, then replace by a new columns
        xmlQuery.setGroup("sex", false);
        xmlQuery.setGroup("lengthClass", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"), "afterSexInjection");

        // Add pmfm columns
        String pmfmsColumns = injectPmfmColumns(context, xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.SORTING_BATCH_INDIVIDUAL,
            "injectionSpeciesLengthPmfm",
            "afterSexInjection",
            // Excluded some pmfms (already extracted in the RDB format)
            ImmutableList.builder()
                .add(PmfmEnum.DISCARD_OR_LANDING.getId(),
                    PmfmEnum.SEX.getId())
                .addAll(getSpeciesLengthPmfmIds()).build().toArray(Integer[]::new)
        );
        boolean hasPmfmsColumnsInjected = StringUtils.isNotBlank(pmfmsColumns);

        // Enable group, need by pmfms columns (if any)
        xmlQuery.setGroup("pmfms", hasPmfmsColumnsInjected);
        xmlQuery.setGroup("numberAtLength", !hasPmfmsColumnsInjected);

        // Enable taxon columns, if enable by program property (inherited from SL or directly from HL)
        boolean enableTaxonColumns = this.enableSpeciesListTaxon(context) || this.enableSpeciesLengthTaxon(context);
        if (enableTaxonColumns) {
            xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTaxon"), "afterSpeciesInjection");
        }
        xmlQuery.setGroup("taxon", enableTaxonColumns);

        return xmlQuery;
    }

    protected long createSampleTable(C context) {
        String tableName = context.getSampleTableName();

        XMLQuery xmlQuery = createSampleQuery(context);

        // execute insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), ST_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    ST_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > Sample table: %s rows inserted", context.getId(), count));
        } else {
            context.addRawTableName(tableName);
        }
        return count;
    }

    protected XMLQuery createSampleQuery(C context) {

        XMLQuery xmlQuery = createXMLQuery(context, "createSampleTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("sampleTableName", context.getSampleTableName());

        Set<String> programLabels = getTripProgramLabels(context);

        // Inject PMFM columns on SAMPLE
        List<ExtractionPmfmColumnVO> samplePmfms = loadPmfmColumns(context, programLabels, AcquisitionLevelEnum.SAMPLE);
        URL samplePmfmInjectionQuery = getInjectionQueryByAcquisitionLevel(context, AcquisitionLevelEnum.SAMPLE);
        injectPmfmColumns(context, xmlQuery, samplePmfms, samplePmfmInjectionQuery, null);

        // Inject PMFM columns on INDIVIDUAL MONITORING
        {
            Integer[] excludedPmfmIds = samplePmfms.stream().map(ExtractionPmfmColumnVO::getPmfmId).toArray(Integer[]::new);
            List<ExtractionPmfmColumnVO> individualMonitoringPmfms = loadPmfmColumns(context, programLabels, AcquisitionLevelEnum.INDIVIDUAL_MONITORING);
            URL individualMonitoringPmfmInjectionQuery = getInjectionQueryByAcquisitionLevel(context, AcquisitionLevelEnum.INDIVIDUAL_MONITORING);
            injectPmfmColumns(context, xmlQuery, individualMonitoringPmfms, individualMonitoringPmfmInjectionQuery, null,
                excludedPmfmIds // Excludes all samples ids, to avoid duplicated column names
            );
        }

        // Record type
        xmlQuery.setGroup("recordType", enableRecordTypeColumn);

        return xmlQuery;
    }

    protected long createReleaseTable(C context) {
        String tableName = context.getReleaseTableName();

        XMLQuery xmlQuery = createReleaseQuery(context);

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        // Clean row using generic tripFilter
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), RL_SHEET_NAME);
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(tableName,
                    RL_SHEET_NAME,
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Extraction #%s > Release table: %s rows inserted", context.getId(), count));
        } else {
            context.addRawTableName(tableName);
        }
        return count;
    }

    protected XMLQuery createReleaseQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createReleaseTable");
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("releaseTableName", context.getReleaseTableName());

        xmlQuery.setGroup("recordType", enableRecordTypeColumn);

        return xmlQuery;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionTripPmfm":
            case "injectionPhysicalGearPmfm":
            case "injectionStationPmfm":
            case "injectionStationParentPmfm":
            case "injectionRawSpeciesListPmfm":
            case "injectionSpeciesListPmfm":
            case "injectionSamplePmfm":
            case "injectionTripTable":
            case "injectionStationTable":
            case "injectionRawSpeciesListTable":
            case "injectionRawSpeciesListDenormalizeTable":
            case "injectionSpeciesListTable_afterSpecies":
            case "injectionSpeciesListTable_afterSex":
            case "injectionSpeciesLengthPmfm":
            case "injectionSpeciesLengthTable":
            case "injectionSpeciesLengthTaxon":
            case "createReleaseTable":
            case "createSampleTable":
                return getQueryFullName(PmfmTripSpecification.FORMAT, PmfmTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected String injectPmfmColumns(C context,
                                       XMLQuery xmlQuery,
                                       Set<String> programLabels,
                                       AcquisitionLevelEnum acquisitionLevel,
                                       Integer... excludedPmfmIds) {
        return injectPmfmColumns(context, xmlQuery, programLabels, acquisitionLevel, null, null, excludedPmfmIds);
    }


    protected String injectPmfmColumns(C context,
                                       XMLQuery xmlQuery,
                                       Set<String> programLabels,
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

        return pmfmColumns.stream()
            .filter(pmfm -> !excludedPmfmIdsList.contains(pmfm.getPmfmId()))
            .map(ExtractionPmfmColumnVO::getLabel)
            .collect(Collectors.joining(","));
    }

    protected URL getInjectionQueryByAcquisitionLevel(C context, AcquisitionLevelEnum acquisitionLevel) {
        switch (acquisitionLevel) {
            case TRIP:
                return getXMLQueryURL(context, "injectionTripPmfm");
            case OPERATION:
                return getXMLQueryURL(context, "injectionStationPmfm");
            case CHILD_OPERATION:
                return getXMLQueryURL(context, "injectionStationParentPmfm");
            case PHYSICAL_GEAR:
                return getXMLQueryURL(context, "injectionPhysicalGearPmfm");
            case SORTING_BATCH:
                return getXMLQueryURL(context, "injectionRawSpeciesListPmfm");
            case SAMPLE:
            case INDIVIDUAL_MONITORING:
                return getXMLQueryURL(context, "injectionSamplePmfm");
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

        if (StringUtils.isNotBlank(injectionPointName)) {
            xmlQuery.injectQuery(injectionPmfmQuery, "%pmfmalias%", pmfmAlias, injectionPointName);
        } else {
            xmlQuery.injectQuery(injectionPmfmQuery, "%pmfmalias%", pmfmAlias);
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
