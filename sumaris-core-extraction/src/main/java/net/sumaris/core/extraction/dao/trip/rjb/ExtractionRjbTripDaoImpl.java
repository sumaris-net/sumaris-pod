package net.sumaris.core.extraction.dao.trip.rjb;

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
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.CostSpecification;
import net.sumaris.core.extraction.specification.data.trip.RjbSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * Extraction for RJB (Pocheteaux). We use individual count, instead of weight
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionRjbTripDao")
@Lazy
@Slf4j
public class ExtractionRjbTripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements RjbSpecification {

    private static final String XML_QUERY_RJB_PATH = "rjb/v%s/%s";

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.RJB;
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        context.setFormat(LiveFormatEnum.RJB);

        return context;
    }

    /* -- protected methods -- */

    protected XMLQuery createTripQuery(C context) {

        XMLQuery xmlQuery = super.createTripQuery(context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

/*        // Bind some referential ids
        xmlQuery.bind("contractCodePmfmIds", Daos.getSqlInNumbers(
            PmfmEnum.CONTRACT_CODE.getId(),
            PmfmEnum.SELF_SAMPLING_PROGRAM.getId()
        ));
        // Bind some referential ids
        xmlQuery.bind("contractCode", "RJB"); // TODO externalize*/

        // TODO externalize
        xmlQuery.bind("taxonGroupLabels", Daos.getSqlInEscapedStrings("RJB_1", "RJB_2"));

        return xmlQuery;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        return xmlQuery;
    }

    @Override
    protected XMLQuery createRawSpeciesListQuery(C context, boolean excludeInvalidStation) {
        XMLQuery xmlQuery = super.createRawSpeciesListQuery(context, excludeInvalidStation);

        // Special case for RJB format:

        // - Hide weight columns, then replace by a new columns
        xmlQuery.setGroup("weight", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionRawSpeciesListTable"));

        // TODO externalize
        xmlQuery.bind("taxonGroupLabels", Daos.getSqlInEscapedStrings("RJB_1", "RJB_2"));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesListQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesListQuery(context);

        // Special case for RJB format:

        // - Hide weight columns, then replace by a new columns
        xmlQuery.setGroup("weight", false);
        xmlQuery.setGroup("lengthCode", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesListTable"), "beforeLengthCode");

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // Special case for RJB format:

        // - Hide sex columns, then replace by a new columns
        xmlQuery.setGroup("sex", false);
        xmlQuery.setGroup("lengthClass", false);
        xmlQuery.setGroup("numberAtLength", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        xmlQuery.bind("isDeadPmfmId", String.valueOf(PmfmEnum.IS_DEAD.getId()));

        return xmlQuery;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        String versionStr = VERSION_1_0.replaceAll("[.]", "_");
        switch (queryName) {
            case "injectionTripTable":
            case "injectionRawSpeciesListTable":
            case "injectionSpeciesListTable":
            case "injectionSpeciesLengthTable":
                return String.format(XML_QUERY_RJB_PATH, versionStr, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}
