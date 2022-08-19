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
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.Sample;
import net.sumaris.core.model.data.SampleMeasurement;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SampleSpecifications extends RootDataSpecifications<Sample> {

    String OBSERVED_LOCATION_IDS = "observedLocationIds";
    String TAG_ID_PMFM_ID = "tagIdPmfmId";
    String TAG_ID = "tagId";

    default Specification<Sample> hasOperationId(Integer operationId) {
        if (operationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.orderBy(cb.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = cb.parameter(Integer.class, SampleVO.Fields.OPERATION_ID);
            return cb.equal(root.get(Sample.Fields.OPERATION).get(IEntity.Fields.ID), param);
        }).addBind(SampleVO.Fields.OPERATION_ID, operationId);
    }

    default Specification<Sample> hasLandingId(Integer landingId) {
        if (landingId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.orderBy(cb.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = cb.parameter(Integer.class, SampleVO.Fields.LANDING_ID);
            return cb.equal(root.get(Sample.Fields.LANDING).get(IEntity.Fields.ID), param);
        }).addBind(SampleVO.Fields.LANDING_ID, landingId);
    }

    default Specification<Sample> hasObservedLocationId(Integer observedLocationId) {
        if (observedLocationId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.orderBy(cb.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> param = cb.parameter(Integer.class, LandingVO.Fields.OBSERVED_LOCATION_ID);
            return cb.equal(
                    root.join(Sample.Fields.LANDING, JoinType.INNER)
                            .join(Landing.Fields.OBSERVED_LOCATION, JoinType.INNER)
                            .get(IEntity.Fields.ID),
                    param);
        }).addBind(LandingVO.Fields.OBSERVED_LOCATION_ID, observedLocationId);
    }

    default Specification<Sample> inObservedLocationIds(Integer... observedLocationIds) {
        if (ArrayUtils.isEmpty(observedLocationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.orderBy(cb.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Collection> param = cb.parameter(Collection.class, OBSERVED_LOCATION_IDS);
            return cb.in(
                    root.join(Sample.Fields.LANDING, JoinType.INNER)
                            .join(Landing.Fields.OBSERVED_LOCATION, JoinType.INNER)
                            .get(IEntity.Fields.ID))
                    .value(param);
        }).addBind(OBSERVED_LOCATION_IDS, Arrays.asList(observedLocationIds));
    }

    default Specification<Sample> hasTagId(String tagId) {
        if (tagId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.orderBy(cb.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> tagIdPmfmIdParam = cb.parameter(Integer.class, TAG_ID_PMFM_ID);
            ParameterExpression<String> tagIdParam = cb.parameter(String.class, TAG_ID);
            Join<Sample, SampleMeasurement> tagIdInnerJoin = root.joinList(Sample.Fields.MEASUREMENTS, JoinType.INNER);
            return cb.and(
                    cb.equal(tagIdInnerJoin.get(SampleMeasurement.Fields.PMFM).get(IEntity.Fields.ID), tagIdPmfmIdParam),
                    cb.equal(tagIdInnerJoin.get(SampleMeasurement.Fields.ALPHANUMERICAL_VALUE), tagIdParam)
            );
        })
            .addBind(TAG_ID_PMFM_ID, PmfmEnum.TAG_ID.getId())
            .addBind(TAG_ID, tagId);
    }

    default Specification<Sample> withTagId(Boolean withTagId) {
        if (!Boolean.TRUE.equals(withTagId)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            query.orderBy(cb.asc(root.get(Sample.Fields.RANK_ORDER)));
            ParameterExpression<Integer> tagIdPmfmIdParam = cb.parameter(Integer.class, TAG_ID_PMFM_ID);
            Join<Sample, SampleMeasurement> tagIdInnerJoin = root.joinList(Sample.Fields.MEASUREMENTS, JoinType.INNER);
            return cb.equal(tagIdInnerJoin.get(SampleMeasurement.Fields.PMFM).get(IEntity.Fields.ID), tagIdPmfmIdParam);
        }).addBind(TAG_ID_PMFM_ID, PmfmEnum.TAG_ID.getId());
    }

    default Specification<Sample> addJoinFetch(SampleFetchOptions fetchOptions, boolean addQueryDistinct) {
        if (fetchOptions == null || !fetchOptions.isWithMeasurementValues()) return null;

        return BindableSpecification.where((root, query, cb) -> {
            if (addQueryDistinct) query.distinct(true); // Need if findAll() is called, to avoid to many rows
            root.fetch(Sample.Fields.MEASUREMENTS, JoinType.LEFT);
            return null;
        });
    }

    List<SampleVO> saveByOperationId(int operationId, List<SampleVO> samples);

    List<SampleVO> saveByLandingId(int landingId, List<SampleVO> samples);

}
