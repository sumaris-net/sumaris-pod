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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.repository.query.Param;
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

	@Value("${sumaris.elasticsearch.vessel.snapshot.page.sleep:0}")
	private long elasticSearchPageSleepTimeMs = 0;

	private final TimeLog timeLog = new TimeLog(VesselSnapshotServiceImpl.class, 500, 1000);
	private final TimeLog longTimeLog = new TimeLog(VesselSnapshotServiceImpl.class, 10 * 60 * 1000 /*10s*/, 60 * 60 * 1000 /*1min*/);
	private final TimeLog indexationTimeLog = new TimeLog(VesselSnapshotServiceImpl.class, 5 * 60 * 1000 /*5 min*/, 20 * 60 * 1000/*20 min*/);

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
			// Use ES repo, if enabled (and ready)
			if (isElasticsearchEnableAndReady()) {
				// Execute ES search
				org.springframework.data.domain.Page<VesselSnapshotVO> result = elasticsearchRepository.findAllAsPage(filter, page, fetchOptions);

				// Put the total into the cache
				if (this.countByFilterCache != null) this.countByFilterCache.put(filter.hashCode(), result.getTotalElements());

				return result.getContent();
			}

			// Use JPA repo
			return repository.findAll(filter, page, fetchOptions);
		}
		finally {
			timeLog.log(startTime, "findAll");
		}
	}


	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.VESSEL_SNAPSHOTS_COUNT_BY_FILTER, key = "#filter.hashCode()")
	public Long countByFilter(@Param("filter") @NonNull VesselFilterVO filter) {
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
	public long putCountByFilterInCache(@Param("filter") VesselFilterVO filter, Long total) {
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
		boolean fullResync = filter.getMinUpdateDate() == null;
		boolean forceRecreate = !fullResync && elasticsearchRepository.count() == 0L;
		if (forceRecreate) {
			filter.setMinUpdateDate(null);
			fullResync = true;
		}

		// If full resync, then mark elasticsearch as not ready
		if (fullResync) {
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
			if (fullResync) {
				elasticsearchRepository.disableReplicas();
			}

			// Collect existing ids
			final Set<Integer> existingVesselFeaturesIds = filter.getMinUpdateDate() != null
				? Beans.getSet(elasticsearchRepository.findAllVesselFeaturesIdsByFilter(
					VesselFilterVO.builder().minUpdateDate(filter.getMinUpdateDate())
						.build()))
				: null;

			// Compute total
			long total = repository.count(filter);
			progression.setTotal(total + 2 /* Add 2 more steps, for deletion and refresh */);

			VesselFetchOptions fetchOptions = VesselFetchOptions.builder()
				.withBasePortLocation(true)
				.withVesselRegistrationPeriod(true)
				.withCountryRegistration(true)
				.build();
			// We try to reduce to 5000, because of mistake in production, when using 10000
			// (See issue sumaris-app#915)
			int pageSize = 5000;
			long offset = 0;
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
						String message = I18n.t("sumaris.elasticsearch.vessel.snapshot.progress", offset, total);
						log.info(message);
						if (offset > 0) {
							progression.setCurrent(offset);
							progression.setMessage(message);
						}

						// Get page's snapshots from the database
						long findAllStartTime = TimeLog.getTime();
						List<VesselSnapshotVO> items = repository.findAll(filter, page, fetchOptions);
						longTimeLog.log(findAllStartTime, "VesselSnapshotRepository.findAll");

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
						long saveAllStartTime = TimeLog.getTime();
						elasticsearchRepository.bulkIndex(items);
						longTimeLog.log(saveAllStartTime, "VesselSnapshotElasticsearchRepository.bulkIndex");

						// Sleep (wait ES finish processing bulk inserts)
						if (elasticSearchPageSleepTimeMs > 0) {
							Thread.sleep(elasticSearchPageSleepTimeMs);
						}

						// Increment counter
						count += items.size();
					} catch (Throwable e) {
						String message = String.format("Error while indexing vessel snapshots (%s/%s): %s", offset, total, e.getMessage());

						// Log first error: log with stack trace
						if (offset == 0) {
							log.error(message, e);
						}
						else {
							// Log without stack trace
							log.error(message);
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
			progression.setCurrent(total + 1);

			long deletes = CollectionUtils.size(existingVesselFeaturesIds);
			boolean hasChanges = inserts.getValue() > 0 || updates.getValue() > 0 || deletes > 0;

			result.setInserts(inserts.getValue());
			result.setUpdates(updates.getValue());
			result.setDeletes(deletes);
			result.setVessels(vesselIds.size());
			result.setErrors(errors.size());

			// Refresh index (Make sure all changes are visible)
			if (hasChanges) elasticsearchRepository.refresh();

			// Enable replicas
			if (fullResync) {
				elasticsearchRepository.enableReplicas();
			}

			// Propagate the first error, if any
			if (!errors.isEmpty()) {
				elasticsearchIndexationReady = false;
				log.error(I18n.t("sumaris.elasticsearch.vessel.snapshot.failed", errors.get(0)));
				throw new SumarisTechnicalException(errors.get(0));
			}

			progression.setCurrent(total + 2);
			String finalMessage = I18n.t("sumaris.elasticsearch.vessel.snapshot.success", count);
			progression.setMessage(finalMessage);
			if (hasChanges) log.info(finalMessage);
			else log.debug(finalMessage);

			elasticsearchIndexationReady = true;
			this.indexationTimeLog.log(startTime, "indexVesselSnapshots");

		} finally {
			indexing = false;
		}
	}

	/* -- private methods -- */

	private boolean isElasticsearchEnableAndReady() {
		return this.elasticsearchRepository != null && this.elasticsearchIndexationReady;
	}

}
