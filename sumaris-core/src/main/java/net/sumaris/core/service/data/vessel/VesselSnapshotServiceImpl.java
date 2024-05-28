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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.data.vessel.VesselSnapshotRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.elasticsearch.vessel.VesselSnapshotElasticsearchRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.vessel.UpdateVesselSnapshotsResultVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.VesselFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.nuiton.i18n.I18n;
import org.nuiton.util.TimeLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service("vesselSnapshotService")
@RequiredArgsConstructor
@Slf4j
public class VesselSnapshotServiceImpl implements VesselSnapshotService {


	protected final SumarisConfiguration configuration;

	protected final VesselSnapshotRepository repository;

	protected final ProgramRepository programRepository;

	protected final VesselSnapshotElasticsearchRepository elasticsearchRepository;

	private boolean indexing = false;
	private boolean elasticsearchIndexationReady = false;

	private final CacheManager cacheManager;
	private Cache countByFilterCache = null;

	private final TimeLog timeLog = new TimeLog(VesselSnapshotServiceImpl.class, 500, 1000);

	@Autowired
	public VesselSnapshotServiceImpl(SumarisConfiguration configuration,
									 VesselSnapshotRepository repository,
									 ProgramRepository programRepository,
									 Optional<VesselSnapshotElasticsearchRepository> elasticsearchRepository,
									 Optional<CacheManager> cacheManager) {
		this.configuration = configuration;
		this.repository = repository;
		this.programRepository = programRepository;
		this.elasticsearchRepository = elasticsearchRepository.orElse(null);
		this.cacheManager = cacheManager.orElse(null);
	}

	@PostConstruct
	public void init() {
		if (cacheManager != null) {
			this.countByFilterCache = cacheManager.getCache(CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER);
		}
	}

