package net.sumaris.core.dao.referential.location;

import net.sumaris.core.model.referential.location.LocationClassification;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author peck7 on 18/08/2020.
 */
public interface LocationClassificationRepository
    extends JpaRepository<LocationClassification, Integer> {

    LocationClassification getByLabel(String label);
}
