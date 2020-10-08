package net.sumaris.core.dao.data.sample;

import net.sumaris.core.dao.data.RootDataRepository;
import net.sumaris.core.model.data.Sample;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.SampleVO;
import net.sumaris.core.vo.filter.SampleFilterVO;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SampleRepository
    extends RootDataRepository<Sample, SampleVO, SampleFilterVO, DataFetchOptions>, SampleSpecifications {

}
