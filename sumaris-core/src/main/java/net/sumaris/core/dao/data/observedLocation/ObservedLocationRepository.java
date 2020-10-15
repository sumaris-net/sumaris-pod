package net.sumaris.core.dao.data.observedLocation;

import net.sumaris.core.dao.data.RootDataRepository;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;

/**
 * @author peck7 on 31/08/2020.
 */
public interface ObservedLocationRepository
    extends RootDataRepository<ObservedLocation, ObservedLocationVO, ObservedLocationFilterVO, DataFetchOptions>,
    ObservedLocationSpecifications {

}
