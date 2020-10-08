package net.sumaris.core.dao.referential.location;

import net.sumaris.core.model.referential.location.LocationLevel;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author peck7 on 18/08/2020.
 */
public interface LocationLevelRepository
    extends JpaRepository<LocationLevel, Integer> {

    LocationLevel findByLabel(String label);
}
