package net.sumaris.core.service.data.vessel;

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


import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.filter.VesselOwnerFilterVO;
import net.sumaris.core.vo.filter.VesselRegistrationFilterVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional
public interface VesselService {

	@Transactional(readOnly = true)
	List<VesselVO> findAll(final VesselFilterVO filter,
						   final net.sumaris.core.dao.technical.Page page,
						   final VesselFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	long countByFilter(final VesselFilterVO filter);

	@Transactional(readOnly = true)
	VesselVO get(int id);

	@CacheEvict(cacheNames = {
		CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER
	}, allEntries = true)
	VesselVO save(VesselVO source);

	@CacheEvict(cacheNames = {
		CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER
	}, allEntries = true)
	List<VesselVO> save(List<VesselVO> sources);

	@CacheEvict(cacheNames = {
		CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER
	}, allEntries = true)
	void delete(int id);

	@CacheEvict(cacheNames = {
		CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER
	}, allEntries = true)
	void delete(List<Integer> ids);

	@Transactional(readOnly = true)
	List<VesselFeaturesVO> findFeaturesByVesselId(int vesselId, Page page, DataFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<VesselRegistrationPeriodVO> findRegistrationPeriodsByVesselId(int vesselId, Page page);

	@Transactional(readOnly = true)
	List<VesselRegistrationPeriodVO> findRegistrationPeriodsByFilter(VesselRegistrationFilterVO filter, Page page);

	@Transactional(readOnly = true)
	List<VesselOwnerPeriodVO> findOwnerPeriodsByVesselId(int vesselId, Page page);

	@Transactional(readOnly = true)
	List<VesselOwnerPeriodVO> findOwnerPeriodsByFilter(VesselOwnerFilterVO filter, Page page);

	@CacheEvict(cacheNames = {
		CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER,
		CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER
	}, allEntries = true)
	void replaceTemporaryVessel(List<Integer> temporaryVesselIds, int targetVesselId);
}
