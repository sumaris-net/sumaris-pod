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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component("vesselSnapshotJob")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean({JobExecutionService.class, JobService.class})
public class VesselSnapshotJob {

	private final SumarisConfiguration configuration;

	private final VesselSnapshotService service;

	private final JobExecutionService jobExecutionService;

	private final JobService jobService;

	@Value("${sumaris.elasticsearch.vessel.snapshot.scheduling.nbYears:-1}")
	private Integer nbYears;

	private boolean enable = false;

	private List<Integer> vesselTypeIds;

	@EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
	public void onConfigurationReady(ConfigurationEvent event) {

		boolean enable = configuration.enableElasticsearchVesselSnapshot() && configuration.enableJobs();


		if (this.enable != enable) {
			this.enable = enable;

			this.vesselTypeIds = configuration.getDataVesselTypeIds();

			// Init or refresh data
			if (enable) {
				indexVesselSnapshots();
			}
		}
	}


	@Scheduled(cron = "${sumaris.elasticsearch.vessel.snapshot.scheduling.cron:0 0 * * * ?}") // Hourly by default
	public void indexVesselSnapshots() {
		if (!enable) return; // Skip

		getLastSuccessJob()
			.ifPresentOrElse(job -> {
				VesselFilterVO currentFilter = createFilter();
				VesselFilterVO previousFilter = this.getJobFilter(job).orElse(null);

				boolean sameFilter = false;
				if (previousFilter != null) {
					previousFilter.setMinUpdateDate(null); // Remove date before calling equals()
					sameFilter = previousFilter.equals(currentFilter);
				}

				if (sameFilter) {
					if (job.getStartDate() != null) {
						Date lastExecutionDate = Dates.addHours(job.getStartDate(), -12);
						currentFilter.setMinUpdateDate(lastExecutionDate);
					}
					else {
						Date maxIndexedUpdateDate = service.getMaxIndexedUpdateDate().orElse(null);
						currentFilter.setMinUpdateDate(maxIndexedUpdateDate);
					}
				}

				indexVesselSnapshots(JobVO.SYSTEM_ISSUER, currentFilter);
			},
			// First load
			() -> indexVesselSnapshots(JobVO.SYSTEM_ISSUER, (Date)null));

	}

	public JobVO indexVesselSnapshots(@NonNull String issuer, Date minUpdateDate) {
		VesselFilterVO filter = createFilter();

		// Compute vessel min update date (= max(update_date))
		if (minUpdateDate != null) {
			filter.setMinUpdateDate(minUpdateDate);
		}

		return indexVesselSnapshots(issuer, filter);
	}

	/* -- protected functions -- */

	protected JobVO indexVesselSnapshots(@NonNull String issuer, @NonNull VesselFilterVO filter) {
		if (!enable) throw new SumarisTechnicalException("Elasticsearch indexation has been disabled"); // Skip
		if (service.isIndexing()) throw new SumarisTechnicalException("Elasticsearch indexation already running");

		if (filter.getMinUpdateDate() == null) {
			log.info(I18n.t("sumaris.elasticsearch.vessel.snapshot.job.name"));
		}
		else {
			log.info(I18n.t("sumaris.elasticsearch.vessel.snapshot.log.since", Dates.formatDate(filter.getMinUpdateDate(), Dates.CSV_DATE_TIME)));
		}

		// Use job service
		JobVO job = JobVO.builder()
			.type(JobTypeEnum.VESSEL_SNAPSHOTS_INDEXATION.name())
			.name(I18n.t("sumaris.elasticsearch.vessel.snapshot.job.name"))
			.issuer(issuer)
			.build();

		// Execute importJob by JobService (async)
		jobExecutionService.run(job,
			() -> filter,
			(progression) -> service.asyncIndexVesselSnapshots(filter, progression));

		return job;
	}

	protected VesselFilterVO createFilter() {
		VesselFilterVO.VesselFilterVOBuilder filter = VesselFilterVO.builder()
			.programLabel(ProgramEnum.SIH.getLabel())
			;

		// Filter on vessel types
		if (CollectionUtils.isNotEmpty(this.vesselTypeIds)) {
			filter.vesselTypeIds(this.vesselTypeIds.toArray(Integer[]::new));
		}
		else {
			// Use known vessel type (except UNKNOWN or unresolved Enum)
			filter.vesselTypeIds(Arrays.stream(VesselTypeEnum.values())
				.filter(vt -> vt != VesselTypeEnum.UNKNOWN)
				.map(VesselTypeEnum::getId)
				.filter(id -> id != null && id >= 0)
				.toArray(Integer[]::new));
		}

		// Add start date
		if (this.nbYears != null && this.nbYears > 0) {
			Date startDate = Dates.resetTime(Dates.getFirstDayOfYear(Dates.getYear(new Date()) - this.nbYears));
			filter.startDate(startDate);
		}

		return filter.build();
	}

	protected Optional<JobVO> getLastSuccessJob() {
		List<JobVO> lastJobs = jobService.findAll(JobFilterVO.builder()
			.types(new String[]{JobTypeEnum.VESSEL_SNAPSHOTS_INDEXATION.name()})
			.status(new JobStatusEnum[]{JobStatusEnum.SUCCESS, JobStatusEnum.WARNING})
			.build(), Page.builder()
			.offset(0)
			.size(1)
			.sortBy(JobVO.Fields.UPDATE_DATE)
			.sortDirection(SortDirection.DESC)
			.build());

		if (CollectionUtils.isEmpty(lastJobs)) return Optional.empty();

		return Optional.of(lastJobs.get(0));
	}

	protected Optional<VesselFilterVO> getJobFilter(@Nullable JobVO job) {
		if (job == null) return Optional.empty();
		VesselFilterVO filter = jobExecutionService.readConfiguration(job, VesselFilterVO.class);
		return Optional.ofNullable(filter);
	}
}