	@Override
	public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
										  int offset, int size,
										  String sortAttribute, SortDirection sortDirection,
										  VesselFetchOptions fetchOptions) {
		return this.findAll(filter, Page.create(offset, size, sortAttribute, sortDirection), fetchOptions);
	}

	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOTS_BY_FILTER)
	public List<VesselSnapshotVO> findAll(@NonNull VesselFilterVO filter,
										  net.sumaris.core.dao.technical.Page page,
										  VesselFetchOptions fetchOptions) {
		long startTime = TimeLog.getTime();
		try {
			if (isElasticsearchEnableAndReady()) {
				// Execute ES search
				org.springframework.data.domain.Page<VesselSnapshotVO> result = elasticsearchRepository.findAllAsPage(filter, page, fetchOptions);

				// Put the total into the cache
				if (this.countByFilterCache != null) this.countByFilterCache.put(filter.hashCode(), result.getTotalElements());

				return result.getContent();
			}
			return repository.findAll(filter, page, fetchOptions);
		}
		finally {
			timeLog.log(startTime, "findAll");
		}
	}


	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER, key = "#filter.hashCode()")
	public Long countByFilter(@NonNull VesselFilterVO filter) {
		long startTime = TimeLog.getTime();
		try {
			if (isElasticsearchEnableAndReady()) {
				return elasticsearchRepository.count(filter);
			}
			return repository.count(filter);
		}
		finally {
			timeLog.log(startTime, "countByFilter");
		}
	}

	@Caching(
		put = {
			@CachePut(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER, key = "#filter.hashCode()")
		}
	)
	public long putCountByFilterInCache(VesselFilterVO filter, Long total) {
		return total;
	}


	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOT_BY_ID_AND_DATE)
	public VesselSnapshotVO getByIdAndDate(final int vesselId, @Nullable Date date) {

		long startTime = TimeLog.getTime();
		try {
			// Load using elasticsearch
			if (isElasticsearchEnableAndReady()) {
				VesselSnapshotVO vessel = elasticsearchRepository.findByVesselIdAtDate(vesselId, date)
					.orElse(null);
				if (vessel != null) return vessel;
				// Continue if not found
				log.debug("Vessel #{} was not found in elasticsearch. Please make sure all vessels has been indexed.", vesselId);
			}

			// Load using database
			return repository.findByVesselIdAndDate(vesselId, date, VesselFetchOptions.DEFAULT)
				.orElseGet(() -> {
					VesselSnapshotVO unknownVessel = new VesselSnapshotVO();
					unknownVessel.setVesselId(vesselId);
					unknownVessel.setName("Unknown result " + vesselId); // TODO remove string
					return unknownVessel;
				});
		}
		finally {
			timeLog.log(startTime, "getByIdAndDate");
		}
	}

	@Override
	public Optional<Date> getMaxIndexedUpdateDate() {
		if (elasticsearchRepository == null || elasticsearchRepository.count() == 0) return Optional.empty();
		return elasticsearchRepository.findMaxUpdateDate();
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

	@Override
	public boolean isIndexing() {
		return indexing;
	}


	public void indexVesselSnapshots(
		@NonNull UpdateVesselSnapshotsResultVO result,
		@NonNull VesselFilterVO filter,
		@NonNull IProgressionModel progression) {
		Preconditions.checkNotNull(this.elasticsearchRepository, "Elasticsearch vessel snapshot has been disabled");
		Preconditions.checkArgument(!indexing, "Vessels indexation already running. Please retry later");

		// Force full resync if no data
		boolean forceRecreate = filter.getMinUpdateDate() != null && elasticsearchRepository.count() == 0L;
		if (forceRecreate) {
			filter.setMinUpdateDate(null);
		}

		// If full resync, then mark elasticsearch as not ready
		if (filter.getMinUpdateDate() == null) {
			elasticsearchIndexationReady = false;
		}

		long startTime = TimeLog.getTime();
		final Optional<ProgramVO> filteredProgram = Optional.ofNullable(filter.getProgramLabel())
			.map(label -> programRepository.getByLabel(label, ProgramFetchOptions.MINIMAL));

		indexing = true;

		try {
			progression.setCurrent(0L);
			result.setErrors(0);
			result.setVessels(0);
			result.setFilterStartDate(filter.getStartDate());
			result.setFilterMinUpdateDate(filter.getMinUpdateDate());

			// Drop existing index
			if (forceRecreate) {
				elasticsearchRepository.recreate();
			}

			// Collect existing ids
			final Set<Integer> existingVesselFeaturesIds = filter.getMinUpdateDate() != null
				? Beans.getSet(elasticsearchRepository.findAllVesselFeaturesIdsByFilter(
					VesselFilterVO.builder().minUpdateDate(filter.getMinUpdateDate())
						.build()))
				: null;

			// Compute total
			long total = repository.count(filter);
			progression.setTotal(total + 1 /* Add one more step, for deletion */);

			VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
				.withBasePortLocation(true)
				.withVesselRegistrationPeriod(true)
				.withCountryRegistration(true)
				.build();
			long offset = 0;
			int pageSize = 10000;
			int count = 0;
			MutableInt updates = new MutableInt(0);
			MutableInt inserts = new MutableInt(0);
			List<Throwable> errors = Lists.newArrayList();
			Set<Integer> vesselIds = Sets.newHashSet();

			if (total > 0) {
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
								offset, total));
						}

						// Get page's snapshots from the database
						List<VesselSnapshotVO> items = repository.findAll(filter, page, fetchOptions);

						items.forEach(vessel -> {
							vesselIds.add(vessel.getVesselId());
							if (vessel.getEndDate() == null) {
								vessel.setEndDate(VesselSnapshotElasticsearchRepository.DEFAULT_END_DATE);
							}

							// Apply default program
							if (vessel.getProgram() == null && filteredProgram.isPresent()) {
								vessel.setProgram(filteredProgram.get());
							}

							if (existingVesselFeaturesIds == null || !existingVesselFeaturesIds.remove(vessel.getVesselFeaturesId())) {
								inserts.increment();
							}
							else {
								updates.increment();
							}
						});

						// Save page's snapshots into elasticsearch
						elasticsearchRepository.saveAll(items);

						// Increment counter
						count += items.size();
					} catch (Throwable e) {
						// Log first error
						if (offset == 0) {
							log.error("Error while indexing vessel snapshots: ", e);
						}
						errors.add(e);
						result.setErrors(errors.size());
						break;
						// Continue to the next page
					}

					offset += pageSize;
					page.setOffset(offset);

					// Update the total (e.g. some element has been added since the count query)
					if (count > total) {
						total = count;
						progression.setTotal(total + 1);
					}
				} while (offset < total);
			}

			progression.setCurrent(total);

			// Delete old documents
			if (CollectionUtils.isNotEmpty(existingVesselFeaturesIds)) {
				progression.setMessage(I18n.t("sumaris.elasticsearch.vessel.snapshot.removing", existingVesselFeaturesIds.size()));
				elasticsearchRepository.deleteAllById(existingVesselFeaturesIds);
			}

			result.setInserts(inserts.getValue());
			result.setUpdates(updates.getValue());
			result.setDeletes(CollectionUtils.size(existingVesselFeaturesIds));
			result.setVessels(vesselIds.size());
			result.setErrors(errors.size());

			// Propagate the first error, if any
			if (!errors.isEmpty()) {
				elasticsearchIndexationReady = false;
				throw new SumarisTechnicalException(errors.get(0));
			}

			progression.setCurrent(total + 1);
			progression.setMessage(I18n.t("sumaris.elasticsearch.vessel.snapshot.success", count));
			elasticsearchIndexationReady = true;
			this.timeLog.log(startTime, "indexVesselSnapshots");

		} finally {
			indexing = false;
		}
	}

	/* -- private methods -- */

	private boolean isElasticsearchEnableAndReady() {
		return this.elasticsearchRepository != null && this.elasticsearchIndexationReady;
	}

}
