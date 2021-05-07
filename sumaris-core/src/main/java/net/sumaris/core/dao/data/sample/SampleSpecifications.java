package net.sumaris.core.dao.data.sample;

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
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.Sample;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SampleSpecifications extends RootDataSpecifications<Sample> {

    default Specification<Sample> hasOperationId(Integer operationId) {
        if (operationId == null) return null;
        BindableSpecification<Sample> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, SampleVO.Fields.OPERATION_ID);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Sample.Fields.OPERATION).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(SampleVO.Fields.OPERATION_ID, operationId);
        return specification;
    }

    default Specification<Sample> hasLandingId(Integer landingId) {
        if (landingId == null) return null;
        BindableSpecification<Sample> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, SampleVO.Fields.LANDING_ID);
            return criteriaBuilder.equal(root.get(Sample.Fields.LANDING).get(IEntity.Fields.ID), param);
        });
        specification.addBind(SampleVO.Fields.LANDING_ID, landingId);
        return specification;
    }

    default Specification<Sample> addJoinFetch(SampleFetchOptions fetchOptions, boolean addQueryDistinct) {
        if (fetchOptions == null || !fetchOptions.isWithMeasurementValues()) return null;

        return BindableSpecification.where((root, query, criteriaBuilder) -> {
            if (addQueryDistinct) query.distinct(true); // Need if findAll() is called, to avoid to many rows
            root.fetch(Sample.Fields.MEASUREMENTS, JoinType.LEFT);
            return null;
        });
    }

    List<SampleVO> saveByOperationId(int operationId, List<SampleVO> samples);

    List<SampleVO> saveByLandingId(int landingId, List<SampleVO> samples);

}
