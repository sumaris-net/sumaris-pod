package net.sumaris.core.service.data.denormalize;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import net.sumaris.core.dao.data.Positions;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.FishingAreaVO;
import net.sumaris.core.vo.data.OperationFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.OperationFilterVO;
import net.sumaris.core.vo.referential.location.LocationVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("denormalizeOperationService")
@RequiredArgsConstructor
@Slf4j
public class DenormalizeOperationServiceImpl implements DenormalizedOperationService {

    private final DenormalizedBatchService denormalizedBatchService;

    private final OperationService operationService;

    private final LocationService locationService;

    // Create a cache for denormalized options, by programId
    private LoadingCache<Integer, DenormalizedBatchOptions> optionsByProgramIdCache;

    private LoadingCache<String, DenormalizedBatchOptions> optionsByProgramLabelCache;

    @PostConstruct
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

    @Override
    public DenormalizedTripResultVO denormalizeByFilter(@NonNull OperationFilterVO operationFilter,
                                                        @NonNull DenormalizedBatchOptions baseOptions) {
        long startTime = System.currentTimeMillis();
        MutableInt operationCount = new MutableInt(0);
        MutableInt batchesCount = new MutableInt(0);
        MutableInt invalidBatchesCount = new MutableInt(0);
        List<String> messages = Lists.newArrayList();

        operationFilter = operationFilter.clone();

        // Split operation ids, if too many
        Integer[] operationIds = operationFilter.getIncludedIds();
        if (ArrayUtils.getLength(operationIds) > 500) {
            int pageSize = 500;
            long pageCount = Math.round((double)(operationIds.length / pageSize) + 0.5); // Get page count
            for (int page = 0; page < pageCount; page++) {
                int from = page * pageSize;
                int to = Math.min(operationIds.length, from + pageSize);
                Integer[] pageOperationIds = Arrays.stream(Arrays.copyOfRange(operationIds, from, to))
                    .mapToInt(Number::intValue)
                    .boxed()
                    .toArray(Integer[]::new);

                operationFilter.setIncludedIds(pageOperationIds);

                // Loop on page
                DenormalizedTripResultVO pageResult = denormalizeByFilter(operationFilter, baseOptions);

                // Update counters
                operationCount.add(pageResult.getOperationCount());
                batchesCount.add(pageResult.getBatchCount());
                invalidBatchesCount.add(pageResult.getInvalidBatchCount());
                if (StringUtils.isNotBlank(pageResult.getMessage())) {
                    messages.add(pageResult.getMessage());
                }
            }
        }
        else {


            // Make sure to exclude parent operation, because should not have batches
            // (see "filage" operation in ACOST program)
            operationFilter.setHasNoChildOperation(true);

            // Select only operation that should be update (if not force)
            operationFilter.setNeedBatchDenormalization(!baseOptions.isForce());

            // DEBUG - force denormalization
            //if (!this.production && !baseOptions.isForce()) operationFilter.setNeedBatchDenormalization(false);

            long operationTotal = operationService.countByFilter(operationFilter);

            boolean hasMoreData;
            int pageSize = 10;

            if (operationTotal > 0) {
                do {
                    // Fetch some operations
                    List<OperationVO> operations = operationService.findAllByFilter(operationFilter,
                        // IMPORTANT: offset always = 0, because the next iteration can exclude newly updated operations - See issue sumaris-app#941
                        0, pageSize,
                        // IMPORTANT: Sort by id ASC, to keep continuity between pages
                        OperationVO.Fields.ID, SortDirection.ASC,
                        OperationFetchOptions.builder()
                            .withChildrenEntities(false)
                            .withMeasurementValues(false)
                            // Fetch position and fishing area, to be able to compute fishing area id, need by conversion
                            .withPositions(true)
                            .withFishingAreas(true)
                            .build());

                    if (CollectionUtils.isNotEmpty(operations)) {
                        operations.parallelStream().forEach(operation -> {
                            try {
                                // Prepare options (add fishing area, date, etc.)
                                DenormalizedBatchOptions options = createOptionsByOperation(operation, baseOptions);

                                List<?> batches = denormalizedBatchService.denormalizeAndSaveByOperationId(operation.getId(), options);
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

                        // Prepare next page (skip previous operation)
                        OperationVO lastOperation = operations.get(operations.size() - 1);
                        operationFilter.setMinId(lastOperation.getId() + 1);
                    }

                    operationCount.add(operations.size());
                    hasMoreData = operations.size() >= pageSize;
                    if (operationCount.intValue() > operationTotal) {
                        operationTotal = operationCount.intValue();
                    }
                } while (hasMoreData);
            }
        }

        return DenormalizedTripResultVO.builder()
            .operationCount(operationCount.intValue())
            .batchCount(batchesCount.intValue())
            .invalidBatchCount(invalidBatchesCount.intValue())
            .message(CollectionUtils.isNotEmpty(messages) ? String.join("\n", messages) : null)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }

    public DenormalizedBatchOptions createOptionsByOperation(@NonNull OperationVO operation,
                                                             @Nullable DenormalizedBatchOptions inheritedOptions) {

        if (inheritedOptions == null) {
            int programId = operationService.getProgramIdById(operation.getId());
            inheritedOptions = denormalizedBatchService.createOptionsByProgramId(programId);
        }

        Optional<Integer[]> fishingAreaLocationIds = getOperationFishingAreaIds(operation);
        if (fishingAreaLocationIds.isEmpty()) {
            log.warn("Cannot found the statistical rectangle for Operation #{}, neither in positions nor in fishing areas", operation.getId());
        }

        DenormalizedBatchOptions options = inheritedOptions.clone(); // Copy, to keep original options unchanged
        options.setFishingAreaLocationIds(fishingAreaLocationIds.orElse(null));
        options.setDateTime(Dates.resetTime(getFishingStartDateTime(operation)));

        return options;
    }

    public Optional<Integer[]> getOperationFishingAreaIds(@NonNull OperationVO operation) {
        // Get the fishing area: first, search from last position
        Integer[] result = Beans.getStream(operation.getPositions())
            .filter(Positions::isNotNullAndValid)
            .map(position -> locationService.getStatisticalRectangleIdByLatLong(position.getLatitude(), position.getLongitude()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toArray(Integer[]::new);
        if (ArrayUtils.isNotEmpty(result)) return Optional.of(result);

        // Try to get location from fishing areas
        result = Beans.getStream(operation.getFishingAreas())
            .filter(fa -> fa.getLocation() != null)
            .map(FishingAreaVO::getLocation)
            .map(LocationVO::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toArray(Integer[]::new);
        if (ArrayUtils.isNotEmpty(result)) return Optional.of(result);

        return Optional.empty(); // Not found
    }

    /**
     * Get the start fishing date (or the start date if no found)
     * @param operation
     * @return
     */
    public Date getFishingStartDateTime(@NonNull OperationVO operation) {
        return operation.getFishingStartDateTime() != null
            ? operation.getFishingStartDateTime()
            : operation.getStartDateTime();
    }
}
