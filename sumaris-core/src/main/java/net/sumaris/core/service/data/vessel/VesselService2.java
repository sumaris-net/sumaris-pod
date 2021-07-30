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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional
public interface VesselService2 {

	@Transactional(readOnly = true)
	List<VesselVO> findAll(VesselFilterVO filter, Page page,
						   DataFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	Long countByFilter(VesselFilterVO filter);

	@Transactional(readOnly = true)
	VesselVO get(int id);

	/*@Transactional(readOnly = true)
	List<VesselSnapshotVO> findSnapshotByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE)
    VesselSnapshotVO getSnapshotByIdAndDate(int vesselId, Date date);

	@Transactional(readOnly = true)
	List<VesselVO> findVesselsByFilter(VesselFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

	@Transactional(readOnly = true)
	Long countVesselsByFilter(VesselFilterVO filter);

	@Transactional(readOnly = true)
    VesselVO getVesselById(int vesselId);

	@Transactional(readOnly = true)
	List<VesselFeaturesVO> getFeaturesByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection);

	@Transactional(readOnly = true)
	List<VesselRegistrationVO> getRegistrationsByVesselId(int vesselId, int offset, int size, String sortAttribute, SortDirection sortDirection);

	@CacheEvict(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, allEntries = true)
	VesselVO save(VesselVO source);

	@CacheEvict(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, allEntries = true)
	VesselVO save(VesselVO source, boolean checkUpdateDate);

	@CacheEvict(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, allEntries = true)
	List<VesselVO> save(List<VesselVO> sources);

	@CacheEvict(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, allEntries = true)
	void delete(int id);

	@CacheEvict(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE, allEntries = true)
	void delete(List<Integer> ids);*/

}
