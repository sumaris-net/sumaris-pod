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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.conversion.WeightLengthConversion;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.*;

public interface WeightLengthConversionSpecifications extends IEntityWithStatusSpecifications<WeightLengthConversion> {

    String MONTH_PARAMETER = "month";

    default Specification<WeightLengthConversion> hasReferenceTaxonIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.REFERENCE_TAXON, ids);
    }

    default Specification<WeightLengthConversion> hasLocationIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.LOCATION, ids);
    }

    default Specification<WeightLengthConversion> hasSexIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.SEX, ids);
    }

    default Specification<WeightLengthConversion> hasLengthParameterIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.LENGTH_PARAMETER, ids);
    }

    default Specification<WeightLengthConversion> hasLengthUnitIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.LENGTH_UNIT, ids);
    }

    default Specification<WeightLengthConversion> atDate(Date aDate) {
        if (aDate == null) return null;
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.setTime(aDate);
        Integer month = aCalendar.get(Calendar.MONTH);
        return BindableSpecification.where((root, query, builder) -> {

            ParameterExpression<Integer> monthParam = builder.parameter(Integer.class, MONTH_PARAMETER);
            return builder.and(
                builder.lessThanOrEqualTo(root.get(WeightLengthConversion.Fields.START_MONTH), monthParam),
                builder.greaterThanOrEqualTo(root.get(WeightLengthConversion.Fields.END_MONTH), monthParam)
            );
        }).addBind(MONTH_PARAMETER, month);
    }

    List<WeightLengthConversionVO> findAll(WeightLengthConversionFilterVO filter, Page page, WeightLengthConversionFetchOptions fetchOptions);

    long count(WeightLengthConversionFilterVO filter);
}
