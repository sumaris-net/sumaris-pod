package net.sumaris.core.service.data;

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

import net.sumaris.core.vo.data.FishingAreaVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author ludovic.pecquot@e-is.pro
 * <p>
 * Service in charge of fishing area beans
 */
@Transactional
public interface FishingAreaService {

    @Transactional(readOnly = true)
    FishingAreaVO getByFishingTripId(int tripId);

    FishingAreaVO saveByFishingTripId(int tripId, FishingAreaVO fishingArea);

    @Transactional(readOnly = true)
    List<FishingAreaVO> getAllByFishingTripId(int tripId);

    List<FishingAreaVO> saveAllByFishingTripId(int tripId, List<FishingAreaVO> fishingAreas);

    @Transactional(readOnly = true)
    List<FishingAreaVO> getAllByOperationId(int operationId);

    List<FishingAreaVO> saveAllByOperationId(int operationId, List<FishingAreaVO> fishingAreas);

    List<FishingAreaVO> saveAllBySaleId(int saleId, List<FishingAreaVO> fishingAreas);

}
