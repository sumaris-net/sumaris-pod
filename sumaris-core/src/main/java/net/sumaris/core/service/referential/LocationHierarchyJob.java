package net.sumaris.core.service.referential;

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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.vo.technical.job.JobVO;
import org.nuiton.i18n.I18n;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.concurrent.Future;

@Component("locationHierarchyJob")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean({JobExecutionService.class, JobService.class})
public class LocationHierarchyJob {

	private final SumarisConfiguration configuration;

	private final LocationService service;

	private final JobExecutionService jobExecutionService;

	private boolean enable = false;

	@EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
	public void onConfigurationReady(ConfigurationEvent event) {

		boolean enable = configuration.enableTechnicalTablesUpdate() && configuration.enableJobs();
		if (this.enable != enable) {
			this.enable = enable;

			// Init or refresh data
			if (enable) {
				start(JobVO.SYSTEM_ISSUER);
			}
		}
	}

	@Scheduled(cron = "${sumaris.referential.location.hierarchy.scheduling.cron:0 0 * * * ?}") // Hourly by default
	public void schedule() {
		if (!enable) return; // Skip
		start(JobVO.SYSTEM_ISSUER);
	}

	public JobVO start(@NonNull String issuer) {
		if (!enable) throw new SumarisTechnicalException("LocationHierarchy update has been disabled"); // Skip

		// Init a job
		JobVO job = JobVO.builder()
			.type(JobTypeEnum.FILL_LOCATION_HIERARCHY.name())
			.name(I18n.t("sumaris.referential.location.hierarchy.job.name"))
			.issuer(issuer)
			.build();

		// Execute it
		jobExecutionService.run(job, this::asyncExecute);

		return job;
	}

	@Async("jobTaskExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Future<Void> asyncExecute(@Nullable IProgressionModel progression) {

		if (progression == null) {
			ProgressionModel progressionModel = new ProgressionModel();
			progressionModel.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, (event) -> {
				if (event.getNewValue() != null) log.debug(event.getNewValue().toString());
			});
			progression = progressionModel;
		}

		progression.setCurrent(0);
		progression.setTotal(1);
		progression.setMessage(I18n.t("sumaris.referential.location.hierarchy.job.start"));

		// Run update
		service.updateLocationHierarchy();

		progression.setCurrent(1);
		progression.setMessage(I18n.t("sumaris.referential.location.hierarchy.job.success"));

		return new AsyncResult<>(null);
	}
}
