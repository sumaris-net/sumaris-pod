package net.sumaris.core.dao.data.sale;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.ExpectedSale;
import net.sumaris.core.vo.data.ExpectedSaleVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface ExpectedSaleSpecifications {

    String TRIP_ID_PARAM = "tripId";

    default Specification<ExpectedSale> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.equal(root.get(ExpectedSale.Fields.TRIP).get(IEntity.Fields.ID), param);
        }).addBind(TRIP_ID_PARAM, tripId);
    }

    List<ExpectedSaleVO> getAllByTripId(int tripId);

    List<ExpectedSaleVO> saveAllByTripId(int tripId, List<ExpectedSaleVO> sales);

}
