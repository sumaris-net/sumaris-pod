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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselRegistrationVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
@Repository("vesselDao")
public class VesselDaoImpl extends BaseDataDaoImpl implements VesselDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(VesselDaoImpl.class);

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private ReferentialDao referentialDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<VesselFeaturesVO> findByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselFeaturesResult> query = cb.createQuery(VesselFeaturesResult.class);
        Root<VesselFeatures> root = query.from(VesselFeatures.class);

        Join<VesselFeatures, Vessel> vesselJoin = root.join(VesselFeatures.Fields.VESSEL, JoinType.INNER);
        Join<Vessel, VesselRegistrationPeriod> vrpJoin = vesselJoin.join(Vessel.Fields.VESSEL_REGISTRATION_PERIODS, JoinType.LEFT);
        Join<VesselRegistrationPeriod, Location> registrationLocationJoin = vrpJoin.join(VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION, JoinType.LEFT);

        query.multiselect(root,
            vrpJoin.get(VesselRegistrationPeriod.Fields.REGISTRATION_CODE),
            vrpJoin.get(VesselRegistrationPeriod.Fields.START_DATE),
            vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE),
            registrationLocationJoin.as(Location.class)
        );

        // Apply sorting
        addSorting(query, cb, root, sortAttribute, sortDirection);

        // No tripFilter: execute request
        if (filter == null) {
            TypedQuery<VesselFeaturesResult> q = getEntityManager().createQuery(query)
                    .setFirstResult(offset)
                    .setMaxResults(size);
            return toVesselFeaturesVOs(q.getResultList());
        }

        List<Integer> statusIds = CollectionUtils.isEmpty(filter.getStatusIds())
            ? null
            : filter.getStatusIds();

        // Apply vessel Filter
        ParameterExpression<Date> dateParam = cb.parameter(Date.class);
        ParameterExpression<Integer> vesselIdParam = cb.parameter(Integer.class);
        ParameterExpression<Integer> vesselFeaturesIdParam = cb.parameter(Integer.class);
        ParameterExpression<String> searchNameParam = cb.parameter(String.class);
        ParameterExpression<String> searchExteriorMarkingParam = cb.parameter(String.class);
        ParameterExpression<String> searchRegistrationCodeParam = cb.parameter(String.class);
        ParameterExpression<Boolean> hasStatusIdsParam = cb.parameter(Boolean.class);
        ParameterExpression<Collection> statusIdsParam = cb.parameter(Collection.class);

        query.where(cb.and(
            // Filter: date
            cb.or(
                cb.and(
                    // if no date in filter, will return only active period
                    cb.isNull(dateParam),
                    cb.isNull(root.get(VesselFeatures.Fields.END_DATE)),
                    cb.isNull(vrpJoin.get(VesselRegistrationPeriod.Fields.END_DATE))
                ),
                cb.and(
                    cb.isNotNull(dateParam),
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
                    cb.like(cb.lower(root.get(VesselFeatures.Fields.NAME)), cb.lower(searchNameParam)),
                    cb.like(cb.lower(root.get(VesselFeatures.Fields.EXTERIOR_MARKING)), cb.lower(searchExteriorMarkingParam)),
                    cb.like(cb.lower(vrpJoin.get(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)), cb.lower(searchRegistrationCodeParam))
            ),

            // Status
            cb.or(
                    cb.isFalse(hasStatusIdsParam),
                    cb.in(vesselJoin.get(Vessel.Fields.STATUS).get(Status.Fields.ID)).value(statusIdsParam)
            )
        );


        String searchText = StringUtils.trimToNull(filter.getSearchText());
        String searchTextAsPrefix = null;
        if (StringUtils.isNotBlank(searchText)) {
            searchTextAsPrefix = (searchText + "*"); // add trailing escape char
            searchTextAsPrefix = searchTextAsPrefix.replaceAll("[*]+", "*"); // group escape chars
            searchTextAsPrefix = searchTextAsPrefix.replaceAll("[%]", "\\%"); // protected '%' chars
            searchTextAsPrefix = searchTextAsPrefix.replaceAll("[*]", "%"); // replace asterix
        }
        String searchTextAnyMatch = StringUtils.isNotBlank(searchTextAsPrefix) ? ("%"+searchTextAsPrefix) : null;

        TypedQuery<VesselFeaturesResult> q = entityManager.createQuery(query)
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
        List<VesselFeaturesResult> result = q.getResultList();
        return toVesselFeaturesVOs(result);
    }

    @Override
    public List<VesselFeaturesVO> getByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselFeatures> query = cb.createQuery(VesselFeatures.class);
        Root<VesselFeatures> root = query.from(VesselFeatures.class);
        query.select(root);

        // Apply filter
        ParameterExpression<Integer> vesselIdParam = cb.parameter(Integer.class);
        query.where(cb.equal(root.get(VesselFeatures.Fields.VESSEL).get(Vessel.Fields.ID), vesselIdParam));

        // Apply sorting
        addSorting(query, cb, root, sortAttribute, sortDirection);

        TypedQuery<VesselFeatures> q = entityManager.createQuery(query)
            .setParameter(vesselIdParam, vesselId)
            .setFirstResult(offset)
            .setMaxResults(size);
        List<VesselFeatures> result = q.getResultList();
        return result.stream().map(this::toVesselFeaturesVO).collect(Collectors.toList());
    }

    @Override
    public List<VesselRegistrationVO> getRegistrationsByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder(); //getEntityManager().getCriteriaBuilder();
        CriteriaQuery<VesselRegistrationPeriod> query = cb.createQuery(VesselRegistrationPeriod.class);
        Root<VesselRegistrationPeriod> root = query.from(VesselRegistrationPeriod.class);
        query.select(root);

        // Apply filter
        ParameterExpression<Integer> vesselIdParam = cb.parameter(Integer.class);
        query.where(cb.equal(root.get(VesselRegistrationPeriod.Fields.VESSEL).get(Vessel.Fields.ID), vesselIdParam));

        // Apply sorting
        addSorting(query, cb, root, sortAttribute, sortDirection);

        TypedQuery<VesselRegistrationPeriod> q = entityManager.createQuery(query)
            .setParameter(vesselIdParam, vesselId)
            .setFirstResult(offset)
            .setMaxResults(size);
        List<VesselRegistrationPeriod> result = q.getResultList();
        return result.stream().map(this::toVesselRegistrationVO).collect(Collectors.toList());
    }

    @Override
    public VesselFeaturesVO getByVesselIdAndDate(int vesselId, Date date) {
        VesselFilterVO filter = new VesselFilterVO();
        filter.setVesselId(vesselId);
        filter.setDate(date);
        List<VesselFeaturesVO> res = findByFilter(filter, 0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC);

        // No result for this date
        if (res.size() == 0) {
            // Retry using only vessel id (and limit to most recent features)
            filter.setDate(null);
            res = findByFilter(filter, 0, 1, VesselFeatures.Fields.START_DATE, SortDirection.DESC);
            if (res.size() == 0) {
                VesselFeaturesVO unknownVessel = new VesselFeaturesVO();
                unknownVessel.setVesselId(vesselId);
                unknownVessel.setName("unknown vessel " + vesselId);
                return unknownVessel;
            }
        }
        return res.get(0);
    }

    @Override
    public VesselFeaturesVO save(VesselFeaturesVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        VesselFeatures entity = null;
        if (source.getId() != null) {
            entity = get(VesselFeatures.class, source.getId());
        }
        boolean isNew = entity == null;
        boolean closeLastFeature = !isNew && !DateUtils.isSameDay(entity.getStartDate(), source.getStartDate());

        // if this feature have to be closed
        if (closeLastFeature) {
            Vessel vessel = entity.getVessel();
//            List<VesselPhysicalMeasurement> measurements = entity.getMeasurements();
            entity.setEndDate(DateUtils.addDays(source.getStartDate(), -1));
            entityManager.merge(entity);
            entityManager.flush();

            // create new feature with same vessel
            entity = new VesselFeatures();
            entity.setVessel(vessel);

//            TODO ? if (config.isPreserveHistoricalMeasurements() && CollectionUtils.isNotEmpty(measurements)) {
                // copy historical measurements

//            }
        }

        if (isNew) {
            entity = new VesselFeatures();
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            lockForUpdate(entity);
        }

        // VO -> Entity
        vesselFeaturesVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // If vessel not exists: create it
        if (entity.getVessel() == null) {
            Vessel vessel = new Vessel();
            vesselFeaturesVOToVesselEntity(source, vessel, true);
            vessel.setCreationDate(newUpdateDate);
            vessel.setUpdateDate(newUpdateDate);

            // Create the vessel
            entityManager.persist(vessel);
            // Link to the target entity
            source.setVesselId(vessel.getId());
            entity.setVessel(vessel);
        } else {
            Vessel vessel = entity.getVessel();
            vesselFeaturesVOToVesselEntity(source, vessel, true);
            vessel.setUpdateDate(newUpdateDate);
            entityManager.merge(vessel);
        }

        // Registration periods
        VesselRegistrationPeriod lastPeriod = CollectionUtils.emptyIfNull(entity.getVessel().getVesselRegistrationPeriods()).stream()
            .filter(period -> period.getEndDate() == null)
            .findFirst().orElse(null);
        boolean closeLastPeriod = closeLastFeature && lastPeriod != null
            && (!lastPeriod.getRegistrationCode().equals(source.getRegistrationCode()) || !lastPeriod.getRegistrationLocation().getId().equals(source.getRegistrationLocation().getId()));
        boolean createNewPeriod = lastPeriod == null || closeLastPeriod;

        if (closeLastPeriod) {
            // set end date the the day before the current start date TODO : the source.startDate must be after the current one : add control on page ? or here ?
            lastPeriod.setEndDate(DateUtils.addDays(source.getStartDate(), -1));
            entityManager.merge(lastPeriod);
        }
        if (createNewPeriod) {
            // create new period
            VesselRegistrationPeriod period = new VesselRegistrationPeriod();
            period.setVessel(entity.getVessel());
            period.setStartDate(source.getStartDate());
            period.setRegistrationCode(source.getRegistrationCode());
            period.setRegistrationLocation(get(Location.class, source.getRegistrationLocation().getId()));
            period.setRankOrder(1);
            entityManager.persist(period);
        } else {
            // update current period
            lastPeriod.setRegistrationCode(source.getRegistrationCode());
            lastPeriod.setRegistrationLocation(get(Location.class, source.getRegistrationLocation().getId()));
            entityManager.merge(lastPeriod);
        }

        // Save entity
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        // Get the entity
        VesselFeatures entity = get(VesselFeatures.class, id);

        boolean deleteParentVessel = CollectionUtils.size(entity.getVessel().getVesselFeatures()) == 1;

        // Vessel features will be deleted by cascade - see Vessel mapping
        if (deleteParentVessel) {

            log.debug(String.format("Deleting vessel {id=%s}...", entity.getVessel().getId()));
            delete(Vessel.class, entity.getVessel().getId());
        }
        else {

            log.debug(String.format("Deleting vessel features {id=%s}...", id));
            delete(VesselFeatures.class, id);
        }
    }

    @Override
    public VesselFeaturesVO toVesselFeaturesVO(VesselFeatures source) {
        if (source == null) return null;

        VesselFeaturesVO target = new VesselFeaturesVO();

        Beans.copyProperties(source, target);

        // Convert from cm to m
        if (source.getLengthOverAll() != null) {
            target.setLengthOverAll(source.getLengthOverAll().doubleValue() /100);
        }
        // Convert tonnage (divide by 100)
        if (source.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt(source.getGrossTonnageGrt().doubleValue() / 100);
        }
        if (source.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt(source.getGrossTonnageGt().doubleValue() / 100);
        }

        target.setVesselId(source.getVessel().getId());
        target.setVesselStatusId(source.getVessel().getStatus().getId());
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Vessel type
        ReferentialVO vesselType = referentialDao.toReferentialVO(source.getVessel().getVesselType());
        target.setVesselType(vesselType);

        // base port location
        LocationVO basePortLocation = locationDao.toLocationVO(source.getBasePortLocation());
        target.setBasePortLocation(basePortLocation);

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        return target;
    }

    /* -- protected methods -- */

    protected List<VesselFeaturesVO> toVesselFeaturesVOs(List<VesselFeaturesResult> source) {
        return source.stream()
                .map(this::toVesselFeaturesVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected VesselFeaturesVO toVesselFeaturesVO(VesselFeaturesResult source) {
        if (source == null)
            return null;

        // Vessel features
        VesselFeaturesVO target = toVesselFeaturesVO(source.getVesselFeatures());

        // Registration code
        target.setRegistrationCode(source.getRegistrationCode());

        // Registration dates
        target.setRegistrationStartDate(source.getRegistrationStartDate());
        target.setRegistrationEndDate(source.getRegistrationEndDate());

        // Registration location
        LocationVO registrationLocation = locationDao.toLocationVO(source.getRegistrationLocation());
        target.setRegistrationLocation(registrationLocation);

        return target;
    }

    private VesselRegistrationVO toVesselRegistrationVO(VesselRegistrationPeriod source) {
        if (source == null)
            return null;

        VesselRegistrationVO target = new VesselRegistrationVO();

        Beans.copyProperties(source, target);

        // Registration location
        LocationVO registrationLocation = locationDao.toLocationVO(source.getRegistrationLocation());
        target.setRegistrationLocation(registrationLocation);

        return target;
    }

    protected void vesselFeaturesVOToEntity(VesselFeaturesVO source, VesselFeatures target, boolean copyIfNull) {

        copyDataProperties(source, target, copyIfNull);

        // Recorder department and person
        copyRecorderPerson(source, target, copyIfNull);

        // Convert from meter to centimeter
        if (source.getLengthOverAll() != null) {
            target.setLengthOverAll((int)(source.getLengthOverAll().doubleValue()  * 100));
        }
        // Convert tonnage (x100)
        if (source.getGrossTonnageGrt() != null) {
            target.setGrossTonnageGrt((int)(source.getGrossTonnageGrt().doubleValue() * 100));
        }
        if (source.getGrossTonnageGt() != null) {
            target.setGrossTonnageGt((int)(source.getGrossTonnageGt().doubleValue() * 100));
        }

        // Vessel
        if (copyIfNull || source.getVesselId() != null) {
            if (source.getVesselId() == null) {
                target.setVessel(null);
            }
            else {
                target.setVessel(load(Vessel.class, source.getVesselId()));
            }
        }

        // Base port location
        if (copyIfNull || source.getBasePortLocation() != null) {
            if (source.getBasePortLocation() == null || source.getBasePortLocation().getId() == null) {
                target.setBasePortLocation(null);
            }
            else {
                target.setBasePortLocation(load(Location.class, source.getBasePortLocation().getId()));
            }
        }
    }

    protected void vesselFeaturesVOToVesselEntity(VesselFeaturesVO source, Vessel target, boolean copyIfNull) {

        // Copy properties from root EXCEPT id
        copyRootDataProperties(source, target, copyIfNull, VesselFeatures.Fields.ID);

        // Vessel type
        if (copyIfNull || source.getVesselType() != null) {
            if (source.getVesselType() == null) {
                target.setVesselType(null);
            }
            else {
                target.setVesselType(load(VesselType.class, source.getVesselType().getId()));
            }
        }

        // Vessel status
        if (copyIfNull || source.getVesselStatusId() != null) {
            if (source.getVesselStatusId() == null) {
                target.setStatus(null);
            }
            else {
                target.setStatus(load(Status.class, source.getVesselStatusId()));
            }
        }

        // Default program
        if (copyIfNull && target.getProgram() == null) {
            target.setProgram(load(Program.class, ProgramEnum.SIH.getId()));
        }
    }

    protected <T> CriteriaQuery<T> addSorting(CriteriaQuery<T> query,
                                              CriteriaBuilder cb,
                                              Root<?> root, String sortAttribute, SortDirection sortDirection) {
        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = root.get(sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                cb.desc(sortExpression) :
                cb.asc(sortExpression)
            );
        }
        return query;
    }

    @Data
    @AllArgsConstructor
    public static class VesselFeaturesResult {
        VesselFeatures vesselFeatures;
        String registrationCode;
        Date registrationStartDate;
        Date registrationEndDate;
        Location registrationLocation;
    }
}
