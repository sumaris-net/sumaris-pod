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


import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.vo.data.MeasurementVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("measurementService")
public class MeasurementServiceImpl implements MeasurementService {

	private static final Log log = LogFactory.getLog(MeasurementServiceImpl.class);

	@Autowired
	protected MeasurementDao measurementDao;

    @Override
    public List<MeasurementVO> getVesselUseMeasurementsByTripId(int tripId) {
        return measurementDao.getVesselUseMeasurementsByTripId(tripId);
    }

	@Override
	public List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId) {
		return measurementDao.getPhysicalGearMeasurements(physicalGearId);
	}

	@Override
	public List<MeasurementVO> getVesselUseMeasurementsByOperationId(int operationId) {
		return measurementDao.getVesselUseMeasurementsByOperationId(operationId);
	}

	@Override
	public List<MeasurementVO> getGearUseMeasurementsByOperationId(int operationId) {
		return measurementDao.getGearUseMeasurementsByOperationId(operationId);
	}

	@Override
	public List<MeasurementVO> getSampleMeasurements(int sampleId) {
		return measurementDao.getSampleMeasurements(sampleId);
	}

}
