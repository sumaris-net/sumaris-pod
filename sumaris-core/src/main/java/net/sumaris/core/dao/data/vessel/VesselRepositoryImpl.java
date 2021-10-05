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
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class VesselRepositoryImpl
    extends RootDataRepositoryImpl<Vessel, VesselVO, VesselFilterVO, VesselFetchOptions>
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
    public List<VesselVO> findAll(VesselFilterVO filter, net.sumaris.core.dao.technical.Page page,
                                          VesselFetchOptions fetchOptions) {

        CriteriaBuilder builder = this.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = builder.createTupleQuery();

        Root<Vessel> root = criteriaQuery.from(Vessel.class);
        Join<Vessel, VesselFeatures> featuresJoin = root.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);

        boolean fetchRegistrationPeriod = filter.getStartDate() != null || filter.getEndDate() != null;
        if (fetchRegistrationPeriod) {
            Join<Vessel, VesselRegistrationPeriod> vrpJoin = root.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);
            criteriaQuery.multiselect(root, featuresJoin, vrpJoin);
        }
        else {
            criteriaQuery.multiselect(root, featuresJoin);
        }
        criteriaQuery.distinct(true);

        // Apply specification
        Specification<Vessel> spec = toSpecification(filter, fetchOptions);
        Predicate predicate = spec.toPredicate(root, criteriaQuery, builder);
        if (predicate != null) criteriaQuery.where(predicate);

        // Add sorting
        addSorting(criteriaQuery, builder, root, page.getSortBy(), page.getSortDirection());

        TypedQuery<Tuple> query = getEntityManager().createQuery(criteriaQuery);

        // Bind parameters
        applyBindings(query, spec);

        // Set Limit
        query.setFirstResult((int)page.getOffset());
        query.setMaxResults(page.getSize());

        return streamQuery(query)
            .map(tuple -> toVO(tuple, fetchRegistrationPeriod, fetchOptions, true))
            .collect(Collectors.toList());
    }

    @Override
    public Specification<Vessel> toSpecification(VesselFilterVO filter, VesselFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            // by ID
            .and(id(filter.getVesselId()))
            .and(vesselFeaturesId(filter.getVesselFeaturesId()))
            // by locations
            .and(registrationLocationId(filter.getRegistrationLocationId()))
            .and(basePortLocationId(filter.getBasePortLocationId()))
            // by status
            .and(statusIds(filter.getStatusIds()))
            // by type
            .and(vesselTypeId(filter.getVesselTypeId()))
            // By period or single date
            .and(betweenFeaturesDate(filter.getStartDate(), filter.getEndDate()))
            .and(betweenRegistrationDate(filter.getStartDate(), filter.getEndDate()))
            // By text
            .and(searchText(filter.getSearchAttributes(), filter.getSearchText()))
            // Quality
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            ;
    }

    @Override
    public void toVO(Vessel source, VesselVO target, VesselFetchOptions fetchOptions, boolean copyIfNull) {
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

        // Vessel features
        if (fetchOptions == null || fetchOptions.isWithVesselFeatures()) {
            VesselFeaturesVO features = vesselFeaturesRepository.getLastByVesselId(source.getId(), DataFetchOptions.MINIMAL).orElse(null);
            if (copyIfNull || features != null) {
                target.setVesselFeatures(features);
            }
        }

        // Vessel registration period
        if (fetchOptions == null || fetchOptions.isWithVesselFeatures()) {
            VesselRegistrationPeriodVO period = vesselRegistrationPeriodRepository.getLastByVesselId(source.getId()).orElse(null);
            if (copyIfNull || period != null) {
                target.setVesselRegistrationPeriod(period);
            }
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

    protected VesselVO toVO(Tuple source,
                            boolean hasRegistrationPeriodTuple,
                            VesselFetchOptions fetchOptions, boolean copyIfNull) {
        VesselVO target = new VesselVO();

        Vessel srcVessel = source.get(0, Vessel.class);
        toVO(srcVessel, target, VesselFetchOptions.builder()
            .withRecorderDepartment(fetchOptions != null && fetchOptions.isWithRecorderDepartment())
            .withRecorderPerson(fetchOptions != null && fetchOptions.isWithRecorderPerson())
            // Do NOT fetch vesselFeatures and vesselRegistrationPeriod
            .withVesselFeatures(false)
            .withVesselRegistrationPeriod(!hasRegistrationPeriodTuple && fetchOptions != null && fetchOptions.isWithVesselRegistrationPeriod())
            .build(), copyIfNull);

        // Vessel features
        if (fetchOptions == null || fetchOptions.isWithVesselFeatures()) {
            VesselFeatures sourceFeatures = source.get(1, VesselFeatures.class);
            if (copyIfNull || sourceFeatures != null) {
                target.setVesselFeatures(vesselFeaturesRepository.toVO(sourceFeatures));
            }
        }

        // Vessel registration period
        if (hasRegistrationPeriodTuple && (fetchOptions == null || fetchOptions.isWithVesselRegistrationPeriod() && fetchOptions != null)) {
            VesselRegistrationPeriod sourceRegistrationPeriod = source.get(2, VesselRegistrationPeriod.class);
            if (copyIfNull || sourceRegistrationPeriod != null) {
                target.setVesselRegistrationPeriod(vesselRegistrationPeriodRepository.toVO(sourceRegistrationPeriod));
            }
        }

        return target;
    }
}
