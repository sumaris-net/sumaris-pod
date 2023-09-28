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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.vessel.UpdateVesselSnapshotsResultVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional
public interface VesselSnapshotService {


	@Transactional(readOnly = true)
	List<VesselSnapshotVO> findAll(VesselFilterVO filter,
								   net.sumaris.core.dao.technical.Page page,
								   VesselFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<VesselSnapshotVO> findAll(VesselFilterVO filter,
								   int offset, int size,
								   String sortAttribute, SortDirection sortDirection,
								   final VesselFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER, key = "#filter.hashCode()")
	Long countByFilter(final VesselFilterVO filter);

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE)
	VesselSnapshotVO getByIdAndDate(int vesselId, Date date);

	@Transactional(readOnly = true)
	Optional<Date> getMaxIndexedUpdateDate();

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	UpdateVesselSnapshotsResultVO indexVesselSnapshots(VesselFilterVO filter);

	@Async("jobTaskExecutor")
	Future<UpdateVesselSnapshotsResultVO> asyncIndexVesselSnapshots(VesselFilterVO filter,
																	@Nullable IProgressionModel progressionModel);
}
