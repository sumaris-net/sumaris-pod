package net.sumaris.core.dao.data.batch;

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

import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.batch.BatchVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 30/03/2020.
 */
public interface BatchSpecifications extends DataSpecifications<Batch> {

    String DEFAULT_ROOT_BATCH_LABEL = "CATCH_BATCH";

    default Specification<Batch> hasNoParent() {
        return BindableSpecification.where((root, query, cb) ->
            cb.isNull(root.get(Batch.Fields.PARENT))
        );
    }

    default Specification<Batch> hasOperationId(Integer operationId) {
        if (operationId == null) return null;

        BindableSpecification<Batch> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, BatchVO.Fields.OPERATION_ID);

            // Sort by rank order
            query.orderBy(cb.asc(root.get(Batch.Fields.RANK_ORDER)));

            return cb.equal(root.get(Batch.Fields.OPERATION).get(IEntity.Fields.ID), param);
        });
        specification.addBind(BatchVO.Fields.OPERATION_ID, operationId);
        return specification;
    }

    default Specification<Batch> hasSaleId(Integer saleId) {
        if (saleId == null) return null;

        BindableSpecification<Batch> specification = BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, BatchVO.Fields.SALE_ID);

            // Sort by rank order
            query.orderBy(cb.asc(root.get(Batch.Fields.RANK_ORDER)));

            return cb.equal(root.get(Batch.Fields.SALE).get(IEntity.Fields.ID), param);
        });
        specification.addBind(BatchVO.Fields.SALE_ID, saleId);
        return specification;
    }

    default Specification<Batch> addJoinFetch(BatchFetchOptions fetchOptions, boolean addQueryDistinct) {
        if (fetchOptions == null || !fetchOptions.isWithMeasurementValues()) return null;

        return BindableSpecification.where((root, query, cb) -> {
            if (addQueryDistinct) query.distinct(true); // Need if findAll() is called, to avoid to many rows
            root.fetch(Batch.Fields.SORTING_MEASUREMENTS, JoinType.LEFT);
            return null;
        });
    }


    BatchVO getCatchBatchByOperationId(int operationId, BatchFetchOptions fetchOptions);

    BatchVO getCatchBatchBySaleId(int saleId, BatchFetchOptions fetchOptions);

    List<BatchVO> saveAllByOperationId(int operationId, List<BatchVO> sources);

    List<BatchVO> saveBySaleId(int saleId, List<BatchVO> sources);

    List<BatchVO> toFlatList(BatchVO source);

    BatchVO toTree(List<BatchVO> sources);
}
