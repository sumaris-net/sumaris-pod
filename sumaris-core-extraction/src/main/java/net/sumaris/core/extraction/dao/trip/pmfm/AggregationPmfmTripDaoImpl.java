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
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDaoImpl;
import net.sumaris.core.extraction.format.ProductFormatEnum;
import net.sumaris.core.extraction.specification.data.trip.AggCostSpecification;
import net.sumaris.core.extraction.specification.data.trip.AggPmfmTripSpecification;
import net.sumaris.core.extraction.specification.data.trip.AggSurvivalTestSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.extraction.vo.trip.survivalTest.AggregationSurvivalTestContextVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("aggregationPmfmTripDao")
@Lazy
@Slf4j
public class AggregationPmfmTripDaoImpl<
    C extends AggregationRdbTripContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO>
    extends AggregationRdbTripDaoImpl<C, F, S>
    implements AggSurvivalTestSpecification {

    @Override
    public ProductFormatEnum getFormat() {
        return ProductFormatEnum.AGG_PMFM_TRIP;
    }

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, F filter, S strata) {
        R context = super.aggregate(source, filter, strata);

        context.setFormat(ProductFormatEnum.AGG_PMFM_TRIP);

        return context;
    }

    /* -- protected methods -- */

    @Override
    protected Class<? extends AggregationRdbTripContextVO> getContextClass() {
        return AggregationRdbTripContextVO.class;
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

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionSpeciesLengthTable":
                return getQueryFullName(AggPmfmTripSpecification.FORMAT, AggPmfmTripSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

}