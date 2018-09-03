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


import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.VesselPositionVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of vesselPosition beans
 * 
 */
@Transactional
public interface VesselPositionService {


	@Transactional(readOnly = true)
	List<VesselPositionVO> getAllByOperationId(int operationId, int offset, int size, String sortAttribute, SortDirection sortDirection);

	@Transactional(readOnly = true)
	VesselPositionVO get(int id);

	VesselPositionVO save(VesselPositionVO vesselPosition);

	List<VesselPositionVO> save(List<VesselPositionVO> vesselPositions);

	void delete(int id);

	void delete(List<Integer> ids);

}
