package net.sumaris.core.dao.referential.pmfm;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;

/**
 * @author peck7 on 19/08/2020.
 */
public interface ParameterRepository
    extends ReferentialRepository<Parameter, ParameterVO, ReferentialFilterVO, ReferentialFetchOptions> {
}
