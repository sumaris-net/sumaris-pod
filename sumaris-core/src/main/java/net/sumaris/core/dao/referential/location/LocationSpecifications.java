package net.sumaris.core.dao.referential.location;

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.model.referential.location.Location;

/**
 * @author peck7 on 18/08/2020.
 */
public interface LocationSpecifications extends ReferentialSpecifications<Location> {

    boolean hasAssociation(int childLocationId, int parentLocationId);

    void addAssociation(int childLocationId, int parentLocationId, double childSurfaceRatio);

    /**
     * Update technical table LOCATION_HIERARCHY, from child/parent links found in LOCATION
     */
    void updateLocationHierarchy();

}
