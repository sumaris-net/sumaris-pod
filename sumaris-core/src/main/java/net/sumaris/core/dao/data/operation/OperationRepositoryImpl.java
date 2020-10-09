package net.sumaris.core.dao.data.operation;

import net.sumaris.core.dao.data.DataRepositoryImpl;
import net.sumaris.core.dao.data.physicalGear.PhysicalGearRepository;
import net.sumaris.core.dao.referential.metier.MetierRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.PhysicalGear;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author peck7 on 01/09/2020.
 */
public class OperationRepositoryImpl
    extends DataRepositoryImpl<Operation, OperationVO, OperationFilterVO, DataFetchOptions>
    implements OperationRepository {

    private static final Logger log = LoggerFactory.getLogger(OperationRepositoryImpl.class);

    @Autowired
    private PhysicalGearRepository physicalGearRepository;

    @Autowired
    private MetierRepository metierRepository;

    protected OperationRepositoryImpl(EntityManager entityManager) {
        super(Operation.class, OperationVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    public void toVO(Operation source, OperationVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
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

        // Métier
        if (source.getMetier() != null) {
            target.setMetier(metierRepository.toVO(source.getMetier()));
        }

    }

    @Override
    public List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> operations) {

        // Load parent entity
        Trip parent = getOne(Trip.class, tripId);

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getOperations()));

        // Save each operation
        List<OperationVO> result = operations.stream().map(source -> {
            source.setTripId(tripId);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        // Update the parent entity
        Daos.replaceEntities(parent.getOperations(),
            result,
            (vo) -> load(Operation.class, vo.getId()));

        return result;
    }

    @Override
    public void toEntity(OperationVO source, Operation target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            } else {
                // Use get() and NOT load(), because trip object will be used later (for physicalGears)
                target.setTrip(getOne(Trip.class, tripId));
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
        {
            // Read physical gear id
            Integer physicalGearId = source.getPhysicalGearId() != null
                ? source.getPhysicalGearId()
                : source.getPhysicalGear() != null ? source.getPhysicalGear().getId() : null;

            // If not found, try using the rankOrder
            if (physicalGearId == null && source.getPhysicalGear() != null && source.getPhysicalGear().getRankOrder() != null && target.getTrip() != null) {
                Integer rankOrder = source.getPhysicalGear().getRankOrder();
                physicalGearId = target.getTrip().getPhysicalGears()
                    .stream()
                    .filter(g -> rankOrder != null && Objects.equals(g.getRankOrder(), rankOrder))
                    .map(PhysicalGear::getId)
                    .findFirst().orElse(null);
                if (physicalGearId == null) {
                    throw new DataIntegrityViolationException(
                        String.format("Operation {starDateTime: '%s'} use a unknown PhysicalGear. PhysicalGear with {rankOrder: %s} not found in gears Trip.",
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

    @Override
    protected Specification<Operation> toSpecification(OperationFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasTripId(filter.getTripId()));
    }
}
