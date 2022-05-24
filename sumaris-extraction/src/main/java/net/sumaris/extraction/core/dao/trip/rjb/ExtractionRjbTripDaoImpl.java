package net.sumaris.extraction.core.dao.trip.rjb;

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
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.specification.data.trip.RjbTripSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Extraction for RJB (Pocheteaux). We use individual count, instead of weight
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionRjbTripDao")
@Lazy
@Slf4j
public class ExtractionRjbTripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements RjbTripSpecification {

    @Override
    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.RJB_TRIP);
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        context.setType(LiveExtractionTypeEnum.RJB_TRIP);

        return context;
    }

    /* -- protected methods -- */

    protected XMLQuery createTripQuery(C context) {

        XMLQuery xmlQuery = super.createTripQuery(context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionTripTable"));

        // Bind some referential ids
        xmlQuery.bind("contractCodePmfmIds", Daos.getSqlInNumbers(
            PmfmEnum.CONTRACT_CODE.getId(),
            PmfmEnum.SELF_SAMPLING_PROGRAM.getId()
        ));
        // Bind some referential ids
        xmlQuery.bind("contractCodeLike", "%RJB"); // TODO externalize

        return xmlQuery;
    }

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        Set<String> excludedColumns = ImmutableSet.of(RdbSpecification.COLUMN_GEAR_TYPE);
        Set<String> groupByColumns = xmlQuery.getColumnNames(e -> !xmlQuery.hasGroup(e, "agg")
            && !excludedColumns.contains(xmlQuery.getAttributeValue(e, "alias", true)));
        xmlQuery.bind("groupByColumns", String.join(",", groupByColumns));

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

        switch (queryName) {
            case "injectionTripTable":
            case "injectionRawSpeciesListTable":
            case "injectionSpeciesListTable":
            case "injectionSpeciesLengthTable":
                return getQueryFullName(RjbTripSpecification.FORMAT, RjbTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}
