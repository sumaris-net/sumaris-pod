package net.sumaris.core.dao.data.operation;

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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.fishingArea.FishingAreaRepository;
import net.sumaris.core.dao.data.physicalGear.PhysicalGearRepository;
import net.sumaris.core.dao.data.product.ProductRepository;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.filter.ProductFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.criteria.ParameterExpression;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author peck7 on 01/09/2020.
 */
@Slf4j
public class OperationGroupRepositoryImpl
    extends DataRepositoryImpl<Operation, OperationGroupVO, OperationGroupFilterVO, DataFetchOptions>
    implements OperationGroupRepository {

    @Autowired
    private MetierRepository metierRepository;

    @Autowired
    private PhysicalGearRepository physicalGearRepository;

    @Autowired
    private ProductRepository productRepository;

    //@Autowired
    //private PacketService packetService;

    @Autowired
    private FishingAreaRepository fishingAreaRepository;

    @Autowired
    private MeasurementDao measurementDao;


    protected OperationGroupRepositoryImpl(EntityManager entityManager) {
        super(Operation.class, OperationGroupVO.class, entityManager);
        setLockForUpdate(false);
        setCheckUpdateDate(false);
    }

    @Override
    protected Specification<Operation> toSpecification(OperationGroupFilterVO filter, DataFetchOptions fetchOptions) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getTripId());
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Operation.Fields.RANK_ORDER_ON_PERIOD))); // Default sort
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, OperationGroupVO.Fields.TRIP_ID);
            if (filter.isOnlyUndefined()) {
                return criteriaBuilder.and(
                        criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param),
                        criteriaBuilder.equal(root.get(Operation.Fields.START_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.DEPARTURE_DATE_TIME)),
                        criteriaBuilder.equal(root.get(Operation.Fields.END_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.RETURN_DATE_TIME))
                );
            } else if (filter.isOnlyDefined()) {
                return criteriaBuilder.and(
                        criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param),
                        criteriaBuilder.notEqual(root.get(Operation.Fields.START_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.DEPARTURE_DATE_TIME))
                );
            } else {
                return criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param);
            }
        });
        specification.addBind(OperationGroupVO.Fields.TRIP_ID, filter.getTripId());
        return specification;
    }

    @Override
    public void toVO(Operation source, OperationGroupVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Trip
        if (source.getTrip() != null) {
            target.setTripId(source.getTrip().getId());
        }

        // Physical gear
        if (source.getPhysicalGear() != null) {
            target.setPhysicalGearId(source.getPhysicalGear().getId());
        }

        // Metier
        if (source.getMetier() != null) {
            target.setMetier(metierRepository.toVO(source.getMetier()));
        }

        // Children entities (not loaded by default)
        Integer operationId = source.getId();
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities() && operationId != null) {

            // Products
            target.setProducts(productRepository.findAll(ProductFilterVO.builder().operationId(operationId).build()));

            // Fishing Areas
            target.setFishingAreas(fishingAreaRepository.getAllByOperationId(operationId));

            // Packets
            // TODO
            //target.setPackets(packetService.getAllByOperationId(operationId));

            // Measurements
            target.setMeasurements(measurementDao.getOperationVesselUseMeasurements(operationId));
            target.setGearMeasurements(measurementDao.getOperationGearUseMeasurements(operationId));
        }

    }

    @Override
    public List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> operationGroups) {
        Preconditions.checkNotNull(operationGroups);

        // Remember existing entities
        final List<OperationGroupVO> existingOperationGroups = findAll(OperationGroupFilterVO.builder().tripId(tripId).onlyDefined(true).build());
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(existingOperationGroups);

        // Save each operation group
        List<OperationGroupVO> result = operationGroups.stream().map(operationGroup -> {
            operationGroup.setTripId(tripId);
            if (operationGroup.getId() != null) {
                sourcesIdsToRemove.remove(operationGroup.getId());
            }
            return save(operationGroup);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        return result;
    }

    @Override
    public void toEntity(OperationGroupVO source, Operation target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        Trip trip = getById(Trip.class, tripId);
        target.setTrip(trip);

        // Recorder department (copy from parent if missing)
        if (copyIfNull || source.getRecorderDepartment() != null || trip.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                if (trip.getRecorderDepartment() != null) {
                    target.setRecorderDepartment(trip.getRecorderDepartment());
                } else {
                    target.setRecorderDepartment(null); // should not happened
                }
            } else {
                target.setRecorderDepartment(getReference(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Metier
        if (copyIfNull || source.getMetier() != null) {
            if (source.getMetier() == null || source.getMetier().getId() == null) {
                target.setMetier(null);
            } else {
                target.setMetier(getReference(Metier.class, source.getMetier().getId()));
            }
        }

        // Physical gear
        if (copyIfNull || source.getPhysicalGearId() != null) {
            if (source.getPhysicalGearId() == null) {
                target.setPhysicalGear(null);
            } else {
                target.setPhysicalGear(getReference(PhysicalGear.class, source.getPhysicalGearId()));
            }
        }

        // Affect date time
        target.setStartDateTime(
            source.isUndefined()
                ? trip.getDepartureDateTime()
                : DateUtils.addSeconds(trip.getDepartureDateTime(), 1)
        );
        target.setEndDateTime(trip.getReturnDateTime());
        target.setFishingStartDateTime(trip.getDepartureDateTime());
        target.setFishingEndDateTime(trip.getReturnDateTime());

    }

    @Override
    public OperationGroupVO getMainUndefinedOperationGroup(int tripId) {
        List<OperationGroupVO> operationGroups = findAll(
                OperationGroupFilterVO.builder().tripId(tripId).onlyUndefined(true).build()
        );
        // Get the first (main ?) undefined operation group
        // todo maybe add is_main_operation and manage metier order in app
        if (CollectionUtils.size(operationGroups) > 0) {
            return operationGroups.get(0);
        }
        return null;
    }

    @Override
    public void updateUndefinedOperationDates(int tripId, Date startDateTime, Date endDateTime) {
        // Fix IMAGINE-561: to NOT use a "UPDATE (...) where id IN (SELECT ... )"
        // Prefer using a "SELECT id FROM Operation", then an "UPDATE where id IN (ids)"
        EntityManager em = getEntityManager();

        // Get the parent trip, to be able to use old start/end dates, to find the undefined operation
        Trip parentTrip = getById(Trip.class, tripId);
        Date previousStartDateTime = parentTrip.getDepartureDateTime();
        Date previousEndDateTime = parentTrip.getReturnDateTime();

        // Check if update need
        if (Dates.equals(previousStartDateTime, startDateTime)
            && Dates.equals(previousEndDateTime, endDateTime)) {
            return; // no changes in dates: skip update
        }

        // Get undefined operation ids
        List<Integer> undefinedOperationIds = em.createNamedQuery("Operation.selectUndefinedOperationIds", Integer.class)
            .setParameter("tripId", tripId)
            .setParameter("startDateTime", previousStartDateTime)
            .setParameter("endDateTime", previousEndDateTime)
            .getResultList();

        // No undefined operations: skip
        if (CollectionUtils.isEmpty(undefinedOperationIds)) return;

        int nbRowUpdated = getEntityManager()
            .createNamedQuery("Operation.updateUndefinedOperationDates")
            .setParameter("ids", undefinedOperationIds)
            .setParameter("startDateTime", startDateTime)
            .setParameter("endDateTime", endDateTime)
            .executeUpdate();

        if (log.isDebugEnabled() && nbRowUpdated > 0) {
            log.debug(String.format("%s undefined operations updated for trip is=%s", nbRowUpdated, tripId));
        }

        // This is need to make sure fetched operations will have updated dates
        getEntityManager().flush();
        getEntityManager().clear();
    }

    @Override
    public List<MetierVO> getMetiersByTripId(int tripId) {
        return findAll(
            OperationGroupFilterVO.builder().tripId(tripId).onlyUndefined(true).build(),
            0,
            1000,
            Operation.Fields.RANK_ORDER_ON_PERIOD,
            SortDirection.ASC,
            null
        ).stream()
            .map(OperationGroupVO::getMetier)
            .collect(Collectors.toList());
    }

    @Override
    public List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers) {
        Preconditions.checkNotNull(metiers);

        // Remember existing entities
        final List<OperationGroupVO> existingOperationGroups = findAll(OperationGroupFilterVO.builder().tripId(tripId).onlyUndefined(true).build());
        final Map<Integer, OperationGroupVO> groupsByMetierId = Beans.splitByProperty(
            existingOperationGroups,
            StringUtils.doting(OperationGroupVO.Fields.METIER, ReferentialVO.Fields.ID)
        );

        // Save each operation group
        metiers.stream()
            .filter(s -> s != null && s.getId() != null)
            .forEach(source -> {
                OperationGroupVO operationGroup = groupsByMetierId.remove(source.getId());
                if (operationGroup == null) {
                    // create new undefined operation group
                    operationGroup = new OperationGroupVO();
                }
                operationGroup.setUndefined(true);
                operationGroup.setTripId(tripId);
                operationGroup.setMetier(source);
                // save it
                save(operationGroup);
            });

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(groupsByMetierId.values())) {
            groupsByMetierId.values().stream().map(OperationGroupVO::getId).forEach(this::deleteById);
        }

        return metiers;
    }
}
