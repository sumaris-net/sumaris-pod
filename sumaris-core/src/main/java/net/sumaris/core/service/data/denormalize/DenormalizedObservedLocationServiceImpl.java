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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.data.LandingService;
import net.sumaris.core.service.data.ObservedLocationService;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("denormalizeObservedLocationService")
@RequiredArgsConstructor
@Slf4j
public class DenormalizedObservedLocationServiceImpl implements DenormalizedObservedLocationService {

    private final ObservedLocationService observedLocationService;

    private final LandingService landingService;

    private final DenormalizedBatchService denormalizedBatchService;

    private final DenormalizedSaleService denormalizedSaleService;

    @Override
    public DenormalizedSaleResultVO denormalizeById(int observedLocationId) {
        // Load denormalized options
        int programId = observedLocationService.getProgramIdById(observedLocationId);
        DenormalizedBatchOptions programOptions = denormalizedBatchService.createOptionsByProgramId(programId);
        // /!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\
        // TODO : -> REMOVE ME <- this is a stub to force fishingAreaLocationIds to have value
        Integer [] stubLocationIds = {115, 125, 127, 128, 136, 131, 135};
        programOptions.setFishingAreaLocationIds(stubLocationIds);
        // /!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\/!\

        // Create operation filter, for this observedLocation
        LandingFilterVO landingFilter = LandingFilterVO.builder()
                .observedLocationId(observedLocationId)
                .hasSale(Boolean.TRUE)
                .build();

        Integer[] saleIds = landingService.findAll(landingFilter, null, LandingFetchOptions.builder()
                .withVesselSnapshot(false)
                .withRecorderDepartment(false)
                .withRecorderPerson(false)
                .withSaleIds(true)
                .build()).stream().map((landing) -> landing.getSaleIds()).collect(Collectors.toList())
                .stream()
                .flatMap(List::stream)
                .distinct()
                .toArray(Integer[]::new);

        return denormalizedSaleService.denormalizeByFilter(SaleFilterVO.builder().includedIds(saleIds).build(), programOptions);
    }
}
