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

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author peck7 on 17/12/2018.
 */
@Transactional
public interface ExtractionService {

    int EXECUTION_TIMEOUT = 10000000;

    @Transactional(timeout = -1)
    IExtractionType getByExample(IExtractionType source);
    @Transactional(timeout = -1)
    IExtractionType getByExample(IExtractionType source, ExtractionProductFetchOptions fetchOptions);

    @Transactional(timeout = EXECUTION_TIMEOUT, propagation = Propagation.REQUIRES_NEW)
    ExtractionContextVO execute(IExtractionType format, ExtractionFilterVO filter, AggregationStrataVO strata);

    @Transactional(timeout = EXECUTION_TIMEOUT, propagation = Propagation.REQUIRES_NEW)
    ExtractionProductVO executeAndSave(IExtractionType format,
                                       ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata);

    @Transactional(timeout = EXECUTION_TIMEOUT, propagation = Propagation.REQUIRES_NEW)
    ExtractionProductVO executeAndSave(int id);

    @Transactional(timeout = EXECUTION_TIMEOUT, propagation = Propagation.REQUIRES_NEW)
    ExtractionResultVO executeAndRead(IExtractionType type,
                                      @Nullable ExtractionFilterVO filter,
                                      @Nullable AggregationStrataVO strata,
                                      Page page,
                                      @Nullable CacheTTL ttl);

    ExtractionResultVO read(IExtractionType type,
                            @Nullable ExtractionFilterVO filter,
                            @Nullable AggregationStrataVO strata,
                            Page page,
                            @Nullable CacheTTL ttl);

    File executeAndDumpTrips(LiveExtractionTypeEnum format, ExtractionTripFilterVO filter);

    File executeAndDumpStrategies(LiveExtractionTypeEnum format, ExtractionStrategyFilterVO filter);

    @Transactional(rollbackFor = IOException.class)
    File executeAndDump(IExtractionType type, ExtractionFilterVO filter, AggregationStrataVO strata) throws IOException;

    File dumpTablesToFile(ExtractionContextVO context, @Nullable ExtractionFilterVO filter);

    void executeAll(ProcessingFrequencyEnum frequency);

    @Transactional(readOnly = true)
    AggregationTechResultVO readByTech(IExtractionType type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       String sort,
                                       SortDirection direction);

    @Transactional(readOnly = true)
    MinMaxVO getTechMinMax(IExtractionType type,
                           @Nullable ExtractionFilterVO filter,
                           @Nullable AggregationStrataVO strata);

    ExtractionFilterVO parseFilter(String jsonFilter);

    List<Map<String, String>> toListOfMap(ExtractionResultVO source);

    ObjectNode[] toJson(ExtractionResultVO source);

}
