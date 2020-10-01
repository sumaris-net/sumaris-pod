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
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("operationDao")
public class OperationDaoImpl extends BaseDataDaoImpl implements OperationDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(OperationDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PhysicalGearRepository physicalGearRepository;

    @Autowired
    private MetierRepository metierDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<OperationVO> getAllByTripId(int tripId, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(size > 0);

        // Fetch locations
        //getEntityManager().enableFetchProfile("with-location");

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Operation> query = builder.createQuery(Operation.class);
        Root<Operation> root = query.from(Operation.class);

        ParameterExpression<Integer> operationIdParam = builder.parameter(Integer.class);

        query.select(root)
            .where(builder.equal(root.get(Operation.Fields.TRIP).get(IReferentialWithStatusEntity.Fields.ID), operationIdParam));

        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = root.get(sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                    builder.desc(sortExpression) :
                    builder.asc(sortExpression)
            );
        }

        TypedQuery<Operation> q = entityManager.createQuery(query)
                .setParameter(operationIdParam, tripId)
                .setFirstResult(offset)
                .setMaxResults(size);
        return toVOs(q.getResultList());
    }

    @Override
    public Long countByTripId(int tripId) {
        return getEntityManager()
                .createNamedQuery("Operation.countByTripId", Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
    }

    @Override
    public OperationVO get(int id) {
        Operation entity = get(Operation.class, id);
        return toVO(entity);
    }

    @Override
    public List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> sources) {


        // Load parent entity
        Trip parent = get(Trip.class, tripId);

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getOperations()));

        // Save each operation
        List<OperationVO> result = sources.stream().map(source -> {
            source.setTripId(tripId);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::delete);
        }

        // Update the parent entity
        Daos.replaceEntities(parent.getOperations(),
                result,
                (vo) -> load(Operation.class, vo.getId()));

        return result;
    }

    @Override
    public OperationVO save(OperationVO source) {
        Preconditions.checkNotNull(source);

        EntityManager session = getEntityManager();
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
        toEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force id to null, to use the generator
            entity.setId(null);

            session.persist(entity);
            source.setId(entity.getId());
        } else {
            session.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        //session.flush();
        //session.clear();

        return source;
    }

    @Override
    public void delete(int id) {
        log.debug(String.format("Deleting operation {id=%s}...", id));
        delete(Operation.class, id);
    }

    @Override
    public OperationVO toVO(Operation source) {
        if (source == null) return null;

        OperationVO target = new OperationVO();

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

        // Métier
        if (source.getMetier() != null) {
            target.setMetier(metierDao.toVO(source.getMetier()));
        }

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        return target;
    }

    /* -- protected methods -- */

    protected List<OperationVO> toVOs(List<Operation> source) {
        return source.stream()
                .map(this::toVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void toEntity(OperationVO source, Operation target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            }
            else {
                target.setTrip(get(Trip.class, tripId)); // Use a GET, because trip will be used later, for physicalGears
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
                source.setQualityFlagId(config.getDefaultQualityFlagId());
            }
            else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Métier
        if (copyIfNull || source.getMetier() != null) {
            if (source.getMetier() == null || source.getMetier().getId() == null) {
                target.setMetier(null);
            }
            else {
                target.setMetier(load(Metier.class, source.getMetier().getId()));
            }
        }

        // Physical gear
        {
            // Read physical gear id
            Integer physicalGearId = source.getPhysicalGearId() != null ? source.getPhysicalGearId() : (
                    source.getPhysicalGear() != null ? source.getPhysicalGear().getId() : null);

            // If not found, try using the rankOrder
            if (physicalGearId == null && source.getPhysicalGear() != null && source.getPhysicalGear().getRankOrder() != null && target.getTrip() != null) {
                Integer rankOrder = source.getPhysicalGear().getRankOrder();
                physicalGearId = target.getTrip().getPhysicalGears()
                        .stream()
                        .filter(g -> rankOrder != null && Objects.equals(g.getRankOrder(), rankOrder))
                        .map(PhysicalGear::getId)
                        .findFirst().orElse(null);
                if (physicalGearId == null) {
                    throw new DataIntegrityViolationException(String.format("Operation {starDateTime: '%s'} use a unknown PhysicalGear. PhysicalGear with {rankOrder: %s} not found in gears Trip.",
                            Dates.toISODateTimeString(source.getStartDateTime()),
                            source.getPhysicalGear().getRankOrder()
                    ));
                }
                source.setPhysicalGearId(physicalGearId);
                source.setPhysicalGear(null);
            }

            if (copyIfNull || physicalGearId != null) {
                if (physicalGearId == null) {
                    target.setPhysicalGear(null);
                } else {
                    target.setPhysicalGear(load(PhysicalGear.class, physicalGearId));
                }
            }
        }
    }
}
