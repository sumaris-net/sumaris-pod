package net.sumaris.extraction.core.dao.trip.apase;

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
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.extraction.core.dao.trip.pmfm.ExtractionPmfmTripDaoImpl;
import net.sumaris.extraction.core.specification.data.trip.ApaseSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.apase.ExtractionApaseContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Extraction for the APASE project (trawl selectivity)
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 */
@Repository("extractionApaseDao")
@Lazy
@Slf4j
public class ExtractionApaseDaoImpl<C extends ExtractionApaseContextVO, F extends ExtractionFilterVO>
        extends ExtractionPmfmTripDaoImpl<C, F>
        implements ApaseSpecification {

    private static final String FG_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + FG_SHEET_NAME + "_%s";
    private static final String CT_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + CT_SHEET_NAME + "_%s";

    public ExtractionApaseDaoImpl() {
        super();
        this.enableRecordTypeColumn = false; // No RECORD_TYPE in this format
    }

    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.APASE);
    }


    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        try {
            // Stop here, if sheet already filled
            String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Physical gear
            long rowCount = createGearTable(context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Catch
            rowCount = createCatchTable(context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            return context;
        }
        finally {
            context.setType(LiveExtractionTypeEnum.APASE);
        }
    }

    @Override
    protected void fillContextTableNames(C context) {
        super.fillContextTableNames(context);

        // Set unique table names
        context.setGearTableName(formatTableName(FG_TABLE_NAME_PATTERN, context.getId()));
        context.setCatchTableName(formatTableName(CT_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet names
        context.setGearSheetName(ApaseSpecification.FG_SHEET_NAME);
        context.setCatchSheetName(ApaseSpecification.CT_SHEET_NAME);

        // Always enable batch denormalization
        context.setEnableBatchDenormalization(true);

        context.addColumnNameReplacement(PmfmEnum.CHILD_GEAR.getLabel(), ApaseSpecification.COLUMN_SUB_GEAR_IDENTIFIER)
            .addColumnNameReplacement(PmfmEnum.BATCH_GEAR_POSITION.getLabel(), ApaseSpecification.COLUMN_SUB_GEAR_POSITION)
            // Rename 'SELECTIVITY_DEVICE_APASE' into 'SELECTION_DEVICE'
            .addColumnNameReplacement(PmfmEnum.SELECTIVITY_DEVICE_APASE.getLabel(), ApaseSpecification.COLUMN_SELECTION_DEVICE)
            .addColumnNameReplacement(PmfmEnum.HEADLINE_CUMULATIVE_LENGTH.getLabel(), PmfmEnum.HEADLINE_LENGTH.getLabel())
        ;

    }

    protected long createGearTable(C context) {

        XMLQuery xmlQuery = createGearQuery(context);

        // execute insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getGearTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getGearTableName(), context.getFilter(), context.getGearSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(context.getGearTableName(),
                context.getGearSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());
            log.debug(String.format("Gear table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getGearTableName());
        }


        return count;
    }

    protected long createCatchTable(C context) {

        XMLQuery xmlQuery = createCatchQuery(context);

        // execute insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getCatchTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getCatchTableName(), context.getFilter(), context.getCatchSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(context.getCatchTableName(),
                context.getCatchSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());
            log.debug(String.format("Catch table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getCatchTableName());
        }


        return count;
    }

    /* -- protected methods -- */

    @Override
    protected boolean isSamplesEnabled(C context) {
        return false;
    }

    protected boolean enableParentOperation(C context) {
        return false;
    }

    protected boolean enableSpeciesListTaxon(C context) {
        return false;
    }

    @Override
    protected boolean enableStationGearPmfms(C context) {
        return false;
    }

    @Override
    protected Class<? extends ExtractionApaseContextVO> getContextClass() {
        return ExtractionApaseContextVO.class;
    }

    @Override
    protected DenormalizedBatchOptions createDenormalizedBatchOptions(String programLabel) {
        DenormalizedBatchOptions options = super.createDenormalizedBatchOptions(programLabel);
        options.setEnableRtpWeight(false);
        options.setEnableAliveWeight(false);
        return options;
    }

    @Override
    protected XMLQuery createTripQuery(C context) {
        XMLQuery xmlQuery = super.createTripQuery(context);
        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripVessel"), "afterVesselInjection");
        return xmlQuery;
    }

    @Override
    protected XMLQuery createStationQuery(C context) {
        XMLQuery query = super.createStationQuery(context);

        // Disable some gear columns (will be on PG table)
        query.setGroup("gearComments", false);
        query.setGroup("selectionDevice", false);

        return query;
    }

    protected XMLQuery createGearQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createGearTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("gearTableName", context.getGearTableName());

        // Inject physical gear pmfms
        injectPmfmColumns(context,
            xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.PHYSICAL_GEAR,
            "injectionPhysicalGearPmfm",
            "pmfmInjection"
        );

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected XMLQuery createCatchQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createCatchTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("stationTableName", context.getStationTableName());
        xmlQuery.bind("catchTableName", context.getCatchTableName());

        // Inject pmfms
        injectPmfmColumns(context,
            xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.SORTING_BATCH,
            null,
            "pmfmsInjection",
            getCatchExcludedPmfmIds().toArray(new Integer[0])
        );

        //xmlQuery.bind("subsamplingCategoryPmfmId", PmfmEnum.BATCH_SORTING.getId().toString());
        xmlQuery.bind("catchWeightPmfmId", PmfmEnum.CATCH_WEIGHT.getId().toString());
        xmlQuery.bind("batchGearPositionPmfmId", PmfmEnum.BATCH_GEAR_POSITION.getId().toString());

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);


        return xmlQuery;
    }

    @Override
    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = super.createRawSpeciesListQuery(context, excludeInvalidStation);
        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);
        // Always disable pmfms (should only keep the length pmfms, in LENGTH_CLASS column)
        xmlQuery.setGroup("pmfms", false);
        xmlQuery.setGroup("numberAtLength", true);
        return xmlQuery;

    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionTripVessel":
            case "createGearTable":
            case "createCatchTable":
            case "injectionStationTable":
            case "createRawSpeciesListDenormalizeTable":
            case "injectionRawSpeciesListPmfm":
            case "injectionPhysicalGearPmfm":
            case "injectionRawSpeciesListTable":
            case "injectionSpeciesLengthTable":
            case "injectionSpeciesLengthPmfm":
                return getQueryFullName(ApaseSpecification.FORMAT, ApaseSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

    protected List<Integer> getCatchExcludedPmfmIds() {
        return ImmutableList.<Integer>builder()
            .addAll(super.getSpeciesListExcludedPmfmIds())
            .add(
                PmfmEnum.CATCH_WEIGHT.getId(),
                PmfmEnum.DISCARD_WEIGHT.getId()
            )
            .build();
    }
    @Override
    protected List<Integer> getSpeciesListExcludedPmfmIds() {
        return ImmutableList.<Integer>builder()
            .addAll(super.getSpeciesListExcludedPmfmIds())
            .add(
                // Already in the catch table
                PmfmEnum.CATCH_WEIGHT.getId(),
                PmfmEnum.DISCARD_WEIGHT.getId()
            )
            .build();
    }

    @Override
    protected List<Integer> getSizeCategoryPmfmIds() {
        return ImmutableList.of(
            PmfmEnum.TRAWL_SIZE_CAT.getId()
        );
    }

    @Override
    protected List<Integer> getSelectivityDevicePmfmIds() {
        return ImmutableList.<Integer>builder()
            .add(
                PmfmEnum.SELECTIVITY_DEVICE.getId(),
                PmfmEnum.SELECTIVITY_DEVICE_APASE.getId()
            )
            .build();
    }
}
