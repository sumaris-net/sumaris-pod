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


import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.filter.PhysicalGearFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of physical gear
 * 
 */
@Transactional
public interface PhysicalGearService {

	@Transactional(readOnly = true)
	List<PhysicalGearVO> findAll(PhysicalGearFilterVO filter, Page page, DataFetchOptions options);

	@Transactional(readOnly = true)
	List<PhysicalGearVO> getAllByTripId(int tripId, DataFetchOptions options);

	List<PhysicalGearVO> saveAllByTripId(int tripId, List<PhysicalGearVO> sources);

	List<PhysicalGearVO> saveAllByTripId(int tripId, List<PhysicalGearVO> sources, List<Integer> idsToRemove);

	void delete(List<Integer> ids);

	void delete(int id);
}
