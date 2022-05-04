package net.sumaris.extraction.core.service;

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

import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.filter.ExtractionTypeFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author peck7 on 17/12/2018.
 */
@Transactional
public interface ExtractionService {


    @Transactional(readOnly = true)
    ExtractionTypeVO getByFormat(IExtractionFormat type);

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> findAll();

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> findAll(@Nullable ExtractionTypeFilterVO filter, Page page);

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> getLiveExtractionTypes();

    ExtractionContextVO execute(IExtractionFormat format, @Nullable ExtractionFilterVO filter);

    ExtractionResultVO read(ExtractionContextVO context,
                            @Nullable ExtractionFilterVO filter,
                            Page page) ;

    ExtractionResultVO executeAndRead(ExtractionTypeVO type,
                                      @Nullable ExtractionFilterVO filter,
                                      Page page);

    ExtractionResultVO executeAndReadWithCache(@NonNull ExtractionTypeVO type,
                                               @Nullable ExtractionFilterVO filter,
                                               @NonNull Page page,
                                               @Nullable CacheTTL ttl);

    @Transactional(rollbackFor = IOException.class)
    File executeAndDump(ExtractionTypeVO type,
                        ExtractionFilterVO filter) throws IOException;

    File executeAndDumpTrips(LiveFormatEnum format, ExtractionTripFilterVO filter);

    File executeAndDumpStrategies(LiveFormatEnum format, ExtractionStrategyFilterVO filter);

    File dumpTablesToFile(ExtractionContextVO context, @Nullable ExtractionFilterVO filter);

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void clean(ExtractionContextVO context);

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    @Async
    CompletableFuture<Boolean> asyncClean(ExtractionContextVO context);

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    ExtractionProductVO toProductVO(ExtractionContextVO context);

    ExtractionTypeVO save(ExtractionTypeVO type, ExtractionFilterVO filter);
}
