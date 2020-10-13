package net.sumaris.core.dao.data.operation;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.physicalGear.PhysicalGearRepository;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author peck7 on 01/09/2020.
 */
public class OperationGroupRepositoryImpl
    extends DataRepositoryImpl<Operation, OperationGroupVO, OperationGroupFilterVO, DataFetchOptions>
    implements OperationGroupRepository {

    private static final Logger log = LoggerFactory.getLogger(OperationGroupRepositoryImpl.class);

    @Autowired
    private MetierRepository metierRepository;

    @Autowired
    private PhysicalGearRepository physicalGearRepository;

    protected OperationGroupRepositoryImpl(EntityManager entityManager) {
        super(Operation.class, OperationGroupVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    protected Specification<Operation> toSpecification(OperationGroupFilterVO filter) {
        return super.toSpecification(filter)
            .and(filter(filter));
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
            target.setPhysicalGear(physicalGearRepository.toVO(source.getPhysicalGear(), DataFetchOptions.builder().withRecorderDepartment(false).build()));
        }

        // Metier
        if (source.getMetier() != null) {
            target.setMetier(metierRepository.toVO(source.getMetier()));
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
        Trip trip = getOne(Trip.class, tripId);
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
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
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
                    operationGroup.setTripId(tripId);
                    operationGroup.setUndefined(true);
                    operationGroup.setMetier(source);
                }
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
