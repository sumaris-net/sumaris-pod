package net.sumaris.core.extraction.service;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.ExtractionContextVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.ExtractionResultVO;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.springframework.transaction.annotation.Propagation;
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
    List<ExtractionTypeVO> getAllExtractionTypes();

    @Transactional
    ExtractionContextVO execute(ExtractionTypeVO type, ExtractionFilterVO filter);

    @Transactional
    ExtractionResultVO read(ExtractionContextVO context,
                            ExtractionFilterVO filter,
                            int offset,
                            int size,
                            String sort,
                            SortDirection direction) ;

    @Transactional
    ExtractionResultVO executeAndRead(ExtractionTypeVO type,
                                      ExtractionFilterVO filter,
                                      int offset,
                                      int size,
                                      String sort,
                                      SortDirection direction) ;

    @Transactional(rollbackFor = IOException.class)
    File executeAndDump(ExtractionTypeVO type,
                        ExtractionFilterVO filter) throws IOException;

    @Transactional
    File executeAndDumpTrips(ExtractionRawFormatEnum format, ExtractionTripFilterVO filter);

    @Transactional
    void clean(ExtractionContextVO context);

    @Transactional(propagation = Propagation.SUPPORTS)
    void asyncClean(ExtractionContextVO context);

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    ExtractionProductVO toProductVO(ExtractionContextVO context);

}
