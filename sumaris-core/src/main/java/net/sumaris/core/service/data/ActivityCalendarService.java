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
import net.sumaris.core.vo.data.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.ActivityCalendarVO;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
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
public interface ActivityCalendarService extends IRootDataQualityService<ActivityCalendarVO> {

	@Transactional(readOnly = true)
	List<ActivityCalendarVO> findAll(ActivityCalendarFilterVO filter, Page page,
						 ActivityCalendarFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ActivityCalendarVO> findAll(ActivityCalendarFilterVO filter, int offset, int size,
						 String sortAttribute,
						 SortDirection sortDirection,
						 ActivityCalendarFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	long countByFilter(ActivityCalendarFilterVO filter);

	@Transactional(readOnly = true)
	ActivityCalendarVO get(int id);

	@Transactional(readOnly = true)
	ActivityCalendarVO get(int id, ActivityCalendarFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	int getProgramIdById(int id);

	void fillVesselSnapshot(ActivityCalendarVO target);

	void fillVesselSnapshots(List<ActivityCalendarVO> targets);

	ActivityCalendarVO save(ActivityCalendarVO source);

	List<ActivityCalendarVO> save(List<ActivityCalendarVO> sources);


	@Async
	CompletableFuture<Boolean> asyncDelete(int id);

	@Async
	CompletableFuture<Boolean> asyncDelete(List<Integer> ids);

	void delete(List<Integer> ids);

	void delete(int id);

}
