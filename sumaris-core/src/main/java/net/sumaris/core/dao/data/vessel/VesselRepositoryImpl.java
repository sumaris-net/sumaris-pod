package net.sumaris.core.dao.data.vessel;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;

@Slf4j
public class VesselRepositoryImpl
    extends RootDataRepositoryImpl<Vessel, VesselVO, VesselFilterVO, DataFetchOptions>
    implements VesselSpecifications {

    private final VesselFeaturesRepository vesselFeaturesRepository;
    private final VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository;
    private final ReferentialDao referentialDao;
    private final ProgramRepository programRepository;

    @Autowired
    public VesselRepositoryImpl(EntityManager entityManager,
                                VesselFeaturesRepository vesselFeaturesRepository,
                                VesselRegistrationPeriodRepository vesselRegistrationPeriodRepository,
                                ReferentialDao referentialDao,
                                ProgramRepository programRepository) {
        super(Vessel.class, VesselVO.class, entityManager);
        setCheckUpdateDate(true);
        setLockForUpdate(true);

        this.vesselFeaturesRepository = vesselFeaturesRepository;
        this.vesselRegistrationPeriodRepository = vesselRegistrationPeriodRepository;
        this.referentialDao = referentialDao;
        this.programRepository = programRepository;
    }

    @Override
    public Page<VesselVO> findAll(VesselFilterVO filter, Pageable pageable, DataFetchOptions fetchOptions) {
        Specification<Vessel> spec = toSpecification(filter, fetchOptions);

        CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselQueryResult> criteriaQuery = builder.createQuery(VesselQueryResult.class);

        Root<Vessel> root = criteriaQuery.from(Vessel.class);
        Join<Vessel, VesselFeatures> featuresJoin = root.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
        Join<Vessel, VesselRegistrationPeriod> vrpJoin = root.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

        criteriaQuery.multiselect(root, featuresJoin, vrpJoin);
        Predicate predicate = spec.toPredicate(root, criteriaQuery, builder);
        if (predicate != null) {
            criteriaQuery.where(predicate);
        }

        Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();
        if (sort.isSorted()) {
            criteriaQuery.orderBy(QueryUtils.toOrders(sort, root, builder));
        }


        TypedQuery<VesselQueryResult> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        if (pageable.isPaged()) {
            query.setFirstResult((int)pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        List<VesselQueryResult> result = query.getResultList();

        // Replace the count of total element, by the result size
        final long fakeTotal =  pageable.getOffset() + result.size();

        return PageableExecutionUtils.getPage(query.getResultList(), pageable, () -> fakeTotal)
            .map(row -> toVO(row, fetchOptions, true));

    }

    @Override
    public Specification<Vessel> toSpecification(VesselFilterVO filter, DataFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getVesselId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(vesselFeatures(filter.getVesselFeaturesId()))
            .and(statusIds(filter.getStatusIds()))
            .and(searchText(filter.getSearchAttributes(), filter.getSearchText()));
    }

    @Override
    public VesselVO save(VesselVO vo, boolean checkUpdateDate) {
        return super.save(vo, checkUpdateDate);
    }

    @Override
    public void toVO(Vessel source, VesselVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Status
        target.setStatusId(source.getStatus().getId());

        // Vessel type
        ReferentialVO vesselType = referentialDao.toVO(source.getVesselType());
        target.setVesselType(vesselType);

        // Recorder department
        if (fetchOptions != null && fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);
        }

        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {
            // Vessel features
            target.setVesselFeatures(vesselFeaturesRepository.getLastByVesselId(source.getId(), DataFetchOptions.MINIMAL).orElse(null));

            // Vessel registration period
            target.setVesselRegistrationPeriod(vesselRegistrationPeriodRepository.getLastByVesselId(source.getId()).orElse(null));
        }

    }


    @Override
    public void toEntity(VesselVO source, Vessel target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Vessel type
        if (copyIfNull || source.getVesselType() != null) {
            if (source.getVesselType() == null) {
                target.setVesselType(null);
            } else {
                target.setVesselType(getReference(VesselType.class, source.getVesselType().getId()));
            }
        }

        // Vessel status
        if (copyIfNull || source.getStatusId() != null) {
            if (source.getStatusId() == null) {
                target.setStatus(null);
            } else {
                target.setStatus(getReference(Status.class, source.getStatusId()));
            }
        }

        // Default program
        if (copyIfNull && target.getProgram() == null) {
            String defaultProgramLabel = getConfig().getVesselDefaultProgramLabel();
            ProgramVO defaultProgram =  StringUtils.isNotBlank(defaultProgramLabel) ? programRepository.getByLabel(defaultProgramLabel) : null;
            if (defaultProgram  != null && defaultProgram.getId() != null) {
                target.setProgram(getReference(Program.class, defaultProgram.getId()));
            }
        }
    }

    @Override
    protected void onAfterSaveEntity(VesselVO vo, Vessel savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
    }

    protected VesselVO toVO(VesselQueryResult source, DataFetchOptions fetchOptions, boolean copyIfNull) {
        VesselVO target = new VesselVO();

        toVO(source.getVessel(), target, DataFetchOptions.builder()
            .withRecorderDepartment(fetchOptions.isWithRecorderDepartment())
            .withRecorderPerson(fetchOptions.isWithRecorderPerson())
            .withChildrenEntities(false) // Do NOT fetch vesselFeatures and vesselRegistrationPeriod
            .build(), copyIfNull);

        if (fetchOptions.isWithChildrenEntities()) {
            // Vessel features
            target.setVesselFeatures(vesselFeaturesRepository.toVO(source.getVesselFeatures()));

            // Vessel registration period
            target.setVesselRegistrationPeriod(vesselRegistrationPeriodRepository.toVO(source.getVesselRegistrationPeriod()));
        }

        return target;
    }
}
