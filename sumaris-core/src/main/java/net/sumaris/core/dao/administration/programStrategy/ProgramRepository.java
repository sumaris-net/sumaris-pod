package net.sumaris.core.dao.administration.programStrategy;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;

/**
 * @author peck7 on 24/08/2020.
 */
public interface ProgramRepository
    extends ReferentialRepository<Program, ProgramVO, ProgramFilterVO, ProgramFetchOptions>,
    ProgramRepositoryExtend {

}
