package net.sumaris.core.dao.data.batch;

import net.sumaris.core.model.data.batch.Batch;
import net.sumaris.core.vo.data.BatchVO;
import net.sumaris.core.vo.data.SampleVO;

import java.util.List;

public interface BatchDao {

    List<BatchVO> getAllByOperationId(int operationId);

    BatchVO get(int id);

    List<BatchVO> saveByOperationId(int operationId, List<BatchVO> sources);

    /**
     * Save a Batch
     * @param Batch
     * @return
     */
    BatchVO save(BatchVO Batch);

    BatchVO toBatchVO(Batch source);

    List<BatchVO> toFlatList(BatchVO source);

    void delete(int id);
}
