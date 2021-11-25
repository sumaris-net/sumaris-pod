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
import net.sumaris.core.dao.data.fishingArea.FishingAreaRepository;
import net.sumaris.core.dao.data.operation.OperationGroupRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.data.FishingAreaVO;
import net.sumaris.core.vo.data.OperationGroupVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
        return Optional.ofNullable(operationGroupRepository.getMainUndefinedOperationGroup(tripId))
            .flatMap(operationGroup -> fishingAreaRepository.getAllByOperationId(operationGroup.getId()).stream().findFirst())
            .orElse(null);
    }

    @Override
    public FishingAreaVO saveByFishingTripId(int tripId, FishingAreaVO fishingArea) {
        OperationGroupVO operationGroup = operationGroupRepository.getMainUndefinedOperationGroup(tripId);
        if (operationGroup == null) {
            if (fishingArea == null) {
                return null; // Nothing to delete
            }
            throw new SumarisTechnicalException("the main undefined operation was not found, please check the trip's metier");
        }

        if (fishingArea == null) {
            fishingAreaRepository.deleteAllByOperationId(operationGroup.getId());
            return null;
        }

        List<FishingAreaVO> fishingAreas = fishingAreaRepository.saveAllByOperationId(operationGroup.getId(), Collections.singletonList(fishingArea));
        return CollectionUtils.extractSingleton(fishingAreas);
    }

    @Override
    public List<FishingAreaVO> getAllByFishingTripId(int tripId) {
        return Optional.ofNullable(operationGroupRepository.getMainUndefinedOperationGroup(tripId))
                .map(operationGroup -> fishingAreaRepository.getAllByOperationId(operationGroup.getId()))
                .orElse(null);
    }

    @Override
    public List<FishingAreaVO> saveAllByFishingTripId(int tripId, List<FishingAreaVO> fishingAreas) {
        OperationGroupVO operationGroup = operationGroupRepository.getMainUndefinedOperationGroup(tripId);
        if (operationGroup == null) {
            if (fishingAreas == null || CollectionUtils.isEmpty(fishingAreas)) {
                return null; // Nothing to delete
            }
            throw new SumarisTechnicalException("the main undefined operation was not found, please check the trip's metier");
        }

        if (CollectionUtils.isEmpty(fishingAreas)) {
            fishingAreaRepository.deleteAllByOperationId(operationGroup.getId());
            return null;
        }

        return fishingAreaRepository.saveAllByOperationId(operationGroup.getId(), fishingAreas);
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
