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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.cache.CacheTTL;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The extraction manager.
 * @author peck7 on 17/12/2018.
 */
@Service
// Do NOT implements @Transactional (transaction is managed by Dispatcher classes)
public interface ExtractionService {

    @Transactional(timeout = -1)
    IExtractionType getByExample(IExtractionType source);
    @Transactional(timeout = -1)
    IExtractionType getByExample(IExtractionType source, ExtractionProductFetchOptions fetchOptions);

    ExtractionContextVO execute(IExtractionType format, ExtractionFilterVO filter, AggregationStrataVO strata);

    ExtractionProductVO executeAndSave(IExtractionType format,
                                       ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata);

    ExtractionProductVO executeAndSave(int id);

    ExtractionResultVO executeAndRead(IExtractionType type,
                                      @Nullable ExtractionFilterVO filter,
                                      @Nullable AggregationStrataVO strata,
                                      Page page,
                                      @Nullable CacheTTL ttl);

    Map<String, ExtractionResultVO> executeAndReadMany(IExtractionType type,
                                          @NonNull ExtractionFilterVO filter,
                                          @Nullable AggregationStrataVO strata,
                                          Page page,
                                          @Nullable CacheTTL ttl);

    File executeAndDump(IExtractionType type, ExtractionFilterVO filter, AggregationStrataVO strata) throws IOException;

    File executeAndDumpTrips(LiveExtractionTypeEnum format, ExtractionTripFilterVO filter);

    File executeAndDumpStrategies(LiveExtractionTypeEnum format, ExtractionStrategyFilterVO filter);

    File executeAndDumpVessels(LiveExtractionTypeEnum format, VesselFilterVO filter);

    ExtractionResultVO executeAndReadStrategies(LiveExtractionTypeEnum format, ExtractionStrategyFilterVO filter, Page page);

    ExtractionResultVO read(IExtractionType type,
                            @Nullable ExtractionFilterVO filter,
                            @Nullable AggregationStrataVO strata,
                            Page page,
                            @Nullable CacheTTL ttl);

    /**
     * Read and return many sheets
     * @param type
     * @param filter
     * @param strata
     * @param page
     * @param ttl
     * @return
     */
    Map<String, ExtractionResultVO> readMany(IExtractionType type,
                                            @NonNull ExtractionFilterVO filter,
                                            @Nullable AggregationStrataVO strata,
                                            Page page,
                                            @Nullable CacheTTL ttl);

    File dumpTablesToFile(ExtractionContextVO context, @Nullable ExtractionFilterVO filter);

    void executeAll(ProcessingFrequencyEnum frequency);

    AggregationTechResultVO readByTech(IExtractionType type,
                                       @Nullable ExtractionFilterVO filter,
                                       @Nullable AggregationStrataVO strata,
                                       String sort,
                                       SortDirection direction);

    MinMaxVO getTechMinMax(IExtractionType type,
                           @Nullable ExtractionFilterVO filter,
                           @Nullable AggregationStrataVO strata);

    @Transactional
    int dropTemporaryTables();

    ExtractionFilterVO parseFilter(String jsonFilter);

    List<Map<String, String>> toListOfMap(ExtractionResultVO source);

    ArrayNode toJsonArray(ExtractionResultVO source);

    ObjectNode toJsonMap(Map<String, ExtractionResultVO> source);

}
