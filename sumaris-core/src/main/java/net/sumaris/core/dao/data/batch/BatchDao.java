package net.sumaris.core.dao.data.batch;

import net.sumaris.core.model.data.batch.Batch;
import net.sumaris.core.vo.data.BatchVO;

public interface BatchDao {

    BatchVO get(int id);

    /**
     * Save a Batch
     * @param Batch
     * @return
     */
    BatchVO save(BatchVO Batch);

    BatchVO toBatchVO(Batch source);

    void delete(int id);
}
