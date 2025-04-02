package net.sumaris.core.dao.data.fishingArea;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.FishingArea;
import net.sumaris.core.vo.data.FishingAreaVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

public interface FishingAreaRepository
    extends SumarisJpaRepository<FishingArea, Integer, FishingAreaVO>, FishingAreaSpecifications
{


    default Specification<FishingArea> hasOperationId(Integer operationId) {
        if (operationId == null) return null;
        BindableSpecification<FishingArea> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, FishingAreaVO.Fields.OPERATION_ID);

            return cb.equal(root.get(FishingArea.Fields.OPERATION).get(IEntity.Fields.ID), param);
        });
        specification.addBind(FishingAreaVO.Fields.OPERATION_ID, operationId);
        return specification;
    }

    default Specification<FishingArea> hasSaleId(Integer saleId) {
        if (saleId == null) return null;
        BindableSpecification<FishingArea> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, FishingAreaVO.Fields.SALE_ID);
            return cb.equal(root.get(FishingArea.Fields.SALE).get(IEntity.Fields.ID), param);
        });
        specification.addBind(FishingAreaVO.Fields.SALE_ID, saleId);
        return specification;
    }
    List<FishingArea> getFishingAreaByOperationId(int operationId);

    List<FishingArea> getFishingAreaByGearUseFeaturesId(int gearUseFeaturesId);

    void deleteAllByOperationId(int operationId);
}
