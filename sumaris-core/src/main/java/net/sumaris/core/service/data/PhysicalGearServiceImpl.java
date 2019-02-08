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


import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.PhysicalGearDao;
import net.sumaris.core.vo.data.PhysicalGearVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("physicalGearService")
public class PhysicalGearServiceImpl implements PhysicalGearService {

	private static final Logger log = LoggerFactory.getLogger(PhysicalGearServiceImpl.class);

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected PhysicalGearDao physicalGearDao;

	@Autowired
	protected MeasurementDao measurementDao;

	@Override
	public List<PhysicalGearVO> getPhysicalGearByTripId(int tripId) {
		return physicalGearDao.getPhysicalGearByTripId(tripId);
	}

	@Override
	public List<PhysicalGearVO> save(int tripId, List<PhysicalGearVO> sources) {
		return physicalGearDao.save(tripId, sources);
	}


}
