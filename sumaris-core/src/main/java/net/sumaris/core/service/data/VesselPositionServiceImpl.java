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


import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.VesselPositionDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.VesselPositionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("vesselPositionService")
@Slf4j
public class VesselPositionServiceImpl implements VesselPositionService {

	@Autowired
	protected VesselPositionDao vesselPositionDao;

	@Override
	public List<VesselPositionVO> getAllByOperationId(int operationId, int offset, int size, String sortAttribute,
                                     SortDirection sortDirection) {
		return vesselPositionDao.getAllByOperationId(operationId, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public VesselPositionVO get(int vesselPositionId) {
		return vesselPositionDao.get(vesselPositionId);
	}

	@Override
	public VesselPositionVO save(final VesselPositionVO vesselPosition) {
		Preconditions.checkNotNull(vesselPosition);
		Preconditions.checkNotNull(vesselPosition.getDateTime(), "Missing startDateTime");
		Preconditions.checkNotNull(vesselPosition.getLatitude(), "Missing latitude");
		Preconditions.checkNotNull(vesselPosition.getLongitude(), "Missing longitude");
		Preconditions.checkNotNull(vesselPosition.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(vesselPosition.getRecorderDepartment().getId(), "Missing recorderDepartment.id");

		VesselPositionVO savedVesselPosition = vesselPositionDao.save(vesselPosition);

		return savedVesselPosition;
	}

	@Override
	public List<VesselPositionVO> save(List<VesselPositionVO> vesselPositions) {
		Preconditions.checkNotNull(vesselPositions);

		return vesselPositions.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		vesselPositionDao.delete(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

}
