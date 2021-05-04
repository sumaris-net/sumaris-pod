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
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.service.data.DenormalizedBatchService;
import net.sumaris.core.service.data.OperationService;
import net.sumaris.core.service.data.TripService;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service("denormalizeTripService")
@Slf4j
public class DenormalizeTripServiceImpl implements DenormalizeTripService {

    @Resource
    private SumarisConfiguration configuration;

    @Resource
    private TripService tripService;

    @Resource
    private DenormalizedBatchService denormalizedBatchService;

    @Resource
    private BatchRepository batchRepository;

    @Resource
    private OperationService operationService;

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

        DataFetchOptions tripFetchOptions = DataFetchOptions.builder()
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
        int operationCount = 0;
        do {
            // Fetch some trips
            List<TripVO> trips = tripService.findByFilter(filter,
                offset, pageSize, // Page
                TripVO.Fields.ID, SortDirection.ASC, // Sort by id, to keep continuity between pages
                tripFetchOptions);

            if (offset > 0 && offset % (pageSize * 2) == 0) {
                progression.setCurrent(offset);
                progression.setMessage(String.format("Processing trips denormalization... %s/%s", offset, tripTotal));
                log.debug(progression.getMessage());
            }

            // Denormalize each trip
            operationCount += trips.stream()
                .map(TripVO::getId)
                .map(this::denormalizeById)
                .mapToLong(DenormalizeTripResultVO::getOperationCount)
                .sum();

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
        progression.setMessage(String.format("Trips denormalization finished, in %s (%s trips, %s operations)",
            TimeUtils.printDurationFrom(startTime),
            tripCount,
            operationCount));
        log.debug(progression.getMessage());

        return DenormalizeTripResultVO.builder()
            .tripCount(tripCount)
            .operationCount(operationCount)
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }

    @Override
    public DenormalizeTripResultVO denormalizeById(int tripId) {
        long startTime = System.currentTimeMillis();
        long operationTotal = operationService.countByTripId(tripId);

        boolean hasMoreData;
        int offset = 0;
        int pageSize = 10;
        int operationCount = 0;
        do {
            // Fetch some operations
            List<OperationVO> operations = operationService.findAllByTripId(tripId,
                offset, pageSize, // Page
                OperationVO.Fields.ID, SortDirection.ASC, // Sort by id, to keep continuity between pages
                DataFetchOptions.builder()
                    .withChildrenEntities(false)
                    .withMeasurementValues(false)
                    .build());

            operations.forEach(operation -> {
                List<DenormalizedBatchVO> denormalizedBatches = denormalizedBatchService.denormalizeAndSaveByOperationId(operation.getId());
                log.trace("Saving {} denormalized batches for operation {id: {}}", CollectionUtils.size(denormalizedBatches), operation.getId());
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
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }
}
