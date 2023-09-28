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

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.optimization.vessel.VesselSnapshotElasticsearchRepository;
import net.sumaris.core.dao.technical.optimization.vessel.VesselSnapshotElasticsearchSpecifications;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.UpdateVesselSnapshotsResultVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static org.nuiton.i18n.I18n.t;

@Service("vesselSnapshotService")
@RequiredArgsConstructor
@Slf4j
public class VesselSnapshotServiceImpl implements VesselSnapshotService {

	protected final SumarisConfiguration configuration;

	protected final VesselSnapshotRepository repository;

	protected final Optional<VesselSnapshotElasticsearchRepository> elasticsearchRepository;

	@Override
	public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
										  net.sumaris.core.dao.technical.Page page,
										  VesselFetchOptions fetchOptions) {
		return repository.findAll(filter, (int)page.getOffset(), page.getSize(), page.getSortBy(), page.getSortDirection(), fetchOptions);
	}

	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER)
	public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
										   int offset, int size,
										   String sortAttribute, SortDirection sortDirection,
										   VesselFetchOptions fetchOptions) {
		return repository.findAll(filter, offset, size, sortAttribute, sortDirection, fetchOptions);
	}
	@Override
	public Long countByFilter(@NonNull VesselFilterVO filter) {
		return repository.count(filter);
	}

	@Override
	public VesselSnapshotVO getByIdAndDate(int vesselId, Date date) {
		return repository.getByVesselIdAndDate(vesselId, date, VesselFetchOptions.DEFAULT)
			.orElseGet(() -> {
				VesselSnapshotVO unknownVessel = new VesselSnapshotVO();
				unknownVessel.setId(vesselId);
				unknownVessel.setName("Unknown vessel " + vesselId); // TODO remove string
				return unknownVessel;
			});
	}


	@Override
	public Future<UpdateVesselSnapshotsResultVO> asyncIndexVesselSnapshots(@NonNull VesselFilterVO filter,
                                                                           @Nullable IProgressionModel progression) {

		if (progression == null) {
			ProgressionModel progressionModel = new ProgressionModel();
			progressionModel.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, (event) -> {
				if (event.getNewValue() != null) log.debug(event.getNewValue().toString());
			});
			progression = progressionModel;
		}

		UpdateVesselSnapshotsResultVO result = UpdateVesselSnapshotsResultVO.builder()
            .build();
		try {
			indexVesselSnapshots(result, filter, progression);

			// Set result status
			result.setStatus(result.hasError() ? JobStatusEnum.ERROR : JobStatusEnum.SUCCESS);

		} catch (Exception e) {
			result.setMessage(t("sumaris.job.error.detail", ExceptionUtils.getStackTrace(e)));

			// Set failed status
			result.setStatus(JobStatusEnum.FATAL);
		}
		return new AsyncResult<>(result);
	}

	@Override
	public Optional<Date> getMaxIndexedUpdateDate() {
		return elasticsearchRepository.flatMap(VesselSnapshotElasticsearchSpecifications::findMaxUpdateDate);
	}

	@Override
	public UpdateVesselSnapshotsResultVO indexVesselSnapshots(@NonNull VesselFilterVO filter) {
		UpdateVesselSnapshotsResultVO result = UpdateVesselSnapshotsResultVO.builder().build();

		ProgressionModel progression = new ProgressionModel();
		progression.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, (event) -> {
			if (event.getNewValue() != null) log.debug(event.getNewValue().toString());
		});

		indexVesselSnapshots(result, filter, progression);

		return result;
	}

	/* -- protected methods -- */

	protected void indexVesselSnapshots(
		@NonNull UpdateVesselSnapshotsResultVO result,
		@NonNull VesselFilterVO filter,
		@NonNull IProgressionModel progression) {

		VesselSnapshotElasticsearchRepository elasticsearchRepository = this.elasticsearchRepository
			.orElseThrow(() -> new IllegalArgumentException("Cannot index vessel snapshots, because indexation has been disabled"));

		progression.setCurrent(0L);
		result.setErrors(0);
		result.setVesselCount(0);
        result.setMinUpdateDate(filter.getMinUpdateDate());

		VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
			.withBasePortLocation(true)
			.build();
		long offset = 0;
		int pageSize = 10000;
		int count = 0;
		List<Exception> errors = Lists.newArrayList();

		// Compute total
		long total = repository.count(filter);

		if (total > 0) {
			progression.setTotal(total);
			Page page = Page.builder()
				.offset(offset)
				.size(pageSize)
				.sortBy(IEntity.Fields.ID)
				.sortDirection(SortDirection.ASC)
				.build();
			do {
				try {
					// Update progression
					if (offset > 0) {
						progression.setCurrent(offset);
						progression.setMessage(I18n.t("sumaris.elasticsearch.vessel.snapshot.progress",
							VesselSnapshotVO.INDEX, offset, total));
					}

					// Get page's snapshots from the database
					List<VesselSnapshotVO> items = repository.findAll(filter, page, fetchOptions);

					// Save page's snapshots into elasticsearch
					elasticsearchRepository.saveAll(items);

					// Increment counter
					count += items.size();
				}
				catch (Exception e) {
					// Log first error
					if (offset == 0) {
						log.error("Error while indexing vessel snapshots: ", e);
					}
					errors.add(e);
					result.setErrors(errors.size());
					// Continue to the next page
				}

				offset += pageSize;
				page.setOffset(offset);

				// Update the total (e.g. some element has been add since the count query)
				if (count > total) {
					total = count;
					progression.setTotal(total);
				}
			} while (offset < total);
		}
		result.setVesselCount(count);
		result.setErrors(errors.size());

		// Propagate the first error, if any
		if (!errors.isEmpty()) {
			throw new SumarisTechnicalException(errors.get(0));
		}

		progression.setCurrent(total);
		progression.setMessage(I18n.t("sumaris.elasticsearch.vessel.snapshot.success", count));

	}


}
