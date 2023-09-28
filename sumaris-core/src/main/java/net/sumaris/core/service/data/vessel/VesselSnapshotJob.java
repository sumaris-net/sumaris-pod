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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.nuiton.i18n.I18n;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Date;
import java.util.Optional;

@Component("vesselSnapshotJob")
@RequiredArgsConstructor
@Slf4j
public class VesselSnapshotJob{

	protected final SumarisConfiguration configuration;

	protected final VesselSnapshotService service;

	private final Optional<JobExecutionService> jobExecutionService;

	private boolean enable = false;

	@Async
	@TransactionalEventListener(
		value = {ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class},
		phase = TransactionPhase.AFTER_COMPLETION)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onConfigurationReady(ConfigurationEvent event) {

		// Detect changes
		if (enable != configuration.isElasticsearchVesselSnapshotEnabled()) {
			enable = configuration.isElasticsearchVesselSnapshotEnabled();

			// Execute once (other execution will be done by scheduler)
			indexVesselSnapshots();
		}
	}


	@Scheduled(cron = "${sumaris.extraction.scheduling.hourly.cron:0 0 * * * ?}")
	public void indexVesselSnapshots() {
		if (!enable) return; // Skip

		// Compute vessel min update date (= max(update_date))
		Date minUpdateDate = service.getMaxIndexedUpdateDate().orElse(null);

		Date today = Dates.resetTime(new Date());
		VesselFilterVO filter = VesselFilterVO.builder()
			.programLabel(ProgramEnum.SIH.getLabel())
			.statusIds(Lists.newArrayList(StatusEnum.TEMPORARY.getId(), StatusEnum.ENABLE.getId()))
			.minUpdateDate(minUpdateDate)
			.startDate(today)
			.endDate(today)
			.build();


		if (minUpdateDate == null) {
			log.info(I18n.t("sumaris.elasticsearch.vessel.snapshot.job.name"));
		}
		else {
			log.info(I18n.t("sumaris.elasticsearch.vessel.snapshot.log.since", Dates.formatDate(minUpdateDate, Dates.CSV_DATE_TIME)));
		}

		// Use job service
		if (this.jobExecutionService.isPresent()) {
			JobExecutionService jobExecutionService = this.jobExecutionService.get();
			JobVO job = JobVO.builder()
				.type(JobTypeEnum.VESSEL_SNAPSHOTS_INDEXATION.name())
				.name(I18n.t("sumaris.elasticsearch.vessel.snapshot.job.name"))
				.issuer(JobVO.SYSTEM_ISSUER)
				.build();


			// Execute importJob by JobService (async)
			jobExecutionService.run(job,
				() -> filter,
				(progression) -> service.asyncIndexVesselSnapshots(filter, progression));
		}
		else {
			service.indexVesselSnapshots(filter);
		}
	}


}
