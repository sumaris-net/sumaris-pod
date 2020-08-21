package net.sumaris.core.dao.referential.location;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;

/**
 * @author peck7 on 18/08/2020.
 */
public interface LocationRepository
    extends ReferentialRepository<Location, LocationVO, ReferentialFilterVO, ReferentialFetchOptions>,
    LocationRepositoryExtend {

}
