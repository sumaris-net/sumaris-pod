package net.sumaris.core.extraction.service;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 */
@Transactional
public interface ExtractionService {

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> findByFilter(@Nullable ExtractionTypeFilterVO filter);

    @Transactional
    ExtractionContextVO execute(ExtractionTypeVO type, @Nullable ExtractionFilterVO filter);

    @Transactional
    ExtractionResultVO read(ExtractionContextVO context,
                            @Nullable ExtractionFilterVO filter,
                            int offset,
                            int size,
                            String sort,
                            SortDirection direction) ;

    @Transactional
    ExtractionResultVO executeAndRead(ExtractionTypeVO type,
                                      @Nullable ExtractionFilterVO filter,
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

    @Transactional
    ExtractionTypeVO save(ExtractionTypeVO type, ExtractionFilterVO filter);
}
