package net.sumaris.core.service.extraction;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.extraction.ExtractionTypeVO;
import net.sumaris.core.vo.extraction.ExtractionFilterVO;
import net.sumaris.core.vo.extraction.ExtractionResultVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ExtractionService {

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> getAllTypes();

    @Transactional(readOnly = true)
    ExtractionResultVO getRows( ExtractionTypeVO type,
                                ExtractionFilterVO filter,
                                int offset,
                                int size,
                                String sort,
                                SortDirection direction) ;
}
