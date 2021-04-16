package net.sumaris.core.extraction.dao.trip.cost;

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
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDaoImpl;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.format.ProductFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.AggCostSpecification;
import net.sumaris.core.extraction.specification.data.trip.AggSurvivalTestSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.extraction.vo.trip.survivalTest.AggregationSurvivalTestContextVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationCostDao")
@Lazy
public class AggregationCostDaoImpl<C extends AggregationRdbTripContextVO, F extends ExtractionFilterVO, S extends AggregationStrataVO>
        extends AggregationRdbTripDaoImpl<C, F, S>
        implements AggSurvivalTestSpecification {

    private static final Logger log = LoggerFactory.getLogger(AggregationCostDaoImpl.class);

    @Override
    public ProductFormatEnum getFormat() {
        return ProductFormatEnum.AGG_COST;
    }

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, F filter, S strata) {
        R context = super.aggregate(source, filter, strata);

        context.setFormat(ProductFormatEnum.AGG_COST);

        return context;
    }

    /* -- protected methods -- */

    @Override
    protected Class<? extends AggregationRdbTripContextVO> getContextClass() {
        return AggregationSurvivalTestContextVO.class;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionSpeciesLengthTable":
                return getQueryFullName(AggCostSpecification.FORMAT, AggCostSpecification.VERSION_1_4, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }


    @Override
    protected XMLQuery createSpeciesLengthQuery(ExtractionProductVO source, C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(source, context);

        // Special case for COST format:

        // - Hide sex columns, then replace by a new column
        xmlQuery.setGroup("sex", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        return xmlQuery;

    }

}
