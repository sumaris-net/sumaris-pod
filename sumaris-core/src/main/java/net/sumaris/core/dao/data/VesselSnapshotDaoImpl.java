package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 19/11/2019.
 */
@Repository("vesselSnapshotDao")
@Slf4j
public class VesselSnapshotDaoImpl extends HibernateDaoSupport implements VesselSnapshotDao {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Override
    public VesselSnapshotVO getByIdAndDate(int vesselId, Date date) {
        VesselFilterVO filter = VesselFilterVO.builder()
            .vesselId(vesselId)
            .startDate(date)
            .build();
        List<VesselSnapshotVO> res = findByFilter(filter, 0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC);

        // No result for this date
        if (res.size() == 0) {
            // Retry using only vessel id (and limit to most recent features)
            filter.setDate(null);
            res = findByFilter(filter, 0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC);
            if (res.size() == 0) {
                VesselSnapshotVO unknownVessel = new VesselSnapshotVO();
                unknownVessel.setId(vesselId);
                unknownVessel.setName("Unknown vessel " + vesselId); // TODO remove string
                return unknownVessel;
            }
        }
        return res.get(0);
    }

    @Override
    public List<VesselSnapshotVO> findByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselSnapshotResult> query = cb.createQuery(VesselSnapshotResult.class);
        Root<VesselFeatures> root = query.from(VesselFeatures.class);

        Join<VesselFeatures, Vessel> vesselJoin = root.join(VesselFeatures.Fields.VESSEL, JoinType.INNER);
        Join<Vessel, Program> programJoin = vesselJoin.join(Vessel.Fields.PROGRAM, JoinType.INNER);
        Join<Vessel, VesselRegistrationPeriod> vrpJoin = vesselJoin.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

        query.multiselect(root, vrpJoin);

        // Apply sorting
        addSorting(query, cb, root, sortAttribute, sortDirection);

        // No filter: execute request
        if (filter == null) {
            TypedQuery<VesselSnapshotResult> q = getEntityManager().createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(size);
            return toVesselSnapshotVOs(q.getResultList());
        }

        List<Integer> statusIds = CollectionUtils.isEmpty(filter.getStatusIds())
            ? null
            : filter.getStatusIds();

        // Apply vessel Filter
        ParameterExpression<String> programParam = cb.parameter(String.class);
        ParameterExpression<Date> dateParam = cb.parameter(Date.class);
        ParameterExpression<Integer> vesselIdParam = cb.parameter(Integer.class);
        ParameterExpression<Integer> vesselFeaturesIdParam = cb.parameter(Integer.class);
        ParameterExpression<String> searchNameParam = cb.parameter(String.class);
        ParameterExpression<String> searchExteriorMarkingParam = cb.parameter(String.class);
        ParameterExpression<String> searchRegistrationCodeParam = cb.parameter(String.class);
        ParameterExpression<Boolean> hasStatusIdsParam = cb.parameter(Boolean.class);
        ParameterExpression<Collection> statusIdsParam = cb.parameter(Collection.class);

