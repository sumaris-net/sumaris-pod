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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository("operationGroupDao")
public class OperationGroupDaoImpl extends BaseDataDaoImpl implements OperationGroupDao {

    /**
     * Logger.
     */
    private static final Logger log =
        LoggerFactory.getLogger(OperationGroupDaoImpl.class);

    @Autowired
    private MetierRepository metierDao;

    @Autowired
    private PhysicalGearRepository physicalGearRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Override
    public List<MetierVO> getMetiersByTripId(int tripId) {

        return getOperationGroupsByTripId(tripId, OperationGroupFilter.UNDEFINED).stream()
            .map(OperationGroupVO::getMetier)
            .collect(Collectors.toList());

    }

    @Override
    public List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> sources) {
        Preconditions.checkNotNull(sources);

        // Load parent entity
        Trip parent = get(Trip.class, tripId);

        // Remember existing entities
        final List<OperationGroupVO> existingOperationGroups = getOperationGroupsByTripId(tripId, OperationGroupFilter.UNDEFINED);
        final Map<Integer, OperationGroupVO> groupsByMetierId = Beans.splitByProperty(existingOperationGroups, OperationGroupVO.Fields.METIER + '.' + ReferentialVO.Fields.ID);

        // Save each operation group
        for (MetierVO source : sources) {
            OperationGroupVO operationGroup = groupsByMetierId.remove(source.getId());
            Operation entity;
            boolean isNew = false;
            if (operationGroup == null) {
                // create new undefined operation group
                operationGroup = new OperationGroupVO();
                operationGroup.setMetier(source);
                entity = new Operation();
                isNew = true;
            } else {
                entity = get(Operation.class, operationGroup.getId());
            }

            if (!isNew) {
                // Check update date
                checkUpdateDateForUpdate(operationGroup, entity);
                // Lock entityName
                lockForUpdate(entity);
            }

            // VO to entity
            undefinedOperationGroupVOToEntity(operationGroup, entity, parent, true);

            // Update update_dt
            Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
            entity.setUpdateDate(newUpdateDate);

            // Save operation
            if (isNew) {
                // Force id to null, to use the generator
                entity.setId(null);
                getEntityManager().persist(entity);
            } else {
                getEntityManager().merge(entity);
            }

        }

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(groupsByMetierId.values())) {
            groupsByMetierId.values().stream().map(OperationGroupVO::getId).forEach(this::delete);
        }

        return sources;
    }

    @Override
    public void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate) {

        int nbRowUpdated = getEntityManager()
            .createNamedQuery("Operation.updateUndefinedOperationDates")
            .setParameter("tripId", tripId)
            .setParameter("startDateTime", startDate)
            .setParameter("endDateTime", endDate)
            .executeUpdate();

        if (log.isDebugEnabled() && nbRowUpdated > 0) {
            log.debug(String.format("%s undefined operations updated for trip is=%s", nbRowUpdated, tripId));
        }

        // This is need to make sure next load will have the good dates
        getEntityManager().flush();
        getEntityManager().clear();
    }

    @Override
    public List<OperationGroupVO> getAllByTripId(int tripId, int offset, int size, String sortAttribute, SortDirection sortDirection) {

        return getOperationGroupsByTripId(tripId, OperationGroupFilter.DEFINED, offset, size, sortAttribute, sortDirection);
    }

    @Override
    public List<OperationGroupVO> getAllByTripId(int tripId) {

        return getOperationGroupsByTripId(tripId, OperationGroupFilter.DEFINED);
    }

    @Override
    public OperationGroupVO get(int id) {
        return toOperationGroupVO(get(Operation.class, id));
    }

    @Override
    public void delete(int id) {
        log.debug(String.format("Deleting operation group {id=%s}...", id));
        delete(Operation.class, id);
    }

    @Override
    public OperationGroupVO save(OperationGroupVO source) {
        Preconditions.checkNotNull(source);
        // Load parent entity
        Integer tripId = source.getTripId() != null ? source.getTripId() : source.getTrip() != null ? source.getTrip().getId() : null;
        if (tripId == null) {
            throw new SumarisTechnicalException("Cannot save an operation group a trip id");
        }
        Trip parent = get(Trip.class, tripId);

        // Save with parent entity
        return save(source, parent);
    }

    @Override
    public List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> sources) {

        Preconditions.checkNotNull(sources);

        // Load parent entity
        Trip parent = get(Trip.class, tripId);

        // Remember existing entities
        final List<OperationGroupVO> existingOperationGroups = getOperationGroupsByTripId(tripId, OperationGroupFilter.DEFINED);
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(existingOperationGroups);

        // Save each operation group
        List<OperationGroupVO> result = sources.stream().map(source -> {
            source.setTripId(tripId);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source, parent);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::delete);
        }

        return result;
    }

    private List<OperationGroupVO> getOperationGroupsByTripId(int tripId, OperationGroupFilter filter) {
        return getOperationGroupsByTripId(tripId, filter, 0, 1000, Operation.Fields.RANK_ORDER_ON_PERIOD, SortDirection.ASC);
    }

    private List<OperationGroupVO> getOperationGroupsByTripId(int tripId, OperationGroupFilter filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Operation> query = builder.createQuery(Operation.class);
        Root<Operation> root = query.from(Operation.class);
        Join<Operation, Trip> tripJoin = root.join(Operation.Fields.TRIP, JoinType.INNER);

        ParameterExpression<Integer> tripIdParam = builder.parameter(Integer.class);

        Predicate predicate = null;
        switch (filter) {
            case UNDEFINED:
                predicate = builder.and(
                    builder.equal(tripJoin.get(Trip.Fields.ID), tripIdParam),
                    builder.equal(root.get(Operation.Fields.START_DATE_TIME), tripJoin.get(Trip.Fields.DEPARTURE_DATE_TIME)),
                    builder.equal(root.get(Operation.Fields.END_DATE_TIME), tripJoin.get(Trip.Fields.RETURN_DATE_TIME))
                );
                break;
            case DEFINED:
                predicate = builder.and(
                    builder.equal(tripJoin.get(Trip.Fields.ID), tripIdParam),
                    builder.notEqual(root.get(Operation.Fields.START_DATE_TIME), tripJoin.get(Trip.Fields.DEPARTURE_DATE_TIME))
                );
                break;
            case ALL:
                predicate = builder.equal(tripJoin.get(Trip.Fields.ID), tripIdParam);
        }
        query.select(root).where(predicate);

        // Add sorting
        addSorting(query, builder, root, sortAttribute, sortDirection);

        TypedQuery<Operation> q = entityManager.createQuery(query)
            .setParameter(tripIdParam, tripId)
            .setFirstResult(offset)
            .setMaxResults(size);
        return toOperationGroupVOs(q.getResultList());
    }

    private OperationGroupVO save(OperationGroupVO source, Trip parent) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(parent);

        Operation entity = null;
        if (source.getId() != null) {
            entity = get(Operation.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Operation();
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);
            // Lock entityName
            lockForUpdate(entity);
        }

        // VO -> Entity
        definedOperationGroupVOToEntity(source, entity, parent, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force id to null, to use the generator
            entity.setId(null);

            getEntityManager().persist(entity);
            source.setId(entity.getId());
        } else {
            getEntityManager().merge(entity);
        }

        source.setUpdateDate(newUpdateDate);
        source.setTripId(parent.getId());

        return source;
    }

    private void undefinedOperationGroupVOToEntity(OperationGroupVO source, Operation target, Trip parent, boolean copyIfNull) {

        operationGroupVOToEntity(source, target, parent, copyIfNull);

        // Affect date time
        target.setStartDateTime(parent.getDepartureDateTime());
        target.setEndDateTime(parent.getReturnDateTime());
        target.setFishingStartDateTime(parent.getDepartureDateTime());
        target.setFishingEndDateTime(parent.getReturnDateTime());

    }

    private void definedOperationGroupVOToEntity(OperationGroupVO source, Operation target, Trip parent, boolean copyIfNull) {

        operationGroupVOToEntity(source, target, parent, copyIfNull);

        // Affect date time
        target.setStartDateTime(DateUtils.addSeconds(parent.getDepartureDateTime(), 1));
        target.setEndDateTime(parent.getReturnDateTime());
        target.setFishingStartDateTime(parent.getDepartureDateTime());
        target.setFishingEndDateTime(parent.getReturnDateTime());

    }

    private void operationGroupVOToEntity(OperationGroupVO source, Operation target, Trip parent, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Trip
        if (copyIfNull || !Objects.equals(parent, target.getTrip())) {
            target.setTrip(parent);
        }

        // Recorder department (copy from parent if missing)
        if (copyIfNull || source.getRecorderDepartment() != null || parent.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                if (parent.getRecorderDepartment() != null) {
                    target.setRecorderDepartment(parent.getRecorderDepartment());
                } else {
                    target.setRecorderDepartment(null); // should not happened
                }
            } else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
                source.setQualityFlagId(config.getDefaultQualityFlagId());
            } else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Métier
        if (copyIfNull || source.getMetier() != null) {
            if (source.getMetier() == null || source.getMetier().getId() == null) {
                target.setMetier(null);
            } else {
                target.setMetier(load(Metier.class, source.getMetier().getId()));
            }
        }

        // Physical gear
        if (copyIfNull || source.getPhysicalGearId() != null || (source.getPhysicalGear() != null && source.getPhysicalGear().getId() != null)) {
            Integer physicalGearId = source.getPhysicalGearId() != null ? source.getPhysicalGearId() : (
                source.getPhysicalGear() != null ? source.getPhysicalGear().getId() : null
            );
            if (physicalGearId == null) {
                target.setPhysicalGear(null);
            } else {
                target.setPhysicalGear(load(PhysicalGear.class, physicalGearId));
            }
        }
    }

    private List<MetierVO> toMetierVOs(List<Metier> metiers) {
        if (CollectionUtils.isEmpty(metiers))
            return new ArrayList<>();

        return metiers.stream().map(metier -> metierDao.toVO(metier)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<OperationGroupVO> toOperationGroupVOs(List<Operation> operations) {
        if (CollectionUtils.isEmpty(operations))
            return new ArrayList<>();

        return operations.stream().map(this::toOperationGroupVO).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private OperationGroupVO toOperationGroupVO(Operation source) {
        if (source == null) return null;

        OperationGroupVO target = new OperationGroupVO();

        Beans.copyProperties(source, target);

        // Trip
        if (source.getTrip() != null) {
            target.setTripId(source.getTrip().getId());
        }

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Physical gear
        if (source.getPhysicalGear() != null) {
            target.setPhysicalGearId(source.getPhysicalGear().getId());
            target.setPhysicalGear(physicalGearRepository.toVO(source.getPhysicalGear(), DataFetchOptions.builder().withRecorderDepartment(false).build()));
        }

        // Metier
        if (source.getMetier() != null) {
            target.setMetier(metierDao.toVO(source.getMetier()));
        }

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        return target;
    }

}
