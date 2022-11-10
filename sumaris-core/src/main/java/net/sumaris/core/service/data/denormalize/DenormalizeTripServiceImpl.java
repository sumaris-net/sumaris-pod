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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.service.data.DenormalizedBatchService;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.service.data.TripService;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("denormalizeTripService")
@Slf4j
public class DenormalizeTripServiceImpl implements DenormalizeTripService {

    @Autowired
    private SumarisConfiguration configuration;

    @Autowired
    private TripService tripService;

    @Autowired
    private DenormalizedBatchService denormalizedBatchService;

    @Autowired
    private OperationService operationService;

    @Autowired(required = false)
    private TaskExecutor taskExecutor;

    @Override
    public DenormalizeTripResultVO denormalizeByFilter(@NonNull TripFilterVO filter) {
        return denormalizeByFilter(filter, new ProgressionModel());
    }

    @Override
    public DenormalizeTripResultVO denormalizeByFilter(@NonNull TripFilterVO filter, @NonNull IProgressionModel progression) {
        long startTime = System.currentTimeMillis();

        progression.setCurrent(0);
        progression.setMessage(String.format("Starting trips denormalization... filter: %s", filter));
        log.debug(progression.getMessage());

        TripFetchOptions tripFetchOptions = TripFetchOptions.builder()
            .withChildrenEntities(false)
            .withMeasurementValues(false)
            .withRecorderPerson(false)
            .build();

        long tripTotal = tripService.countByFilter(filter);
        progression.setTotal(tripTotal);

        boolean hasMoreData;
        int offset = 0;
        int pageSize = 10;
        int tripCount = 0;
        MutableInt operationCount = new MutableInt(0);
        MutableInt batchCount = new MutableInt(0);
        MutableInt invalidBatchCount = new MutableInt(0);
        do {
            // Fetch some trips
            List<TripVO> trips = tripService.findAll(filter,
                offset, pageSize, // Page
                TripVO.Fields.ID, SortDirection.ASC, // Sort by id, to keep continuity between pages
                tripFetchOptions);

            if (offset > 0 && offset % (pageSize * 2) == 0) {
                progression.setCurrent(offset);
                progression.setMessage(String.format("Processing trips denormalization... %s/%s", offset, tripTotal));
                log.debug(progression.getMessage());
            }

            // Denormalize each trip
            trips.stream()
                .map(TripVO::getId)
                .map(this::denormalizeById)
                .forEach(result -> {
                    operationCount.add(result.getOperationCount());
                    batchCount.add(result.getBatchCount());
                    invalidBatchCount.add(result.getInvalidBatchCount());
                });

            offset += pageSize;
            tripCount += trips.size();
            hasMoreData = trips.size() >= pageSize;
            if (tripCount > tripTotal) {
                tripTotal = tripCount;
                progression.adaptTotal(tripTotal);
            }
        } while (hasMoreData);

        // Success log
        progression.setCurrent(tripCount);
        progression.setMessage(String.format("Trips denormalization finished, in %s - %s trips, %s operations, %s batches - %s invalid batch tree (skipped)",
            TimeUtils.printDurationFrom(startTime),
            tripCount,
            operationCount,
            batchCount,
            invalidBatchCount));
        log.debug(progression.getMessage());

        return DenormalizeTripResultVO.builder()
            .tripCount(tripCount)
            .operationCount(operationCount.intValue())
            .batchCount(batchCount.intValue())
            .invalidBatchCount(invalidBatchCount.intValue())
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }

    @Override
    public DenormalizeTripResultVO denormalizeById(int tripId) {
        long startTime = System.currentTimeMillis();
        long operationTotal = operationService.countByTripId(tripId);

        // Load denormalized options
        int programId = tripService.getProgramIdById(tripId);
        DenormalizedBatchOptions options = denormalizedBatchService.createOptionsByProgramId(programId);

        boolean hasMoreData;
        int offset = 0;
        int pageSize = 10;
        int operationCount = 0;
        MutableInt batchesCount = new MutableInt(0);
        MutableInt errorCount = new MutableInt(0);
        do {
            // Fetch some operations
            List<OperationVO> operations = operationService.findAllByTripId(tripId,
                offset, pageSize, // Page
                OperationVO.Fields.ID, SortDirection.ASC, // Sort by id, to keep continuity between pages
                OperationFetchOptions.builder()
                    .withChildrenEntities(false)
                    .withMeasurementValues(false)
                    .build());

            operations.forEach(operation -> {
                try {
                    List<?> batches = denormalizedBatchService.denormalizeAndSaveByOperationId(operation.getId(), options);
                    batchesCount.add(CollectionUtils.size(batches));
                } catch (SumarisBusinessException be) {
                    log.error(be.getMessage());
                    errorCount.increment();
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);
                    errorCount.increment();
                }
            });

            offset += pageSize;
            operationCount += operations.size();
            hasMoreData = operations.size() >= pageSize;
            if (operationCount > operationTotal) {
                operationTotal = operationCount;
            }
        } while (hasMoreData);

        return DenormalizeTripResultVO.builder()
            .tripCount(1)
            .operationCount(operationCount)
            .batchCount(batchesCount.intValue())
            .invalidBatchCount(errorCount.intValue())
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
}
