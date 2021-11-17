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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.PmfmValueType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionPmfmTripDao")
@Lazy
@Slf4j
public class ExtractionPmfmTripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements PmfmTripSpecification {

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.PMFM_TRIP;
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        context.setFormat(LiveFormatEnum.PMFM_TRIP);

        return context;
    }

    /* -- protected methods -- */


    protected XMLQuery createTripQuery(C context) {

        XMLQuery xmlQuery = super.createTripQuery(context);

        // Add PMFM from program, if on program has been set
        String programLabel = context.getTripFilter().getProgramLabel();
        if (StringUtils.isNotBlank(programLabel)) {
            injectPmfmColumns(context, xmlQuery,
                    Collections.singletonList(programLabel),
                    AcquisitionLevelEnum.TRIP,
                    // Excluded PMFM (already exists as RDB format columns)
                    PmfmEnum.NB_OPERATION.getId());
        }

        return xmlQuery;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        // Inject Pmfm columns
        injectPmfmColumns(context, xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.OPERATION,
            // Excluded PMFM (already exists as RDB format columns)
            PmfmEnum.SMALLER_MESH_GAUGE_MM.getId(),
            PmfmEnum.GEAR_DEPTH_M.getId(),
            PmfmEnum.BOTTOM_DEPTH_M.getId(),
            PmfmEnum.SELECTIVITY_DEVICE.getId(),
            PmfmEnum.TRIP_PROGRESS.getId()
        );

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

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionTripPmfm":
            case "injectionOperationPmfm":
            case "injectionSpeciesLengthTable":
                return getQueryFullName(PmfmTripSpecification.FORMAT, PmfmTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }


    protected void injectPmfmColumns(C context,
                                     XMLQuery xmlQuery,
                                     List<String> programLabels,
                                     AcquisitionLevelEnum acquisitionLevel,
                                     Integer... excludedPmfmIds) {

        // Load PMFM columns to inject
        List<ExtractionPmfmColumnVO> pmfmColumns = loadPmfmColumns(context, programLabels, acquisitionLevel);

        if (CollectionUtils.isEmpty(pmfmColumns)) return; // Skip if empty

        // Compute the injection query
        URL injectionQuery = getInjectionQueryByAcquisitionLevel(context, acquisitionLevel);
        if (injectionQuery == null) {
            log.warn("No XML query found, for Pmfm injection on acquisition level: " + acquisitionLevel.name());
            return;
        }

        List<Integer> excludedPmfmIdsList = Arrays.asList(excludedPmfmIds);
        pmfmColumns.stream()
                .filter(pmfm -> !excludedPmfmIdsList.contains(pmfm.getPmfmId()))
                .forEach(pmfm -> injectPmfmColumn(context, xmlQuery, injectionQuery, pmfm));
    }

    protected URL getInjectionQueryByAcquisitionLevel(C context, AcquisitionLevelEnum acquisitionLevel) {
        switch (acquisitionLevel) {
            case TRIP:
                return getXMLQueryURL(context, "injectionTripPmfm");

            case OPERATION:
                return getXMLQueryURL(context, "injectionOperationPmfm");

            default:
                return null;
        }
    }

    protected void injectPmfmColumn(C context,
                                    XMLQuery xmlQuery,
                                    URL injectionPmfmQuery,
                                    ExtractionPmfmColumnVO pmfm
    ) {
        xmlQuery.injectQuery(injectionPmfmQuery, "%pmfmAlias%", pmfm.getAlias());

        xmlQuery.bind("pmfmId" + pmfm.getAlias(), String.valueOf(pmfm.getPmfmId()));
        xmlQuery.bind("pmfmLabel" + pmfm.getAlias(), pmfm.getLabel());

        // Disable groups of unused pmfm type
        for (PmfmValueType enumType: PmfmValueType.values()) {
            boolean active = enumType == pmfm.getType();
            xmlQuery.setGroup(enumType.name().toLowerCase() + pmfm.getAlias(), active);
        }
    }
}
