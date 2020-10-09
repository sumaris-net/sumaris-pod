package net.sumaris.core.dao.data.operation;

import net.sumaris.core.dao.data.DataRepository;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.filter.OperationFilterVO;

import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationRepository
    extends DataRepository<Operation, OperationVO, OperationFilterVO, DataFetchOptions>, OperationSpecifications {

    List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> operations);

}
