package net.sumaris.core.dao.data.sale;

import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.data.VesselSnapshotDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.SaleFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author peck7 on 01/09/2020.
 */
public class SaleRepositoryImpl
    extends RootDataRepositoryImpl<Sale, SaleVO, SaleFilterVO, DataFetchOptions>
    implements SaleSpecifications {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private VesselSnapshotDao vesselSnapshotDao;

    protected SaleRepositoryImpl(EntityManager entityManager) {
        super(Sale.class, SaleVO.class, entityManager);
    }

    @Override
    public void toVO(Sale source, SaleVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Sale location
        target.setSaleLocation(locationRepository.toVO(source.getSaleLocation()));

        // Sale type
        ReferentialVO saleType = referentialDao.toVO(source.getSaleType());
        target.setSaleType(saleType);

        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) { // default is false
            target.setVesselSnapshot(vesselSnapshotDao.getByIdAndDate(source.getVessel().getId(), source.getStartDateTime()));
            target.setQualityFlagId(source.getQualityFlag().getId());

            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);

            // Recorder person
            if (source.getRecorderPerson() != null) {
                PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson());
                target.setRecorderPerson(recorderPerson);
            }
        }
    }

    @Override
    public List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sales) {
        // Load parent entity
        Trip parent = getOne(Trip.class, tripId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getSales()));

        // Save each gears
        List<SaleVO> result = sales.stream().map(source -> {
            source.setTripId(tripId);
            source.setProgram(parentProgram);
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        return result;
    }

    @Override
    public void toEntity(SaleVO source, Sale target, boolean copyIfNull) {

        // copy some fields from trip
        TripVO trip = source.getTrip();
        if (trip != null) {
            source.setRecorderDepartment(trip.getRecorderDepartment());
            source.setRecorderPerson(trip.getRecorderPerson());
            source.setVesselSnapshot(trip.getVesselSnapshot());
            source.setQualityFlagId(trip.getQualityFlagId());
        }

        super.toEntity(source, target, copyIfNull);

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            }
            else {
                target.setTrip(load(Trip.class, tripId));
            }
        }

        // Sale location
        if (copyIfNull || source.getSaleLocation() != null) {
            if (source.getSaleLocation() == null || source.getSaleLocation().getId() == null) {
                target.setSaleLocation(null);
            }
            else {
                target.setSaleLocation(load(Location.class, source.getSaleLocation().getId()));
            }
        }

        // Sale type
        if (copyIfNull || source.getSaleType() != null) {
            if (source.getSaleType() == null || source.getSaleType().getId() == null) {
                target.setSaleType(null);
            }
            else {
                target.setSaleType(load(SaleType.class, source.getSaleType().getId()));
            }
        }
    }

    @Override
    protected Specification<Sale> toSpecification(SaleFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasTripId(filter.getTripId()));
    }

}
