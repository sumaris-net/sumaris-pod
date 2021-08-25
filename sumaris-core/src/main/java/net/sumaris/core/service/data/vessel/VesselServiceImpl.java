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


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.vessel.VesselFeaturesSpecifications;
import net.sumaris.core.dao.data.vessel.VesselRepository;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("vesselService2")
@Slf4j
public class VesselServiceImpl implements VesselService2 {

	@Autowired
	protected VesselRepository vesselRepository;

	@Autowired
	protected VesselSnapshotRepository vesselSnapshotRepository;

	@Override
	public List<VesselSnapshotVO> findSnapshotByFilter(VesselFilterVO filter, Page page,
													   DataFetchOptions fetchOptions) {
		return ((VesselFeaturesSpecifications<VesselFeatures, VesselSnapshotVO, VesselFilterVO, DataFetchOptions>)vesselSnapshotRepository)
			.findAll(VesselFilterVO.nullToEmpty(filter), page, fetchOptions);
	}

	@Override
	public List<VesselVO> findAll(VesselFilterVO filter, Page page,
								  DataFetchOptions fetchOptions) {

		return vesselRepository.findAll(
			VesselFilterVO.nullToEmpty(filter),
			page,
			fetchOptions);
	}

	@Override
	public Long countByFilter(VesselFilterVO filter) {
		return vesselRepository.count(VesselFilterVO.nullToEmpty(filter));
	}

	@Override
	public VesselVO get(int id) {
		return vesselRepository.get(id);
	}

	@Override
	public VesselSnapshotVO getSnapshotByIdAndDate(int vesselId, Date date) {
		return vesselSnapshotRepository.getByVesselIdAndDate(vesselId, date, DataFetchOptions.MINIMAL)
			.orElseGet(() -> {
				VesselSnapshotVO unknownVessel = new VesselSnapshotVO();
				unknownVessel.setId(vesselId);
				unknownVessel.setName("Unknown vessel " + vesselId); // TODO remove string
				return unknownVessel;
			});
	}
}
