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

package net.sumaris.core.dao.data.batch;

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import javax.annotation.Nonnull;
import java.util.List;

public interface DenormalizedBatchSpecifications<E extends DenormalizedBatch, V extends DenormalizedBatchVO> {

    String TRIP_ID_PARAM = "tripId";

    default Specification<E> hasNoParent() {
        return BindableSpecification.where((root, query, cb) ->
            cb.isNull(root.get(DenormalizedBatch.Fields.PARENT))
        );
    }

    default Specification<E> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        BindableSpecification<E> specification = BindableSpecification.where((root, query, cb) -> {
            Join<Operation, Trip> tripJoin = Daos.composeJoin(root, StringUtils.doting(DenormalizedBatch.Fields.OPERATION, Operation.Fields.TRIP), JoinType.LEFT);
            ParameterExpression<Integer> param = cb.parameter(Integer.class, TRIP_ID_PARAM);
            query.orderBy(cb.asc(Daos.composePath(root, StringUtils.doting(DenormalizedBatch.Fields.OPERATION, IEntity.Fields.ID))), cb.asc(root.get(DenormalizedBatch.Fields.FLAT_RANK_ORDER)));
            return cb.equal(tripJoin.get(IEntity.Fields.ID), param);
        });
        specification.addBind(TRIP_ID_PARAM, tripId);
        return specification;
    }

    default Specification<E> hasOperationId(Integer operationId) {
        if (operationId == null) return null;

        BindableSpecification<E> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, DenormalizedBatchVO.Fields.OPERATION_ID);
            // Sort by flat rank order
            query.orderBy(cb.asc(root.get(DenormalizedBatch.Fields.FLAT_RANK_ORDER)));
            return cb.equal(root.get(DenormalizedBatch.Fields.OPERATION).get(IEntity.Fields.ID), param);
        });
        specification.addBind(DenormalizedBatchVO.Fields.OPERATION_ID, operationId);
        return specification;
    }

    default Specification<E> hasSaleId(Integer saleId) {
        if (saleId == null) return null;

        BindableSpecification<E> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, DenormalizedBatchVO.Fields.SALE_ID);
            return cb.equal(root.get(DenormalizedBatch.Fields.SALE).get(IEntity.Fields.ID), param);
        });
        specification.addBind(DenormalizedBatchVO.Fields.SALE_ID, saleId);
        return specification;
    }

    default Specification<E> isLanding(Boolean isLanding) {
        if (isLanding == null) return null;
        BindableSpecification<E> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Boolean> param = cb.parameter(Boolean.class, DenormalizedBatchVO.Fields.IS_LANDING);
            return cb.equal(root.get(DenormalizedBatch.Fields.IS_LANDING), param);
        });
        specification.addBind(DenormalizedBatchVO.Fields.IS_LANDING, isLanding);
        return specification;
    }

    default Specification<E> isDiscard(Boolean isDiscard) {
        if (isDiscard == null) return null;
        BindableSpecification<E> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Boolean> param = cb.parameter(Boolean.class, DenormalizedBatchVO.Fields.IS_DISCARD);
            return cb.equal(root.get(DenormalizedBatch.Fields.IS_DISCARD), param);
        });
        specification.addBind(DenormalizedBatchVO.Fields.IS_DISCARD, isDiscard);
        return specification;
    }

    V getCatchBatchByOperationId(int operationId);

    V getCatchBatchBySaleId(int saleId);

    List<V> saveAllByOperationId(int operationId, @Nonnull List<V> sources);

    List<V> saveAllBySaleId(int saleId, @Nonnull List<V> sources);

    V toVO(E source);

    void copy(BatchVO source, V target, boolean copyIfNull);
}
