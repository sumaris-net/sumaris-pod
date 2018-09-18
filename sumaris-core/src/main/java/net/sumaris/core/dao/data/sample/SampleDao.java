package net.sumaris.core.dao.data.sample;

import net.sumaris.core.model.data.sample.Sample;
import net.sumaris.core.vo.data.SampleVO;

import java.util.List;

public interface SampleDao {

    List<SampleVO> getAllByOperationId(int operationId);

    SampleVO get(int id);

    /**
     * Save a sample
     * @param sample
     * @return
     */
    SampleVO save(SampleVO sample);

    SampleVO toSampleVO(Sample source);

    void delete(int id);
}
