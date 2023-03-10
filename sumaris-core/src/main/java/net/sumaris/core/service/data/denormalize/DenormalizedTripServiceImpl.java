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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.service.data.TripService;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.OperationFilterVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("denormalizeTripService")
@RequiredArgsConstructor
@Slf4j
public class DenormalizedTripServiceImpl implements DenormalizedTripService {

    private final TripService tripService;

    private final DenormalizedBatchService denormalizedBatchService;

    private final DenormalizedOperationService denormalizedOperationService;



    @Override
    public DenormalizedTripResultVO denormalizeByFilter(@NonNull TripFilterVO filter) {
        ProgressionModel progress = new ProgressionModel();
        progress.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, (event) -> {
            if (event.getNewValue() != null) log.debug(event.getNewValue().toString());
        });
        return denormalizeByFilter(filter, new ProgressionModel());
    }

    @Override
    public DenormalizedTripResultVO denormalizeByFilter(@NonNull TripFilterVO tripFilter, @NonNull IProgressionModel progression) {
        long startTime = System.currentTimeMillis();

        progression.setCurrent(0);
        progression.setMessage(String.format("Starting trips denormalization... filter: %s", tripFilter));

        TripFetchOptions tripFetchOptions = TripFetchOptions.builder()
            .withChildrenEntities(false)
            .withMeasurementValues(false)
            .withRecorderPerson(false)
            .build();

        long tripTotal = tripService.countByFilter(tripFilter);
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
            List<TripVO> trips = tripService.findAll(tripFilter,
                offset, pageSize, // Page
                TripVO.Fields.ID, SortDirection.ASC, // Sort by id, to keep continuity between pages
                tripFetchOptions);

            if (offset > 0 && offset % (pageSize * 2) == 0) {
                progression.setCurrent(offset);
                progression.setMessage(String.format("Processing trips denormalization... %s/%s", offset, tripTotal));
                //log.trace(progression.getMessage());
            }

            // Denormalize each trip
            trips.stream()
                .map(trip -> {
                    // Load denormalized options
                    DenormalizedBatchOptions programOptions = denormalizedOperationService.createOptionsByProgramId(trip.getProgram().getId());

                    // Create operations filter, for this trip
                    OperationFilterVO operationFilter = OperationFilterVO.builder()
                        .tripId(trip.getId())
                        .includedIds(tripFilter.getOperationIds())
                        .hasNoChildOperation(true)
                        .build();

                    // Denormalize trip's operation
                    return denormalizedOperationService.denormalizeByFilter(operationFilter, programOptions);
                })
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
        //log.debug(progression.getMessage());

        return DenormalizedTripResultVO.builder()
            .tripCount(tripCount)
            .operationCount(operationCount.intValue())
            .batchCount(batchCount.intValue())
            .invalidBatchCount(invalidBatchCount.intValue())
            .executionTime(System.currentTimeMillis() - startTime)
            .build();
    }

    @Override
    public DenormalizedTripResultVO denormalizeById(int tripId) {
        // Load denormalized options
        int programId = tripService.getProgramIdById(tripId);
        DenormalizedBatchOptions programOptions = denormalizedBatchService.createOptionsByProgramId(programId);

        // Create operation filter, for this trip
        OperationFilterVO operationFilter = OperationFilterVO.builder()
            .tripId(tripId)
            .hasNoChildOperation(true)
            .build();

        return denormalizedOperationService.denormalizeByFilter(operationFilter, programOptions);
    }

}
