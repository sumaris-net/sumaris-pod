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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.rdb.AggregationRdbTripDaoImpl;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationRjbTripDao")
@Lazy
@Slf4j
public class AggregationRjbTripDaoImpl<
    C extends AggregationRdbTripContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO>
    extends AggregationRdbTripDaoImpl<C, F, S>
    implements AggSurvivalTestSpecification {

    @Override
    public AggExtractionTypeEnum getFormat() {
        return AggExtractionTypeEnum.AGG_RJB_TRIP;
    }

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, @Nullable F filter, S strata) {
        R context = super.aggregate(source, filter, strata);

        context.setType(AggExtractionTypeEnum.AGG_RJB_TRIP);

        return context;
    }

    /* -- protected methods -- */

    @Override
    protected Class<? extends AggregationRdbTripContextVO> getContextClass() {
        return AggregationRdbTripContextVO.class;
    }

    @Override
    protected void fillContextTableNames(C context) {
        super.fillContextTableNames(context);

        context.addColumnNameReplacement(RdbSpecification.COLUMN_INDIVIDUAL_SEX, RjbTripSpecification.COLUMN_SEX);
    }

    @Override
    protected XMLQuery createSpeciesListQuery(ExtractionProductVO source, C context) {
        XMLQuery xmlQuery = super.createSpeciesListQuery(source, context);

        // - Hide weight columns (will be replace with columns on individual count)
        xmlQuery.setGroup("weight", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesListTable"));

        return xmlQuery;

    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(ExtractionProductVO source, C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(source, context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        return xmlQuery;

    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionSpeciesListTable":
            case "injectionSpeciesLengthTable":
                return getQueryFullName(AggRjbTripSpecification.FORMAT, AggRjbTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

}
