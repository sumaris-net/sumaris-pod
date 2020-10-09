package net.sumaris.core.dao.data.observedLocation;

import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 31/08/2020.
 */
public class ObservedLocationRepositoryImpl
    extends RootDataRepositoryImpl<ObservedLocation, ObservedLocationVO, ObservedLocationFilterVO, DataFetchOptions>
    implements ObservedLocationSpecifications {

    private static final Logger log = LoggerFactory.getLogger(ObservedLocationRepositoryImpl.class);

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private PersonRepository personRepository;

    protected ObservedLocationRepositoryImpl(EntityManager entityManager) {
        super(ObservedLocation.class, ObservedLocationVO.class, entityManager);
    }

    @Override
    protected Specification<ObservedLocation> toSpecification(ObservedLocationFilterVO filter) {
        return super.toSpecification(filter)
            .and(hasLocationId(filter.getLocationId()))
            .and(withStartDate(filter.getStartDate()))
            .and(withEndDate(filter.getEndDate()));
    }

    @Override
    public void toVO(ObservedLocation source, ObservedLocationVO target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Remove endDateTime if same as startDateTime
        if (target.getEndDateTime() != null && target.getEndDateTime().equals(target.getStartDateTime())) {
            target.setEndDateTime(null);
        }

        // Location
        target.setLocation(locationRepository.toVO(source.getLocation()));

        // Observers
        if ((fetchOptions == null || fetchOptions.isWithObservers()) && CollectionUtils.isNotEmpty(source.getObservers())) {
            Set<PersonVO> observers = source.getObservers().stream().map(personRepository::toVO).collect(Collectors.toSet());
            target.setObservers(observers);
        }

    }

    @Override
    public void toEntity(ObservedLocationVO source, ObservedLocation target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // If endDateTime is empty, fill using startDateTime
        if (target.getEndDateTime() == null) {
            target.setEndDateTime(target.getStartDateTime());
        }

        // Departure location
        if (copyIfNull || source.getLocation() != null) {
            if (source.getLocation() == null || source.getLocation().getId() == null) {
                target.setLocation(null);
            } else {
                target.setLocation(load(Location.class, source.getLocation().getId()));
            }
        }
    }

}
