package net.sumaris.core.service.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.data.fishingArea.FishingAreaRepository;
import net.sumaris.core.dao.data.operation.OperationGroupRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.data.FishingAreaVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 09/06/2020.
 */
@Service("fishingAreaService")
public class FishingAreaServiceImpl implements FishingAreaService {

    private final FishingAreaRepository fishingAreaRepository;
    private final OperationGroupRepository operationGroupRepository;

    @Autowired
    public FishingAreaServiceImpl(FishingAreaRepository fishingAreaRepository, OperationGroupRepository operationGroupRepository) {
        this.fishingAreaRepository = fishingAreaRepository;
        this.operationGroupRepository = operationGroupRepository;
    }

    @Override
    public FishingAreaVO getByFishingTripId(int tripId) {
        return Optional.ofNullable(operationGroupRepository.getMainUndefinedOperationGroupId(tripId))
            .flatMap(operationGroupId -> fishingAreaRepository.getAllByOperationId(operationGroupId)
                .stream()
                // Get the first, order by id (should be the first saved)
                .min(Comparator.comparingInt(FishingAreaVO::getId))
            )
            .orElse(null);
    }

    @Override
    public FishingAreaVO saveByFishingTripId(int tripId, FishingAreaVO fishingArea) {
        List<FishingAreaVO> fishingAreas = saveAllByFishingTripId(tripId, ImmutableList.of(fishingArea));
        return CollectionUtils.extractSingleton(fishingAreas);
    }

    @Override
    public List<FishingAreaVO> getAllByFishingTripId(int tripId) {
        return Optional.ofNullable(operationGroupRepository.getMainUndefinedOperationGroupId(tripId))
                .map(fishingAreaRepository::getAllByOperationId)
                .orElse(null);
    }

    @Override
    public List<FishingAreaVO> saveAllByFishingTripId(int tripId, List<FishingAreaVO> fishingAreas) {
        Integer operationGroupId = operationGroupRepository.getMainUndefinedOperationGroupId(tripId);
        if (operationGroupId == null) {
            if (fishingAreas == null || CollectionUtils.isEmpty(fishingAreas)) {
                return null; // Nothing to delete
            }
            throw new SumarisTechnicalException("the main undefined operation was not found, please check the trip's metier");
        }

        if (CollectionUtils.isEmpty(fishingAreas)) {
            fishingAreaRepository.deleteAllByOperationId(operationGroupId);
            return null;
        }

        return fishingAreaRepository.saveAllByOperationId(operationGroupId, fishingAreas);
    }

    @Override
    public List<FishingAreaVO> getAllByOperationId(int operationId) {
        return fishingAreaRepository.getAllByOperationId(operationId);
    }

    @Override
    public List<FishingAreaVO> saveAllByOperationId(int operationId, List<FishingAreaVO> fishingAreas) {
        Preconditions.checkNotNull(fishingAreas);
        return fishingAreaRepository.saveAllByOperationId(operationId, fishingAreas);
    }
}
