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
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.DataControlOptions;
import net.sumaris.core.vo.data.DataValidateOptions;
import net.sumaris.core.vo.data.LandingFetchOptions;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of observed location data
 * 
 */
@Transactional
public interface LandingService {

	@Transactional(readOnly = true)
	List<LandingVO> findAll(LandingFilterVO filter, Page page, LandingFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	Long countByFilter(LandingFilterVO filter);

	@Transactional(readOnly = true)
	LandingVO get(Integer id);

	@Transactional(readOnly = true)
	LandingVO get(Integer landingId, LandingFetchOptions fetchOptions);

	List<LandingVO> saveAllByObservedLocationId(int observedLocationId, List<LandingVO> data);

	LandingVO save(LandingVO data);

	List<LandingVO> save(List<LandingVO> data);

	void deleteAllByObservedLocationId(int observedLocationId);

	void deleteAllByFilter(LandingFilterVO filter);

	void delete(int id);

	void delete(List<Integer> ids);

    LandingVO control(LandingVO data, @Nullable DataControlOptions options);

	LandingVO validate(LandingVO data, @Nullable DataValidateOptions options);

	LandingVO unvalidate(LandingVO data, @Nullable DataValidateOptions options);

	void fillVesselSnapshot(LandingVO target);

	void fillVesselSnapshots(List<LandingVO> target);
}
