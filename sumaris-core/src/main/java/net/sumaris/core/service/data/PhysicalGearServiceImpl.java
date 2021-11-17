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
import net.sumaris.core.dao.data.physicalGear.PhysicalGearRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service("physicalGearService")
@Slf4j
public class PhysicalGearServiceImpl implements PhysicalGearService {

	@Autowired
	protected PhysicalGearRepository physicalGearRepository;

	@Override
	public List<PhysicalGearVO> findAll(PhysicalGearFilterVO filter, Page page, DataFetchOptions options) {
		return physicalGearRepository.findAll(filter != null ? filter : new PhysicalGearFilterVO(), page, options);
	}

	@Override
	public List<PhysicalGearVO> getAllByTripId(int tripId, DataFetchOptions options) {
		return physicalGearRepository.findAllVO(physicalGearRepository.hasTripId(tripId), options);
	}

	@Override
	public List<PhysicalGearVO> saveAllByTripId(int tripId, List<PhysicalGearVO> sources) {
		return physicalGearRepository.saveAllByTripId(tripId, sources, null);
	}

	@Override
	public List<PhysicalGearVO> saveAllByTripId(int tripId, List<PhysicalGearVO> sources, List<Integer> idsToRemove) {
		return physicalGearRepository.saveAllByTripId(tripId, sources, idsToRemove);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public void delete(int id) {
		// Apply deletion
		physicalGearRepository.deleteById(id);
	}

}
