package net.sumaris.extraction.core.dao.trip.cost;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.extraction.core.dao.trip.rdb.AggregationRdbTripDaoImpl;
import net.sumaris.extraction.core.format.ProductFormatEnum;
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
@Repository("aggregationCostDao")
@Lazy
@Slf4j
public class AggregationCostDaoImpl<
    C extends AggregationRdbTripContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO>
    extends AggregationRdbTripDaoImpl<C, F, S>
    implements AggCostSpecification {

    @Override
    public ProductFormatEnum getFormat() {
        return ProductFormatEnum.AGG_COST;
    }

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, @Nullable F filter, S strata) {
        R context = super.aggregate(source, filter, strata);

        context.setFormat(ProductFormatEnum.AGG_COST);

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

        // Rename some columns
        context.addColumnNameReplacement(RdbSpecification.COLUMN_INDIVIDUAL_SEX, CostSpecification.COLUMN_SEX);
    }

}