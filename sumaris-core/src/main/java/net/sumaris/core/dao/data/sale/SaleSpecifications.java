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

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.vo.data.SaleVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SaleSpecifications extends RootDataSpecifications<Sale> {

    String TRIP_ID_PARAM = "tripId";

    default Specification<Sale> hasTripId(Integer tripId) {
        BindableSpecification<Sale> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Sale.Fields.TRIP).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(TRIP_ID_PARAM, tripId);
        return specification;
    }

    List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sales);

}
