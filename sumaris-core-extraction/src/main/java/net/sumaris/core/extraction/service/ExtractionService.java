package net.sumaris.core.extraction.service;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.ExtractionContextVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.live.trip.ExtractionTripFilterVO;
import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 */
@Transactional
public interface ExtractionService {


    @Transactional(readOnly = true)
    List<ExtractionTypeVO> getAllTypes();

    @Transactional
    ExtractionResultVO getRows(ExtractionTypeVO type,
                               ExtractionFilterVO filter,
                               int offset,
                               int size,
                               String sort,
                               SortDirection direction) ;

    @Transactional(rollbackFor = IOException.class)
    File getFile(ExtractionTypeVO type,
                 ExtractionFilterVO filter) throws IOException;

    @Transactional
    File getFile(ExtractionLiveFormat format, ExtractionTripFilterVO filter);

    @Transactional
    void clean(ExtractionContextVO context);

}
