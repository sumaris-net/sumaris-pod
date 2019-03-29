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
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of trip data
 * 
 */
@Transactional
public interface TripService {


	@Transactional(readOnly = true)
	List<TripVO> getAllTrips(int offset, int size);

	@Transactional(readOnly = true)
	List<TripVO> findByFilter(TripFilterVO filter, int offset, int size);

	@Transactional(readOnly = true)
	List<TripVO> findByFilter(TripFilterVO filter, int offset, int size,
							  String sortAttribute,
							  SortDirection sortDirection,
							  DataFetchOptions fieldOptions);

	@Transactional(readOnly = true)
	Long countByFilter(TripFilterVO filter);

	@Transactional(readOnly = true)
	TripVO get(int id);

	@Transactional(readOnly = true)
	<T> T get(int id, Class<T> targetClass);

	TripVO save(TripVO trip, boolean withOperation);

	List<TripVO> save(List<TripVO> trips, boolean withOperation);

	void delete(int id);

	void delete(List<Integer> ids);

    TripVO control(TripVO trip);

	TripVO validate(TripVO trip);

	TripVO unvalidate(TripVO trip);
}
