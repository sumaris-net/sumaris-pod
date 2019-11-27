package net.sumaris.core.extraction.service;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
