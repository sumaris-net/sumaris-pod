/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.extraction.core.dao.trip.free;

import com.google.common.base.Preconditions;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.Free1Specification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionFree1TripDao")
@Lazy
public class ExtractionFree1TripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements Free1Specification {

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.FREE1;
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        // Override some context properties
        context.setFormat(LiveFormatEnum.FREE1);

        return context;
    }

    /* -- protected methods -- */

    @Override
    protected XMLQuery createTripQuery(C context) {
        XMLQuery xmlQuery = super.createTripQuery(context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

        return xmlQuery;
    }

    @Override
    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        // Hide GearType (not in the FREE format)
        xmlQuery.setGroup("gearType", false);

        // - Hide some columns (replaced by a new columns names)
        xmlQuery.setGroup("date", false);
        xmlQuery.setGroup("time", false);
        xmlQuery.setGroup("fishingTime", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionStationTable"));

        // Bind some PMFM ids
        xmlQuery.bind("effortPmfmIds", Daos.getSqlInNumbers(
            PmfmEnum.HEADLINE_CUMULATIVE_LENGTH.getId(),
            PmfmEnum.BEAM_CUMULATIVE_LENGTH.getId(),
            PmfmEnum.NET_LENGTH.getId()));
        // TODO: add SIH Ifremer missing parameters (see FREE1 format specification) ?
        //        TOTAL_LENGTH_HAULED Longueur levée,               = NET_LENGTH ?
        //        TOTAL_NB_HOOKS Nombre total d'hameçons,           missing in SUMARIS
        //        NB_FISH_POT Nombre de casiers nasses ou poches,   missing in SUMARIS
        //        HEADLINE_LENGTH Longueur de la corde de dos (cumulée si jumeaux),   = HEADLINE_CUMULATIVE_LENGTH ?
        //        WIDTH_GEAR Largeur cumulée (drague),              missing in SUMARIS
        //        SEINE_LENGTH Longueur de la bolinche ou senne     missing in SUMARIS

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesListQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesListQuery(context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesListTable"));

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
            case "injectionTripTable":
            case "injectionStationTable":
            case "injectionSpeciesListTable":
            case "injectionSpeciesLengthTable":
                return getQueryFullName(Free1Specification.FORMAT, Free1Specification.VERSION_1, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}