        query.where(cb.and(
            // Program
            cb.or(
                cb.isNull(programParam),
                cb.equal(programJoin.get(Program.Fields.LABEL), programParam)
            ),

            // Filter: date
            cb.or(
                cb.and(
                    // if no date in filter, will return only active period
                    cb.isNull(dateParam.as(String.class)),
                    cb.isNull(root.get(VesselFeatures.Fields.END_DATE)),
                    cb.isNull(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE))
                ),
                cb.and(
                    cb.isNotNull(dateParam.as(String.class)),
                    cb.and(
                        cb.or(
                            cb.isNull(root.get(VesselFeatures.Fields.END_DATE)),
                            cb.greaterThan(root.get(VesselFeatures.Fields.END_DATE), dateParam)
                        ),
                        cb.lessThan(root.get(VesselFeatures.Fields.START_DATE), dateParam)
                    ),
                    cb.and(
                        cb.or(
                            cb.isNull(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE)),
                            cb.greaterThan(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE), dateParam)
                        ),
                        cb.lessThan(vrpJoin.get(VesselRegistrationPeriod.Fields.START_DATE), dateParam)
                    )
                )
            ),

            // Filter: vessel features id
            cb.or(
                cb.isNull(vesselFeaturesIdParam),
                cb.equal(root.get(VesselFeatures.Fields.ID), vesselFeaturesIdParam)
            ),

            // Filter: vessel id
            cb.or(
                cb.isNull(vesselIdParam),
                cb.equal(vesselJoin.get(Vessel.Fields.ID), vesselIdParam))
            ),

            // Filter: search text (on exterior marking OR id)
            cb.or(
                cb.isNull(searchNameParam),
                cb.like(cb.lower(root.get(VesselFeatures.Fields.NAME)), searchNameParam, Daos.LIKE_ESCAPE_CHAR),
                cb.like(cb.lower(root.get(VesselFeatures.Fields.EXTERIOR_MARKING)), searchExteriorMarkingParam, Daos.LIKE_ESCAPE_CHAR),
                cb.like(cb.lower(vrpJoin.get(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)), searchRegistrationCodeParam, Daos.LIKE_ESCAPE_CHAR)
            ),

            // Status
            cb.or(
                cb.isFalse(hasStatusIdsParam),
                cb.in(vesselJoin.get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(statusIdsParam)
            )
        );


        String searchTextAsPrefix = Daos.getEscapedSearchText(filter.getSearchText());
        searchTextAsPrefix = searchTextAsPrefix != null ? searchTextAsPrefix.toLowerCase() : null;
        String searchTextAnyMatch = StringUtils.isNotBlank(searchTextAsPrefix) ? ("%"+searchTextAsPrefix) : null;

        TypedQuery<VesselSnapshotResult> q = getEntityManager().createQuery(query)
            .setParameter(programParam, filter.getProgramLabel())
            .setParameter(dateParam, filter.getDate())
            .setParameter(vesselFeaturesIdParam, filter.getVesselFeaturesId())
            .setParameter(vesselIdParam, filter.getVesselId())
            .setParameter(searchExteriorMarkingParam, searchTextAsPrefix)
            .setParameter(searchRegistrationCodeParam, searchTextAsPrefix)
            .setParameter(searchNameParam, searchTextAnyMatch)
            .setParameter(hasStatusIdsParam, CollectionUtils.isNotEmpty(statusIds))
            .setParameter(statusIdsParam, statusIds)
            .setFirstResult(offset)
            .setMaxResults(size);
        List<VesselSnapshotResult> result = q.getResultList();
        return toVesselSnapshotVOs(result);
    }

    private List<VesselSnapshotVO> toVesselSnapshotVOs(List<VesselSnapshotResult> source) {
        return source.stream()
            .map(this::toVesselSnapshotVO)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private VesselSnapshotVO toVesselSnapshotVO(VesselSnapshotResult source) {
        if (source == null)
            return null;

        VesselSnapshotVO target = new VesselSnapshotVO();

        // Vessel features
        VesselFeatures features = source.getVesselFeatures();

        Beans.copyProperties(features, target);

        // Convert from cm to m
        if (features.getLengthOverAll() != null) {
            target.setLengthOverAll(features.getLengthOverAll().doubleValue() /100);
        }
        // Convert tonnage (divide by 100)
        if (features.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt(features.getGrossTonnageGrt().doubleValue() / 100);
        }
        if (features.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt(features.getGrossTonnageGt().doubleValue() / 100);
        }

        target.setId(features.getVessel().getId());
        target.setVesselStatusId(features.getVessel().getStatus().getId());
        target.setQualityFlagId(features.getQualityFlag().getId());

        // Vessel type
        ReferentialVO vesselType = referentialDao.toVO(features.getVessel().getVesselType());
        target.setVesselType(vesselType);

        // base port location
        LocationVO basePortLocation = locationRepository.toVO(features.getBasePortLocation());
        target.setBasePortLocation(basePortLocation);

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(features.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        // Registration period
        VesselRegistrationPeriod period = source.getVesselRegistrationPeriod();
        if (period != null) {

            // Registration code
            target.setRegistrationCode(period.getRegistrationCode());

            // Int registration code
            target.setIntRegistrationCode(period.getIntRegistrationCode());

            // Registration location
            LocationVO registrationLocation = locationRepository.toVO(period.getRegistrationLocation());
            target.setRegistrationLocation(registrationLocation);
        }

        return target;

    }

    @Data
    @AllArgsConstructor
    public static class VesselSnapshotResult {
        VesselFeatures vesselFeatures;
        VesselRegistrationPeriod vesselRegistrationPeriod;
    }

}
