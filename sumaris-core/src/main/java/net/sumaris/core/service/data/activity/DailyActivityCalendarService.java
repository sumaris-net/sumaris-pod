/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.data.activity;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.data.IRootDataQualityService;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarVO;
import net.sumaris.core.vo.filter.DailyActivityCalendarFilterVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author BLA
 * 
 *    Service in charge of activityCalendar data
 * 
 */
@Transactional
public interface DailyActivityCalendarService extends IRootDataQualityService<DailyActivityCalendarVO> {

	@Transactional(readOnly = true)
	List<DailyActivityCalendarVO> findAll(DailyActivityCalendarFilterVO filter, Page page,
										  DailyActivityCalendarFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<DailyActivityCalendarVO> findAll(DailyActivityCalendarFilterVO filter, int offset, int size,
						 String sortAttribute,
						 SortDirection sortDirection,
						 DailyActivityCalendarFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	long countByFilter(DailyActivityCalendarFilterVO filter);

	@Transactional(readOnly = true)
	DailyActivityCalendarVO get(int id);

	@Transactional(readOnly = true)
	DailyActivityCalendarVO get(int id, DailyActivityCalendarFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	int getProgramIdById(int id);

	void fillVesselSnapshot(DailyActivityCalendarVO target);

	void fillVesselSnapshots(List<DailyActivityCalendarVO> targets);

	DailyActivityCalendarVO save(DailyActivityCalendarVO source);

	List<DailyActivityCalendarVO> save(List<DailyActivityCalendarVO> sources);


	@Async
	CompletableFuture<Boolean> asyncDelete(int id);

	@Async
	CompletableFuture<Boolean> asyncDelete(List<Integer> ids);

	void delete(List<Integer> ids);

	void delete(int id);

}
