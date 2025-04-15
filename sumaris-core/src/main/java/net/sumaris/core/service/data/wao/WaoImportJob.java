package net.sumaris.core.service.data.wao;

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

import com.exasol.parquetio.data.Row;
import com.exasol.parquetio.reader.RowParquetReader;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.administration.samplingScheme.DenormalizedSamplingStrataService;
import net.sumaris.core.service.referential.taxon.TaxonGroupService;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.vo.administration.samplingScheme.DenormalizedSamplingStrataVO;
import net.sumaris.core.vo.administration.samplingScheme.SamplingStrataFilterVO;
import net.sumaris.core.vo.data.vessel.UpdateVesselSnapshotsResultVO;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.taxon.TaxonGroupVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Component("WaoImportJob")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean({JobExecutionService.class, JobService.class})
public class WaoImportJob {

	private final SumarisConfiguration configuration;

	private final TaxonGroupService taxonGroupService;

	private final DenormalizedSamplingStrataService denormalizedSamplingStrataService;

	private final JobExecutionService jobExecutionService;

	private boolean enable = false;

	@EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
	public void onConfigurationReady() {

		boolean enable = configuration.enableWaoUpdate() && configuration.enableJobs();
		if (this.enable != enable) {
			this.enable = enable;

			// Init or refresh data
			if (enable) {
				start(JobVO.SYSTEM_ISSUER);
			}
		}
	}

	@Scheduled(cron = "${sumaris.wao.scheduling.cron:0 0 * * * ?}")
	public void schedule() {
		if (!enable) return; // Skip
		start(JobVO.SYSTEM_ISSUER);
	}

	/* -- protected functions -- */

	public JobVO start(@NonNull String issuer) {
		if (!enable) throw new SumarisTechnicalException("WAO importation has been disabled"); // Skip

		// Use job service
		JobVO job = JobVO.builder()
			.type(JobTypeEnum.WAO_IMPORTATION.name())
			.name(I18n.t("sumaris.wao.job.name"))
			.issuer(issuer)
			.build();

		// Execute importJob by JobService (async)
		jobExecutionService.run(job, this::asyncExecute);

		return job;
	}

	@Async("jobTaskExecutor")
	@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
	public Future<UpdateVesselSnapshotsResultVO> asyncExecute(@Nullable IProgressionModel progression) {

		this.enable = false;	// TODO: remove this line

		if (progression == null) {
			ProgressionModel progressionModel = new ProgressionModel();
			progressionModel.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, (event) -> {
				if (event.getNewValue() != null) log.debug(event.getNewValue().toString());
			});
			progression = progressionModel;
		}

		progression.setCurrent(0);
		progression.setTotal(1);
		progression.setMessage(I18n.t("sumaris.wao.job.start"));

		// Run import
		//final org.apache.hadoop.fs.Path path = new Path("C:\\Projets\\IMAGINE\\sumaris-pod\\sumaris-core\\src\\main\\resources\\parquet-input\\CONTACTS.parquet");
		final org.apache.hadoop.fs.Path path = new Path(configuration.getWaoDirectory().getAbsolutePath(), "CONTACTS.parquet");
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();

			// Retrieve index of the fields
			final int speciesToObserveIndex = row.getFieldNames().indexOf("ESPECES_OBSERVEES");
			if (speciesToObserveIndex == -1) {
				throw new Exception("Field 'ESPECES_OBSERVEES' not found");
			}
			final int samplingStrataIndex = row.getFieldNames().indexOf("PLAN_CODE");
			if (samplingStrataIndex == -1) {
				throw new Exception("Field 'PLAN_CODE' not found");
			}

			while (row != null) {
				//List<Object> values = row.getValues();

				// Retrieve species to observe
				Object speciesToObserveObj = row.getValue(speciesToObserveIndex);
				if (speciesToObserveObj == null) {
					row = reader.read();
					continue;
				}
				String speciesToObserveStr = (String) speciesToObserveObj;	// TODO: Change to "Species to observe"
				Stream<String> speciesToObserve = Arrays.stream(speciesToObserveStr.split(",")).distinct();
				long countSpeciesToObserve = Arrays.stream(speciesToObserveStr.split(",")).distinct().count();

				// Resolve species
				TaxonGroupVO[] taxonGroups = speciesToObserve
						.map(String::trim)
						.map(label -> this.taxonGroupService.findAllByFilter(ReferentialFilterVO.builder()
								.label(label)
								.statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
								.build()
						).stream().findFirst())
						.filter(Optional::isPresent)
						.map(Optional::get)
						.toArray(TaxonGroupVO[]::new);


				log.info("input {} - output {}", countSpeciesToObserve, taxonGroups.length);
				if (taxonGroups.length - countSpeciesToObserve > 0) {
					log.warn("Some species were not found.");
				}

				// Retrieve sampling strata
				Object samplingStrataObj = row.getValue(samplingStrataIndex);
				if (samplingStrataObj == null) {
					row = reader.read();
					continue;
				}
				String samplingStrataStr = (String) samplingStrataObj;
				// TODO: List<DenormalizedSamplingStrataVO> samplingStratas = denormalizedSamplingStrataService.findByFilter(SamplingStrataFilterVO.builder().label(samplingStrataStr).build())

				// Next line
				row = reader.read();
			}
		} catch (final IOException exception) {
			log.error("IO Error:  {}", exception.getMessage());
		}
		catch (final Exception exception) {
			log.error("Error:  {}", exception.getMessage());
		}

		progression.setCurrent(1);
		progression.setMessage(I18n.t("sumaris.wao.job.success"));

		return new AsyncResult<>(null);
	}
}
