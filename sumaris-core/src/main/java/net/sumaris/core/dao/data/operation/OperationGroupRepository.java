package net.sumaris.core.dao.data.operation;

import net.sumaris.core.dao.data.DataRepository;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationGroupRepository
    extends DataRepository<Operation, OperationGroupVO, OperationGroupFilterVO, DataFetchOptions>, OperationGroupSpecifications {
}
