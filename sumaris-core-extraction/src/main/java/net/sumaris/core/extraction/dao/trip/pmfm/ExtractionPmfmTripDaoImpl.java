package net.sumaris.core.extraction.dao.trip.pmfm;

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
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.PmfmTripSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

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

    @Autowired
    protected StrategyService strategyService;

    private static final String XML_QUERY_PMFM_PATH = "pmfm/v%s/%s";
    private static final String XML_QUERY_PMFM_V1_0_PATH = String.format(XML_QUERY_PMFM_PATH,
            VERSION_1_0.replaceAll("[.]", "_"), "%s");

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        context.setFormat(LiveFormatEnum.PMFM_TRIP);

        return context;
    }

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.PMFM_TRIP;
    }

    /* -- protected methods -- */


    protected XMLQuery createTripQuery(C context) {

        XMLQuery xmlQuery = super.createTripQuery(context);

        // Hide already pmfm columns
        //xmlQuery.setGroup("xxx", false);
        String programLabel = context.getTripFilter().getProgramLabel();
        if (StringUtils.isNotBlank(programLabel)) {
            injectPmfmColumns(context, xmlQuery,
                    Collections.singletonList(programLabel),
                    AcquisitionLevelEnum.TRIP,
                    PmfmEnum.NB_OPERATION);
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
                PmfmEnum.NB_OPERATION);

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


        return xmlQuery;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionPmfm":
                return String.format(XML_QUERY_PMFM_V1_0_PATH, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }


    protected void injectPmfmColumns(C context,
                                     XMLQuery xmlQuery,
                                     List<String> programLabels,
                                     AcquisitionLevelEnum acquisitionLevel,
                                     PmfmEnum... excludedPmfms
    ) {
        fillPmfmInfos(context, programLabels, acquisitionLevel);
        if (CollectionUtils.isEmpty(context.getPmfmInfos())) return;

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionPmfm"));
        //xmlQuery.bind();

    }
}
