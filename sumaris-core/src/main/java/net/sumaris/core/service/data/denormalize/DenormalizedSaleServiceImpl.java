package net.sumaris.core.service.data.denormalize;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2024 SUMARiS Consortium
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.core.service.data.SaleService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.SaleFetchOptions;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.SaleFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("denormalizedSaleService")
@RequiredArgsConstructor
@Slf4j
public class DenormalizedSaleServiceImpl implements DenormalizedSaleService {

    private final DenormalizedBatchService denormalizedBatchService;

    private final SaleService saleService;

    private LoadingCache<Integer, DenormalizedBatchOptions> optionsByProgramIdCache;

    private LoadingCache<String, DenormalizedBatchOptions> optionsByProgramLabelCache;

    protected void init() {
        // Create a cache for denormalized options, by programId
        optionsByProgramIdCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5 min (if job is very long, the options will be reload)
                .build(CacheLoader.from(denormalizedBatchService::createOptionsByProgramId));

        // Create a cache for denormalized options, by programLabel
        optionsByProgramLabelCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5 min (if job is very long, the options will be reload)
                .build(CacheLoader.from(denormalizedBatchService::createOptionsByProgramLabel));

    }

    @Override
    public DenormalizedBatchOptions createOptionsByProgramId(int programId) {
        return optionsByProgramIdCache.getUnchecked(programId);
    }

    @Override
    public DenormalizedBatchOptions createOptionsByProgramLabel(String programLabel) {
        return optionsByProgramLabelCache.getUnchecked(programLabel);
    }

    public DenormalizedSaleResultVO denormalizeByFilter(@NonNull SaleFilterVO saleFilter,
                                                        @NonNull DenormalizedBatchOptions baseOptions) {
        long startTime = System.currentTimeMillis();
        MutableInt saleCount = new MutableInt(0);
        MutableInt batchesCount = new MutableInt(0);
        MutableInt invalidBatchesCount = new MutableInt(0);
        List<String> messages = Lists.newArrayList();

        saleFilter = saleFilter.clone();

        // Split operation ids, if too many
        Integer[] saleIds = saleFilter.getIncludedIds();
        if (ArrayUtils.getLength(saleIds) > 500) {
            int pageSize = 500;
            long pageCount = Math.round((double)(saleIds.length / pageSize) + 0.5); // Get page count
            for (int page = 0; page < pageCount; page++) {
                int from = page * pageSize;
                int to = Math.min(saleIds.length, from + pageSize);
                Integer[] pageSaleIds = Arrays.stream(Arrays.copyOfRange(saleIds, from, to))
                        .mapToInt(Number::intValue)
                        .boxed()
                        .toArray(Integer[]::new);

                saleFilter.setIncludedIds(pageSaleIds);

                // Loop on page
                DenormalizedSaleResultVO pageResult = denormalizeByFilter(saleFilter, baseOptions);

                // Update counters
                batchesCount.add(pageResult.getBatchCount());
                invalidBatchesCount.add(pageResult.getInvalidBatchCount());
                if (StringUtils.isNotBlank(pageResult.getMessage())) {
                    messages.add(pageResult.getMessage());
                }
            }
        }
        else {

            // Select only operation that should be update (if not force)
            saleFilter.setNeedBatchDenormalization(!baseOptions.isForce());

            // DEBUG - force denormalization
            //if (!this.production && !baseOptions.isForce()) operationFilter.setNeedBatchDenormalization(false);

            long saleTotal = saleService.countByFilter(saleFilter);

            boolean hasMoreData;
            int offset = 0;
            int pageSize = 10;

            if (saleTotal > 0) {
                do {
                    // Fetch some operations
                    List<SaleVO> sales = saleService.findAllByFilter(saleFilter,
                            offset, pageSize, // Page
                            SaleVO.Fields.ID, SortDirection.ASC, // Sort by id, to keep continuity between pages
                            SaleFetchOptions.builder()
                                    .withChildrenEntities(false)
                                    .withMeasurementValues(false)
                                    .withFishingAreas(true)
                                    .build());

                    sales.parallelStream().forEach(sale -> {
                        try {
                            // Prepare options (add fishing area, date, etc.)
                            DenormalizedBatchOptions options = createOptionsBySale(sale, baseOptions);

                            List<?> batches = denormalizedBatchService.denormalizeAndSaveBySaleId(sale.getId(), options);
                            batchesCount.add(CollectionUtils.size(batches));
                        } catch (SumarisBusinessException be) {
                            log.error(be.getMessage());
                            messages.add(be.getMessage());
                            invalidBatchesCount.increment();
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            messages.add(e.getMessage());
                            invalidBatchesCount.increment();
                        }
                    });

                    offset += pageSize;
                    saleCount.add(sales.size());
                    hasMoreData = sales.size() >= pageSize;
                    if (saleCount.intValue() > saleTotal) {
                        saleTotal = saleCount.intValue();
                    }
                } while (hasMoreData);
            }
        }

        return DenormalizedSaleResultVO.builder()
                .saleCount(saleCount.intValue())
                .batchCount(batchesCount.intValue())
                .invalidBatchCount(invalidBatchesCount.intValue())
                .message(CollectionUtils.isNotEmpty(messages) ? String.join("\n", messages) : null)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
    }
    @Override
    public DenormalizedBatchOptions createOptionsBySale(@NonNull SaleVO sale, @Nullable DenormalizedBatchOptions inheritedOptions) {
        if (inheritedOptions == null) {
            int programId = saleService.getProgramIdById(sale.getId());
            inheritedOptions = denormalizedBatchService.createOptionsByProgramId(programId);
        }

        DenormalizedBatchOptions options = inheritedOptions.clone(); // Copy, to keep original options unchanged
        options.setDateTime(Dates.resetTime(sale.getStartDateTime()));

        return options;
    }

}
