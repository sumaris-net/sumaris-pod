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


import lombok.NonNull;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.OperationFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of operation beans
 * 
 */
@Transactional
public interface OperationService {


	@Transactional(readOnly = true)
	List<OperationVO> findAllByTripId(int tripId, OperationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<OperationVO> findAllByTripId(int tripId, int offset, int size, String sortAttribute, SortDirection sortDirection, OperationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<OperationVO> findAllByFilter(@NonNull OperationFilterVO filter, @NonNull OperationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<OperationVO> findAllByFilter(OperationFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection, OperationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	Long countByTripId(int tripId);

	@Transactional(readOnly = true)
	Long countByFilter(OperationFilterVO filter);

	@Transactional(readOnly = true)
	OperationVO get(int id);

	@Transactional(readOnly = true)
	OperationVO get(int id, OperationFetchOptions o);

	OperationVO save(OperationVO source);

	OperationVO control(OperationVO source);

	List<OperationVO> save(List<OperationVO> sources);

	List<OperationVO> saveAllByTripId(int tripId, List<OperationVO> sources);

	void delete(int id);

	void delete(List<Integer> ids);

	int getProgramIdById(int id);

}
