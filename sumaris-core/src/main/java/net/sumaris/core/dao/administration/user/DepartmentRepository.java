package net.sumaris.core.dao.administration.user;

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;

/**
 * @author peck7 on 20/08/2020.
 */
public interface DepartmentRepository
    extends ReferentialRepository<Department, DepartmentVO, DepartmentFilterVO>, DepartmentRepositoryExtend {

}
