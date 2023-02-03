package net.sumaris.core.dao.data;

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

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("vesselDao")
public class VesselDaoImpl extends HibernateDaoSupport implements VesselDao {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private ProgramRepository programRepository;

    @Override
    public VesselVO get(int id) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselResult> query = builder.createQuery(VesselResult.class);

        Root<Vessel> vesselRoot = query.from(Vessel.class);
        Join<Vessel, VesselFeatures> featuresJoin = vesselRoot.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
        Join<Vessel, VesselRegistrationPeriod> vrpJoin = vesselRoot.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

        query.multiselect(vesselRoot, featuresJoin, vrpJoin);

        // filter by active features and registration
        query.where(builder.and(
            builder.equal(vesselRoot.get(Vessel.Fields.ID), id),
            builder.isNull(featuresJoin.get(VesselFeatures.Fields.END_DATE)),
            builder.isNull(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE))
        ));

        TypedQuery<VesselResult> q = getEntityManager().createQuery(query);
        VesselResult result = q.getSingleResult();

        // TODO: maybe remove filter if no result
        return toVesselVO(result);
    }

    @Override
    public List<VesselVO> findByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselResult> query = builder.createQuery(VesselResult.class);
        Root<Vessel> vesselRoot = query.from(Vessel.class);
        Join<Vessel, VesselFeatures> featuresJoin = vesselRoot.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
        Join<Vessel, VesselRegistrationPeriod> vrpJoin = vesselRoot.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

        // Select 3 entities
        query.multiselect(vesselRoot, featuresJoin, vrpJoin);

        // Apply sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            // replace some field aliases
            sortAttribute = sortAttribute.replaceFirst(VesselVO.Fields.VESSEL_FEATURES, Vessel.Fields.VESSEL_FEATURES);
            sortAttribute = sortAttribute.replaceFirst(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, Vessel.Fields.VESSEL_REGISTRATION_PERIODS);
            sortAttribute = sortAttribute.replaceFirst(VesselVO.Fields.STATUS_ID, StringUtils.doting(Vessel.Fields.STATUS, Status.Fields.ID));
        }
        addSorting(query, builder, vesselRoot, sortAttribute, sortDirection);

        // Create query
        TypedQuery<VesselResult> typedQuery = createVesselQuery(builder, query, vesselRoot, featuresJoin, vrpJoin, filter)
            .setFirstResult(offset)
            .setMaxResults(size);
        return toVesselVOs(typedQuery.getResultList());

    }

    @Override
    public Long countByFilter(VesselFilterVO filter) {

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<Vessel> root = query.from(Vessel.class);
        Join<Vessel, VesselFeatures> featuresJoin = root.join(Vessel.Fields.VESSEL_FEATURES, JoinType.LEFT);
        Join<Vessel, VesselRegistrationPeriod> vrpJoin = root.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);

        query.select(builder.count(root));

        return createVesselQuery(builder, query, root, featuresJoin, vrpJoin, filter).getSingleResult();
    }

    @Override
    public List<VesselFeaturesVO> getFeaturesByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselFeatures> query = builder.createQuery(VesselFeatures.class);
        Root<VesselFeatures> root = query.from(VesselFeatures.class);
        query.select(root);

        // Apply filter
        ParameterExpression<Integer> vesselIdParam = builder.parameter(Integer.class);
        query.where(builder.equal(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.ID), vesselIdParam));

        // Apply sorting
        addSorting(query, builder, root, sortAttribute, sortDirection);

        TypedQuery<VesselFeatures> q = getEntityManager().createQuery(query)
            .setParameter(vesselIdParam, vesselId)
            .setFirstResult(offset)
            .setMaxResults(size);
        List<VesselFeatures> result = q.getResultList();
        return result.stream().map(this::toVesselFeaturesVO).collect(Collectors.toList());
    }

    @Override
    public List<VesselRegistrationPeriodVO> getRegistrationsByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselRegistrationPeriod> query = builder.createQuery(VesselRegistrationPeriod.class);
        Root<VesselRegistrationPeriod> root = query.from(VesselRegistrationPeriod.class);
        query.select(root);

        // Apply filter
        ParameterExpression<Integer> vesselIdParam = builder.parameter(Integer.class);
        query.where(builder.equal(root.get(VesselRegistrationPeriod.Fields.VESSEL).get(Vessel.Fields.ID), vesselIdParam));

        // Apply sorting
        addSorting(query, builder, root, sortAttribute, sortDirection);

        TypedQuery<VesselRegistrationPeriod> q = getEntityManager().createQuery(query)
            .setParameter(vesselIdParam, vesselId)
            .setFirstResult(offset)
            .setMaxResults(size);
        List<VesselRegistrationPeriod> result = q.getResultList();
        return result.stream().map(this::toVesselRegistrationVO).collect(Collectors.toList());
    }

    @Override
    public VesselVO save(VesselVO source, boolean checkUpdateDate) {
        Preconditions.checkNotNull(source);

        Vessel target = null;
        if (source.getId() != null) {
            target = find(Vessel.class, source.getId());
        }
        boolean isNew = target == null;

        if (isNew) {
            target = new Vessel();
        }

        if (!isNew) {

            if (checkUpdateDate) {
                // Check update date
                Daos.checkUpdateDateForUpdate(source, target);
            }

            // Lock entityName
            lockForUpdate(target);
        }

        // VO -> Entity
        vesselVOToEntity(source, target, true);

        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        if (isNew) {
            // Force creation date
            target.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);
        }

        // Update update_dt
        target.setUpdateDate(newUpdateDate);
        source.setUpdateDate(newUpdateDate);

        // Save entity
        if (isNew) {
            getEntityManager().persist(target);
            source.setId(target.getId());
        } else {
            getEntityManager().merge(target);
        }

        // Save Vessel features
        VesselFeaturesVO features = source.getVesselFeatures();
        if (features != null) {
            if (features.getId() == null) {
                // New features
                VesselFeatures featuresEntity = new VesselFeatures();
                vesselFeaturesVOToEntity(features, featuresEntity, false);
                featuresEntity.setCreationDate(newUpdateDate);
                featuresEntity.setUpdateDate(newUpdateDate);
                // Affect the vessel
                featuresEntity.setVessel(target);
                // Create new entity
                getEntityManager().persist(featuresEntity);
                // Get new Id
                features.setId(featuresEntity.getId());
            } else {
                // Update features
                VesselFeatures featuresEntity = getById(VesselFeatures.class, features.getId());
                lockForUpdate(featuresEntity);
                vesselFeaturesVOToEntity(features, featuresEntity, true);
                featuresEntity.setUpdateDate(newUpdateDate);
                // Update entity
                getEntityManager().merge(featuresEntity);
            }
            // update source feature update also
            features.setUpdateDate(newUpdateDate);
        }

        // Save Registration period
        VesselRegistrationPeriodVO registration = source.getVesselRegistrationPeriod();
        if (registration != null) {
            if (registration.getId() == null) {
                // New period
                VesselRegistrationPeriod periodEntity = new VesselRegistrationPeriod();
                vesselRegistrationPeriodVOToEntity(registration, periodEntity, false);
                // Affect Vessel
                periodEntity.setVessel(target);
                // Create new entity
                getEntityManager().persist(periodEntity);
                // Get new Id
                source.getVesselRegistrationPeriod().setId(periodEntity.getId());
            } else {
                // Update period
                VesselRegistrationPeriod registrationEntity = getById(VesselRegistrationPeriod.class, registration.getId());
                lockForUpdate(registrationEntity);
                vesselRegistrationPeriodVOToEntity(registration, registrationEntity, true);
                // Update entity
                getEntityManager().merge(registrationEntity);
            }
        }



        getEntityManager().flush();
        getEntityManager().clear();

        return source;
    }

    @Override
    public void delete(int id) {

        // Get the entity
        Vessel entity = find(Vessel.class, id);
        if (entity == null) throw new DataRetrievalFailureException(String.format("Vessel with id %s not exists", id));

        delete(entity);
    }

    /* -- protected methods -- */

    private <R> TypedQuery<R> createVesselQuery(CriteriaBuilder cb, CriteriaQuery<R> query,
                                                Root<Vessel> vesselRoot,
                                                Join<Vessel, VesselFeatures> featuresJoin,
                                                Join<Vessel, VesselRegistrationPeriod> vrpJoin,
                                                VesselFilterVO filter) {

        if (filter != null) {
            Join<Vessel, Program> programJoin = vesselRoot.join(Vessel.Fields.PROGRAM, JoinType.INNER);

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
                        cb.isNull(featuresJoin.get(VesselFeatures.Fields.END_DATE)),
                        cb.isNull(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE))
                    ),
                    cb.and(
                        cb.isNotNull(dateParam.as(String.class)),
                        cb.and(
                            cb.or(
                                cb.isNull(featuresJoin.get(VesselFeatures.Fields.END_DATE)),
                                cb.greaterThan(featuresJoin.get(VesselFeatures.Fields.END_DATE), dateParam)
                            ),
                            cb.lessThan(featuresJoin.get(VesselFeatures.Fields.START_DATE), dateParam)
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
                    cb.equal(featuresJoin.get(VesselFeatures.Fields.ID), vesselFeaturesIdParam)
                ),

                // Filter: vessel id
                cb.or(
                    cb.isNull(vesselIdParam),
                    cb.equal(vesselRoot.get(Vessel.Fields.ID), vesselIdParam))
                ),

                // Filter: search text (on exterior marking OR id)
                cb.or(
                    cb.isNull(searchNameParam),
                    cb.like(cb.lower(featuresJoin.get(VesselFeatures.Fields.NAME)), searchNameParam, Daos.LIKE_ESCAPE_CHAR),
                    cb.like(cb.lower(featuresJoin.get(VesselFeatures.Fields.EXTERIOR_MARKING)), searchExteriorMarkingParam, Daos.LIKE_ESCAPE_CHAR),
                    cb.like(cb.lower(vrpJoin.get(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)), searchRegistrationCodeParam, Daos.LIKE_ESCAPE_CHAR)
                ),

                // Status
                cb.or(
                    cb.isFalse(hasStatusIdsParam),
                    cb.in(vesselRoot.get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(statusIdsParam)
                )
            );

            String searchTextAsPrefix = Daos.getEscapedSearchText(filter.getSearchText());
            searchTextAsPrefix = searchTextAsPrefix != null ? searchTextAsPrefix.toLowerCase() : null;
            String searchTextAnyMatch = StringUtils.isNotBlank(searchTextAsPrefix) ? ("%" + searchTextAsPrefix) : null;

            List<Integer> statusIds = CollectionUtils.isEmpty(filter.getStatusIds())
                ? null
                : filter.getStatusIds();

            return getEntityManager().createQuery(query)
                .setParameter(programParam, filter.getProgramLabel())
                .setParameter(dateParam, filter.getDate())
                .setParameter(vesselFeaturesIdParam, filter.getVesselFeaturesId())
                .setParameter(vesselIdParam, filter.getVesselId())
                .setParameter(searchExteriorMarkingParam, searchTextAsPrefix)
                .setParameter(searchRegistrationCodeParam, searchTextAsPrefix)
                .setParameter(searchNameParam, searchTextAnyMatch)
                .setParameter(hasStatusIdsParam, CollectionUtils.isNotEmpty(statusIds))
                .setParameter(statusIdsParam, statusIds);

        } else {

            // if no date in filter, will return only active period
            query.where(
                cb.and(
                    cb.isNull(featuresJoin.get(VesselFeatures.Fields.END_DATE)),
                    cb.isNull(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE))
                )
            );

            return getEntityManager().createQuery(query);

        }
    }

    private List<VesselVO> toVesselVOs(List<VesselResult> source) {
        return source.stream()
            .map(this::toVesselVO)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private VesselVO toVesselVO(VesselResult source) {
        if (source == null)
            return null;

        VesselVO target = new VesselVO();
        Beans.copyProperties(source.getVessel(), target);

        // Status
        target.setStatusId(source.getVessel().getStatus().getId());

        // Vessel type
        ReferentialVO vesselType = referentialDao.toVO(source.getVessel().getVesselType());
        target.setVesselType(vesselType);

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getVessel().getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        // Vessel features
        target.setVesselFeatures(toVesselFeaturesVO(source.getVesselFeatures()));

        // Vessel registration period
        target.setVesselRegistrationPeriod(toVesselRegistrationVO(source.getVesselRegistrationPeriod()));

        return target;
    }

    private VesselFeaturesVO toVesselFeaturesVO(VesselFeatures source) {
        if (source == null) return null;

        VesselFeaturesVO target = new VesselFeaturesVO();

        Beans.copyProperties(source, target);

        // Convert from cm to m
        if (source.getLengthOverAll() != null) {
            target.setLengthOverAll(source.getLengthOverAll().doubleValue() / 100);
        }
        // Convert tonnage (divide by 100)
        if (source.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt(source.getGrossTonnageGrt().doubleValue() / 100);
        }
        if (source.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt(source.getGrossTonnageGt().doubleValue() / 100);
        }

        target.setQualityFlagId(source.getQualityFlag().getId());

        // Hull material
        if (source.getHullMaterial() != null) {
            ReferentialVO hullMaterial = referentialDao.toVO(source.getHullMaterial());
            target.setHullMaterial(hullMaterial);
        }
        else {
            target.setHullMaterial(null);
        }

        // base port location
        LocationVO basePortLocation = locationRepository.toVO(source.getBasePortLocation());
        target.setBasePortLocation(basePortLocation);

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        return target;
    }

    private VesselRegistrationPeriodVO toVesselRegistrationVO(VesselRegistrationPeriod source) {
        if (source == null)
            return null;

        VesselRegistrationPeriodVO target = new VesselRegistrationPeriodVO();

        Beans.copyProperties(source, target);

        // Registration location
        LocationVO registrationLocation = locationRepository.toVO(source.getRegistrationLocation());
        target.setRegistrationLocation(registrationLocation);

        return target;
    }

    private void vesselVOToEntity(VesselVO source, Vessel target, boolean copyIfNull) {

        DataDaos.copyDataProperties(getEntityManager(), source, target, copyIfNull);

        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

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
            String defaultProgramLabel = ProgramEnum.SIH.getLabel(); //getConfig().getVesselDefaultProgramLabel();
            ProgramVO defaultProgram =  StringUtils.isNotBlank(defaultProgramLabel) ? programRepository.getByLabel(defaultProgramLabel) : null;
            if (defaultProgram  != null && defaultProgram.getId() != null) {
                target.setProgram(getReference(Program.class, defaultProgram.getId()));
            }
        }
    }

    private void vesselFeaturesVOToEntity(VesselFeaturesVO source, VesselFeatures target, boolean copyIfNull) {

        DataDaos.copyDataProperties(getEntityManager(), source, target, copyIfNull);

        // Recorder department and person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);

        // Convert from meter to centimeter
        if (source.getLengthOverAll() != null) {
            target.setLengthOverAll((int) (source.getLengthOverAll() * 100));
        }
        // Convert tonnage (x100)
        if (source.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt((int) (source.getGrossTonnageGrt() * 100));
        }
        if (source.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt((int) (source.getGrossTonnageGt() * 100));
        }

        // Hull material
        if (copyIfNull || source.getHullMaterial() != null) {
            if (source.getHullMaterial() == null || source.getHullMaterial().getId() == null) {
                target.setHullMaterial(null);
            } else {
                target.setHullMaterial(getReference(QualitativeValue.class, source.getHullMaterial().getId()));
            }
        }

        // Base port location
        if (copyIfNull || source.getBasePortLocation() != null) {
            if (source.getBasePortLocation() == null || source.getBasePortLocation().getId() == null) {
                target.setBasePortLocation(null);
            } else {
                target.setBasePortLocation(getReference(Location.class, source.getBasePortLocation().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(null);
            } else {
                target.setQualityFlag(getReference(QualityFlag.class, source.getQualityFlagId()));
            }
        }
        else if (copyIfNull) {
            // Set default
            target.setQualityFlag(getReference(QualityFlag.class, QualityFlagEnum.NOT_QUALIFIED.getId()));
        }
    }

    private void vesselRegistrationPeriodVOToEntity(VesselRegistrationPeriodVO source, VesselRegistrationPeriod target, boolean copyIfNull) {

        // Registration start date
        if (copyIfNull || source.getStartDate() != null) {
            target.setStartDate(source.getStartDate());
        }

        // Registration end date
        if (copyIfNull || source.getEndDate() != null) {
            target.setEndDate(source.getEndDate());
        }

        // Registration code
        if (copyIfNull || source.getRegistrationCode() != null) {
            target.setRegistrationCode(source.getRegistrationCode());
        }

        // Registration location
        if (copyIfNull || source.getRegistrationLocation() != null) {
            if (source.getRegistrationLocation() == null || source.getRegistrationLocation().getId() == null) {
                target.setRegistrationLocation(null);
            } else {
                target.setRegistrationLocation(getReference(Location.class, source.getRegistrationLocation().getId()));
            }
        }

        // default quality flag
        if (target.getQualityFlag() == null) {
            target.setQualityFlag(getReference(QualityFlag.class, SumarisConfiguration.getInstance().getDefaultQualityFlagId()));
        }

        // default rank order
        if (target.getRankOrder() == null) {
            target.setRankOrder(1);
        }
    }

    @Data
    @AllArgsConstructor
    private static class VesselResult {
        private Vessel vessel;
        private VesselFeatures vesselFeatures;
        private VesselRegistrationPeriod vesselRegistrationPeriod;
    }
}
