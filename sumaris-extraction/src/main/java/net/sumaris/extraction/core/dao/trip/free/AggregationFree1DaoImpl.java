package net.sumaris.extraction.core.dao.trip.free;

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
@Repository("aggregationFree1Dao")
@Lazy
@Slf4j
public class AggregationFree1DaoImpl<
    C extends AggregationRdbTripContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO>
    extends AggregationRdbTripDaoImpl<C, F, S>
    implements AggSurvivalTestSpecification {

    @Override
    public ProductFormatEnum getFormat() {
        return ProductFormatEnum.AGG_FREE;
    }

    @Override
    public <R extends C> R aggregate(ExtractionProductVO source, @Nullable F filter, S strata) {
        R context = super.aggregate(source, filter, strata);

        context.setFormat(ProductFormatEnum.AGG_FREE);

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
        context.addColumnNameReplacement(AggRdbSpecification.COLUMN_FISHING_TIME, Free1Specification.COLUMN_FISHING_DURATION)
            .addColumnNameReplacement(RdbSpecification.COLUMN_DATE, Free1Specification.COLUMN_FISHING_DATE)
            .addColumnNameReplacement(RdbSpecification.COLUMN_TIME, Free1Specification.COLUMN_FISHING_TIME)
            .addColumnNameReplacement(RdbSpecification.COLUMN_INDIVIDUAL_SEX, Free1Specification.COLUMN_SEX);
    }

    @Override
    protected XMLQuery createStationQuery(ExtractionProductVO source, C context) {
        XMLQuery xmlQuery = super.createStationQuery(source, context);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionStationTable"));

        return xmlQuery;

    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionStationTable":
                return getQueryFullName(AggFree1Specification.FORMAT, AggFree1Specification.VERSION_1, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

}
