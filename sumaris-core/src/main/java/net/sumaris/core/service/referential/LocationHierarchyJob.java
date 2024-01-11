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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
				updateLocationHierarchy(JobVO.SYSTEM_ISSUER);
			}
		}
	}

	@Scheduled(cron = "${sumaris.referential.location.hierarchy.scheduling.cron:0 0 * * * ?}") // Hourly by default
	public void updateLocationHierarchy() {
		if (!enable) return; // Skip
		updateLocationHierarchy(JobVO.SYSTEM_ISSUER);
	}

	public JobVO updateLocationHierarchy(@NonNull String issuer) {
		if (!enable) throw new SumarisTechnicalException("LocationHierarchy update has been disabled"); // Skip

		// Init a job
		JobVO job = JobVO.builder()
			.type(JobTypeEnum.LOCATION_HIERARCHY.name())
			.name(I18n.t("sumaris.referential.location.hierarchy.job.name"))
			.issuer(issuer)
			.build();

		// Execute it
		jobExecutionService.run(job, service::asyncUpdateLocationHierarchy);

		return job;
	}
}
