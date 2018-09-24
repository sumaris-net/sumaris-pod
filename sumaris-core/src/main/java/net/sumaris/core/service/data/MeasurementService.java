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


import net.sumaris.core.vo.data.MeasurementVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author BLA
 * 
 *    Service in charge of measurements
 * 
 */
@Transactional
public interface MeasurementService {

    @Transactional(readOnly = true)
    List<MeasurementVO> getVesselUseMeasurementsByTripId(int tripId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getVesselUseMeasurementsByOperationId(int operationId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getGearUseMeasurementsByOperationId(int operationId);

    @Transactional(readOnly = true)
    List<MeasurementVO> getSampleMeasurements(int sampleId);

    @Transactional(readOnly = true)
    Map<Integer, Object> getSampleMeasurementsMap(int sampleId);

    @Transactional(readOnly = true)
    Map<Integer, Object> getBatchSortingMeasurementsMap(int batchId);

    @Transactional(readOnly = true)
    Map<Integer, Object> getBatchQuantificationMeasurementsMap(int batchId);

}
