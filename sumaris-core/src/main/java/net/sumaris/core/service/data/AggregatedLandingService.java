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


import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.filter.AggregatedLandingFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author ludovic.pecquot@e-is.pro
 * 
 *    Service in charge of aggregated landing data
 * 
 */
@Transactional
public interface AggregatedLandingService {

	@Transactional(readOnly = true)
	List<AggregatedLandingVO> findAll(AggregatedLandingFilterVO filter);

	List<AggregatedLandingVO> saveAll(AggregatedLandingFilterVO filter, List<AggregatedLandingVO> aggregatedLandings);

	void deleteAll(AggregatedLandingFilterVO filter, List<Integer> vesselIds);

}
