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

package net.sumaris.core.dao.referential.conversion;

import net.sumaris.core.dao.referential.IEntityWithStatusSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.conversion.RoundWeightConversion;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.RoundWeightConversionVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Date;
import java.util.List;

public interface RoundWeightConversionSpecifications extends IEntityWithStatusSpecifications<Integer, RoundWeightConversion> {

    String DATE_PARAMETER = "date";

    default Specification<RoundWeightConversion> hasTaxonGroupIds(Integer... ids) {
        return hasInnerJoinIds(RoundWeightConversion.Fields.TAXON_GROUP, ids);
    }

    default Specification<RoundWeightConversion> hasLocationIds(Integer... ids) {
        return hasInnerJoinIds(RoundWeightConversion.Fields.LOCATION, ids);
    }

    default Specification<RoundWeightConversion> hasDressingIds(Integer... ids) {
        return hasInnerJoinIds(RoundWeightConversion.Fields.DRESSING, ids);
    }

    default Specification<RoundWeightConversion> hasPreservingIds(Integer... ids) {
        return hasInnerJoinIds(RoundWeightConversion.Fields.PRESERVING, ids);
    }

    default Specification<RoundWeightConversion> atDate(Date aDate) {
        if (aDate == null) return null;
        return BindableSpecification.where((root, query, cb) -> {

            ParameterExpression<Date> dateParam = cb.parameter(Date.class, DATE_PARAMETER);
            return cb.not(
                cb.or(
                    cb.lessThan(Daos.nvlEndDate(root.get(RoundWeightConversion.Fields.END_DATE), cb, getDatabaseType()), dateParam),
                    cb.greaterThan(root.get(RoundWeightConversion.Fields.START_DATE), dateParam)
                )
            );
        }).addBind(DATE_PARAMETER, Dates.resetTime(aDate));
    }


    DatabaseType getDatabaseType();

    List<RoundWeightConversionVO> findAll(RoundWeightConversionFilterVO filter, Page page, RoundWeightConversionFetchOptions fetchOptions);

    long count(RoundWeightConversionFilterVO filter);
}
