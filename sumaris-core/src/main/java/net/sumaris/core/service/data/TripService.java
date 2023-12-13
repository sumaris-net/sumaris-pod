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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.TripSaveOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author BLA
 * 
 *    Service in charge of trip data
 * 
 */
@Transactional
public interface TripService extends IRootDataQualityService<TripVO> {

	@Transactional(readOnly = true)
	List<TripVO> findAll(TripFilterVO filter, Page page,
						 TripFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<TripVO> findAll(TripFilterVO filter, int offset, int size,
						 String sortAttribute,
						 SortDirection sortDirection,
						 TripFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	long countByFilter(TripFilterVO filter);

	@Transactional(readOnly = true)
	TripVO get(int id);

	@Transactional(readOnly = true)
	TripVO get(int id, TripFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	int getProgramIdById(int id);

	void fillVesselSnapshot(TripVO target);

	void fillVesselSnapshots(List<TripVO> targets);
	void fillTripLandingLinks(TripVO target);

	void fillTripsLandingLinks(List<TripVO> targets);

	TripVO save(TripVO trip, @Nullable TripSaveOptions saveOptions);

	List<TripVO> save(List<TripVO> trips, @Nullable TripSaveOptions saveOptions);


	@Async
	CompletableFuture<Boolean> asyncDelete(int id);

	@Async
	CompletableFuture<Boolean> asyncDelete(List<Integer> ids);

	void delete(List<Integer> ids);

	void delete(int id);

	void deleteAllByLandingId(int landingId);

}
