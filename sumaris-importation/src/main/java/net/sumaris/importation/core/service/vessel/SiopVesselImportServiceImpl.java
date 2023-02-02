/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.importation.core.service.vessel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.event.job.JobEndEvent;
import net.sumaris.core.event.job.JobProgressionEvent;
import net.sumaris.core.event.job.JobProgressionVO;
import net.sumaris.core.event.job.JobStartEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.VesselTypeEnum;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.data.vessel.VesselService;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.data.VesselVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportContextVO;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportResultVO;
import net.sumaris.importation.core.util.csv.CSVFileReader;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableShort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

@Service("siopVesselLoaderService")
@RequiredArgsConstructor
@Slf4j
public class SiopVesselImportServiceImpl implements SiopVesselImportService {

	protected final static String LABEL_NAME_SEPARATOR_REGEXP = "[ \t]+-[ \t]+";
	protected static final String[] INPUT_DATE_PATTERNS = new String[] {
		"dd/MM/yyyy"
	};
	protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
		// Siop vessel synonyms (for LPDB)
		.put("Num.ro CFR", StringUtils.doting(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE))
		.put("Quart[.] mar.", StringUtils.doting(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION))
		.put("Immatr[.]", StringUtils.doting(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, VesselRegistrationPeriod.Fields.REGISTRATION_CODE))
		.put("Nom", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME))
		.put("Jauge", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.GROSS_TONNAGE_GT))
		.put("Longueur HT", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.LENGTH_OVER_ALL))
		.put("Puissance motrice", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.ADMINISTRATIVE_POWER))
		.put("Lieu vente 1", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.BASE_PORT_LOCATION, "4"))
		.put("Lieu vente 2", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.BASE_PORT_LOCATION, "3"))
		.put("Lieu débarquement 1", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.BASE_PORT_LOCATION))
		.put("Lieu débarquement 2", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.BASE_PORT_LOCATION, "2"))
		.put("Date adh.", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.START_DATE))
		.put("Date Départ", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.END_DATE))
		.put("Indicatif radio", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.IRCS))
		.put("Année mise en service", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.CONSTRUCTION_YEAR))
		.put("Comment. 1", StringUtils.doting(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.COMMENTS))

		.build();

	protected static final Map<String, LocationVO> harbourReplacements = ImmutableMap.<String, String>builder()
		// Lieu débarquement -> Harbour
		.put("Ondarroa", "ESOND - Ondarroa")
		.put("CASTLETOWNBERE", "IECSW - Castletownbere")
		.put("KILLYBEGS", "IEKBS - Killybegs")
		.put("La Coruña", "ESLCG - La Coruña")
		.put("Lochinver", "GBLOV - Lochinver")
		.put("Pasajes", "ESPAS - Pasajes")
		.put("Saint-Guénolé (JBE_V3)", "FRGN2 - Saint-Guénolé (Penmarch)")
		.put("Yeu port / Joinville (L'Ile-d'Yeu)", "FRPRJ - Port-Joinville (L''Ile-d''Yeu)")
		.put("Quiberon", "FRQUI - Quiberon (Port-Maria)")
		.put("Plougasnou", "FRPLO - Plougasnou (Le Diben-Primel)")
		.put("Roscoff", "FRGMX - Roscoff")

		// CRIEE -> Harbour
		.put("CRIEE CONCARNEAU", "FRCOC - Concarneau")
		.put("CRIEE QUIBERON", "FRQUI - Quiberon (Port-Maria)")
		.put("CRIEE ST GUENOLE", "FRGN2 - Saint-Guénolé (Penmarch)")
		.put("CRIEE LE CROISIC", "FROII - Le Croisic")
		.put("CRIEE SAINT QUAY", "FRHSB - Saint-Quay-Portrieux")
		.put("CRIEE LOGUIVY", "FRGPA - Loguivy de la mer (Ploubazlanec)")
		.put("CRIEE LORIENT", "FRLRT - Lorient")
		.put("CRIEE ERQUY", "FRLFY - Erquy")
		.put("CRIEE AUDIERNE", "FRAUD - Audierne")
		.put("CRIEE ROSCOFF", "FRGMX - Roscoff")
		.put("CRIEE BREST", "FRBES - Brest")
		.put("CRIEE LE GUILVINEC", "FRGVC - Guilvinec")
		.put("CRIEE DOUARNENEZ", "FRDRZ - Douarnenez")
		.put("CRIEE LES SABLES D'OLONNE", "FRLSO - Les Sables-d'Olonne")
		.put("CRIEE LOCTUDY", "FRLOC - Loctudy")
		.put("CRIEE LA TURBALLE", "FRTBE - La Turballe")
		.put("CRIEE SAINT JEAN DE LUZ", "FRZJZ - Saint-Jean-de-Luz, Ciboure")
		.put("CRIEE GRANVILLE", "FRGFR - Granville")
		.build()
		.entrySet()
		.stream()
		.collect(Collectors.toMap(
			Map.Entry::getKey,
			entry -> {
				String[] parts = entry.getValue().split(LABEL_NAME_SEPARATOR_REGEXP);
				LocationVO location = new LocationVO();
				location.setLabel(parts[0]);
				location.setName(parts[1]);
				location.setLevelId(LocationLevelEnum.HARBOUR.getId());
				return location;
			})
		);
	protected Map<Integer, LocationVO> locationByFilterCache = Maps.newConcurrentMap();


	protected final static LocationVO UNRESOLVED_LOCATION = new LocationVO();

	protected static final Date DEFAULT_START_DATE = Dates.safeParseDate("01/01/1990", INPUT_DATE_PATTERNS);
	protected static final String DEFAULT_REGISTRATION_COUNTRY_LABEL = "FRA";


	protected final SumarisConfiguration config;

	protected final ReferentialService referentialService;

	protected final LocationService locationService;

	protected final VesselService vesselService;

	protected final PersonService personService;

	protected final ApplicationContext applicationContext;

	private final ObjectMapper objectMapper;

	private final ApplicationEventPublisher publisher;

	@Override
	public SiopVesselImportResultVO importFromFile(@NonNull SiopVesselImportContextVO context,
												   IProgressionModel progressionModel) throws IOException {
		Files.checkExists(context.getProcessingFile());
		Preconditions.checkNotNull(context.getRecorderPersonId());

		SiopVesselImportResultVO result = context.getResult();

		// Init progression model
		progressionModel = Optional.ofNullable(progressionModel).orElseGet(ProgressionModel::new);
		progressionModel.setMessage(t("sumaris.import.job.start", context.getProcessingFile().getName()));

		PersonVO recorderPerson = personService.getById(context.getRecorderPersonId());

		String uniqueKeyPropertyName = StringUtils.doting(
			VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, VesselRegistrationPeriodVO.Fields.INT_REGISTRATION_CODE
		);

		// Make sure to create all locations
		locationByFilterCache.clear();
		for (LocationVO location : harbourReplacements.values()) {
			this.fillOrCreateLocation(location, LocationLevelEnum.HARBOUR.getId());
		}

		Date startDate = Dates.resetTime(new Date());

		File tempFile = null;
		try {
			tempFile = prepareFile(context.getProcessingFile());

			Set<String> includedHeaders = new HashSet<>(headerReplacements.values());
			String uniqueKeyHeaderName = headerReplacements.entrySet().stream()
				.filter(entry -> uniqueKeyPropertyName.equals(entry.getValue()))
				.map(Map.Entry::getKey)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Cannot resolve CSV header corresponding to the model property " + uniqueKeyPropertyName));

			// Do load
			try (CSVFileReader reader = new CSVFileReader(tempFile, true, true, Charsets.UTF_8.name())) {

				Map<String, Integer> existingKeys = collectExistingVessels(uniqueKeyPropertyName);
				Set<String> processedKeys = Sets.newHashSet();

				MutableShort inserts = new MutableShort(0);
				MutableShort updates = new MutableShort(0);
				MutableShort disables = new MutableShort(0);
				MutableShort errors = new MutableShort(0);
				MutableShort warnings = new MutableShort(0);
				MutableShort rowCounter = new MutableShort(1);
				List<String> logs = new ArrayList<>();

				List<VesselVO> vessels = readRows(reader, includedHeaders).stream()
					.map(this::toVO)
					.collect(Collectors.toList());

				progressionModel.setTotal(vessels.size() + 1 /*disable vessels*/);

				for (VesselVO vessel: vessels) {

					try {
						// Get the unique key
						String uniqueKey = Beans.getProperty(vessel, uniqueKeyPropertyName);
						if (uniqueKey == null) {
							warnings.increment();;
							String message = String.format("Invalid row #%s: no value for the required header '%s'. Skipping", rowCounter, uniqueKeyHeaderName);
							logs.add(message);
							log.warn(message);
						}

						// Check if not already processed (duplicated key)
						else if (processedKeys.contains(uniqueKey)) {
							warnings.increment();
							String message = String.format("Invalid row #%s: duplicated value '%s=%s' (same value has been already processed). Skipping", rowCounter, uniqueKeyHeaderName, uniqueKey);
							logs.add(message);
							log.warn(message);
						}
						else {
							// Fill default properties
							fillVessel(vessel, recorderPerson);

							try {
								boolean isNew = !existingKeys.containsKey(uniqueKey);

								if (isNew) {
									log.debug("Inserting new vessel {} ...", uniqueKey);
									insert(vessel);
									inserts.increment();

								} else {
									log.debug("Updating existing vessel: {}", uniqueKey);
									Integer vesselId = existingKeys.get(uniqueKey);
									vessel.setId(vesselId);
									boolean updated = update(vessel, startDate);
									if (updated) updates.increment();
								}

								processedKeys.add(uniqueKey);
							}
							catch (SumarisBusinessException e) {
								errors.increment();
								String message = String.format("Failed to import vessel %s at line #%s: %s", uniqueKey, rowCounter, e.getMessage());
								logs.add(message);
								log.error(message);
								// Continue
							}
							catch (Exception e) {
								errors.increment();
								String message = String.format("Failed to import vessel %s at line #%s: %s", uniqueKey, rowCounter, e.getMessage());
								logs.add(message);
								log.error(message);
								// Continue
							}
						}
					}
					finally {
						rowCounter.increment();
						if (rowCounter.intValue() % 10 == 0) {
							progressionModel.setCurrent(rowCounter.intValue());
						}
					}
				}
				progressionModel.setCurrent(vessels.size());

				// Disable not present vessels
				Set<Integer> vesselIdsToDisable = existingKeys.entrySet().stream()
					.filter(e -> !processedKeys.contains(e.getKey()))
					.map(Map.Entry::getValue)
					.collect(Collectors.toSet());
				if (CollectionUtils.isNotEmpty(vesselIdsToDisable)) {

					vesselIdsToDisable.forEach(vesselId -> {
						try {
							disable(vesselId, startDate);
							disables.increment();
						} catch (Exception e) {
							if (log.isDebugEnabled()) {
								log.error("Failed to disable vessel #{}: {}", vesselId, e.getMessage(), e);
							}
							else {
								log.error("Failed to disable vessel #{}: {}", vesselId, e.getMessage());
							}
						}
					});
				}

				if (errors.intValue() == 0) {
					String message = String.format("Successfully import vessels. %s inserts, %s updates, %s disables, %s warnings", inserts, updates, disables, warnings);
					logs.add(message);
					log.info(message);
				}
				else {
					String message = String.format("Successfully import vessels. %s inserts, %s updates, %s disables, %s warnings, %s errors", inserts, updates, disables, warnings, errors);
					logs.add(message);
					log.warn(message);
				}

				Set<String> temporaryHarbourNames = findAllTemporaryLocationNames(LocationLevelEnum.HARBOUR.getId());
				if (CollectionUtils.isNotEmpty(temporaryHarbourNames)) {
					String message = String.format("Some temporary harbours exists in database. Please check: name(s):\n\t- %s",
						String.join("\n\t- ", temporaryHarbourNames));
					logs.add(message);
					log.warn(message);
				}

				// Update result
				result.setInserts(inserts.intValue());
				result.setUpdates(updates.intValue());
				result.setDisables(disables.intValue());
				result.setWarnings(warnings.intValue());
				result.setErrors(errors.intValue());

				if (CollectionUtils.isNotEmpty(logs)) {
					result.setMessage(String.join("\n", logs));
				}

				return result;
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		}
		finally {
			Files.deleteQuietly(tempFile);
			Files.deleteTemporaryFiles(context.getProcessingFile());

			locationByFilterCache.clear();
			progressionModel.setCurrent(progressionModel.getTotal());
		}

	}

	@Override
	public Future<SiopVesselImportResultVO> asyncImportFromFile(SiopVesselImportContextVO context, JobVO job) {
		int jobId = job.getId();

		final SiopVesselImportService self = applicationContext.getBean(SiopVesselImportService.class);

		try {
			// Affect context to job (as json)
			job.setConfiguration(objectMapper.writeValueAsString(context));
		} catch (JsonProcessingException e) {
			throw new SumarisTechnicalException(e);
		}

		// Publish job start event
		publisher.publishEvent(new JobStartEvent(jobId, job));

		// Create progression model and listener to throttle events
		ProgressionModel progressionModel = new ProgressionModel();
		io.reactivex.rxjava3.core.Observable<JobProgressionVO> progressionObservable = Observable.create(emitter -> {

			// Create listener on bean property and emit the value
			PropertyChangeListener listener = evt -> {
				ProgressionModel progression = (ProgressionModel) evt.getSource();
				JobProgressionVO jobProgression = JobProgressionVO.fromModelBuilder(progression)
					.id(jobId)
					.name(job.getName())
					.build();
				emitter.onNext(jobProgression);

				if (progression.isCompleted()) {
					// complete observable
					emitter.onComplete();
				}
			};

			// Add listener on current progression and message
			progressionModel.addPropertyChangeListener(ProgressionModel.Fields.CURRENT, listener);
			progressionModel.addPropertyChangeListener(ProgressionModel.Fields.MESSAGE, listener);
		});

		Disposable progressionSubscription = progressionObservable
			// throttle for 500ms to filter unnecessary flow
			.throttleLatest(500, TimeUnit.MILLISECONDS, true)
			// Publish job progression event
			.subscribe(jobProgressionVO -> publisher.publishEvent(new JobProgressionEvent(jobId, jobProgressionVO)));

		// Execute import
		try {
			SiopVesselImportResultVO result;

			try {
				result = self.importFromFile(context, progressionModel);

				// Set result status
				job.setStatus(result.hasError() ? JobStatusEnum.ERROR : JobStatusEnum.SUCCESS);

			} catch (Exception e) {
				// Result is kept in context
				result = context.getResult();
				result.setMessage(t("sumaris.import.vessel.error.detail", ExceptionUtils.getStackTrace(e)));

				// Set failed status
				// TODO
				//job.setStatus(JobStatusEnum.FAILED);
				job.setStatus(JobStatusEnum.ERROR);
			}

			try {
				// Serialize result in job report (as json)
				job.setReport(objectMapper.writeValueAsString(result));
			} catch (JsonProcessingException e) {
				throw new SumarisTechnicalException(e);
			}

			return new AsyncResult<>(result);

		} finally {

			// Publish job end event
			publisher.publishEvent(new JobEndEvent(jobId, job));
			Observables.dispose(progressionSubscription);
		}
	}

	/* -- protected methods -- */

	protected File prepareFile(File inputFile) throws IOException {
		char separator = detectSeparator(inputFile);

		return prepareFile(inputFile, separator);
	}

	protected File prepareFile(File inputFile, char separator) {

		try {
			File tempFile = Files.getNewTemporaryFile(inputFile);

			// Replace in headers (exact match
			Map<String, String> exactHeaderReplacements = Maps.newHashMap();
			for (String header: headerReplacements.keySet()) {
				// WARN: match start OR \ufeff (BOM UTF-8) character
				String regexp = "(^\ufeff?|" + separator + ")\"?" + header + "\"?(" + separator + "|$)";
				String replacement = "$1" + headerReplacements.get(header) + "$2";
				exactHeaderReplacements.put(regexp, replacement);
			}
			Files.replaceAllInHeader(inputFile, tempFile, exactHeaderReplacements);

			// Replace in lines
			//Files.replaceAll(tempFile, linesReplacements, 1, -1/*all rows*/);

			return tempFile;
		} catch(IOException e) {
			throw new SumarisTechnicalException("Could not preparing file: " + inputFile.getPath(), e);
		}
	}

	/**
	 * Read file's rows, as map
	 * @param reader
	 * @param includedHeaders
	 * @return
	 */
	public List<Map<String, String>> readRows(final CSVFileReader reader,
											  Set<String> includedHeaders) throws IOException {
		List<Map<String, String>> rows = Lists.newArrayList();
		// Read column headers :
		String[] headers = reader.getHeaders();

		// Read rows
		String[] cols;
		while ((cols = reader.readNext()) != null) {
			int colIndex = 0;

			Map<String, String> row = new HashMap<>();
			for (String cellValue : cols) {
				String headerName =  headers[colIndex++];
				if (includedHeaders.contains(headerName)) {
					row.put(headerName, cellValue);
				}
			}

			if (!row.isEmpty()) {
				rows.add(row);
			}
		}
		return rows;
	}

	protected char detectSeparator(File inputFile) throws IOException {
		try (CSVFileReader reader = new CSVFileReader(inputFile, true, true, Charsets.UTF_8.name())) {
			return reader.getSeparator();
		}
	}

	protected <T>  Map<T, Integer> collectExistingVessels(final String uniquePropertyName) {
		Page page = Page.builder()
			.offset(0)
			.size(100)
			.sortBy(VesselVO.Fields.ID)
			.build();
		boolean fetchMore;
		Map<T, Integer> result = Maps.newHashMap();
		do {
			List<VesselVO> vessels = vesselService.findAll(VesselFilterVO.builder()
				.programLabel(ProgramEnum.SIH.getLabel())
				.statusIds(Lists.newArrayList(StatusEnum.ENABLE.getId()))
				.build(), page, VesselFetchOptions.builder()
					.withVesselRegistrationPeriod(uniquePropertyName.startsWith(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD))
					.withVesselFeatures(uniquePropertyName.startsWith(VesselVO.Fields.VESSEL_FEATURES))
				.build());
			vessels.forEach(v -> {
				try {
					T uniqueKeyValue = Beans.<VesselVO, T>getProperty(v, uniquePropertyName);
					if (uniqueKeyValue != null) {
						result.put(uniqueKeyValue, v.getId());
					}
				}
				catch (NestedNullException ne) {
					// SKip
				}
			});
			fetchMore = vessels.size() >= page.getSize();
			page.setOffset(page.getOffset() + page.getSize());
		} while (fetchMore);

		return result;
	}

	protected VesselVO insert(VesselVO source) {
		Preconditions.checkNotNull(source.getVesselFeatures());
		Preconditions.checkNotNull(source.getVesselRegistrationPeriod());

		Preconditions.checkArgument(source.getId() == null);
		Preconditions.checkArgument(source.getVesselFeatures().getId() == null);
		Preconditions.checkArgument(source.getVesselRegistrationPeriod().getId() == null);

		return vesselService.save(source);
	}

	protected boolean update(VesselVO source, Date startDate) {
		Preconditions.checkNotNull(source.getId());
		Preconditions.checkNotNull(source.getVesselFeatures());
		Preconditions.checkNotNull(source.getVesselRegistrationPeriod());

		// Retrieve existing vessel
		VesselVO previousVessel = vesselService.get(source.getId());
		Preconditions.checkNotNull(previousVessel);
		Preconditions.checkArgument(source != previousVessel);

		// Check if changed
		boolean changes = false;
		if (source.getVesselFeatures() != null) {
			changes = changes
				|| previousVessel.getVesselFeatures() == null
				|| !equals(previousVessel.getVesselFeatures(), source.getVesselFeatures(), VesselFeaturesVO.Fields.BASE_PORT_LOCATION)
				|| !previousVessel.getVesselFeatures().getId().equals(source.getVesselFeatures().getBasePortLocation().getId());
		}
		if (source.getVesselRegistrationPeriod() != null) {
			changes = changes
				|| previousVessel.getVesselRegistrationPeriod() == null
				|| !equals(previousVessel.getVesselRegistrationPeriod(), source.getVesselRegistrationPeriod(), VesselRegistrationPeriodVO.Fields.REGISTRATION_LOCATION)
				|| !previousVessel.getVesselRegistrationPeriod().getId().equals(source.getVesselRegistrationPeriod().getRegistrationLocation().getId());
		}

		if (!changes) return false; // No changes

		Date previousEndDate = Dates.addSeconds(startDate, -1);

		// Update current data periods
		if (source.getVesselFeatures() != null) {
			// Reuse previous ids
			if (previousVessel.getVesselFeatures() != null && previousVessel.getVesselFeatures().getStartDate().equals(startDate)) {
				source.getVesselFeatures().setId(previousVessel.getVesselFeatures().getId());
			}
			else {
				source.getVesselFeatures().setId(null);
				// Close period of previous data
				previousVessel.getVesselFeatures().setEndDate(previousEndDate);
			}
			source.getVesselFeatures().setStartDate(startDate);
		}
		if (source.getVesselRegistrationPeriod() != null) {
			// Reuse previous ids
			if (previousVessel.getVesselRegistrationPeriod() != null && previousVessel.getVesselRegistrationPeriod().getStartDate().equals(startDate)) {
				source.getVesselRegistrationPeriod().setId(previousVessel.getVesselRegistrationPeriod().getId());
			}
			else {
				source.getVesselRegistrationPeriod().setId(null);
				// Close period of previous data
				previousVessel.getVesselRegistrationPeriod().setEndDate(previousEndDate);
			}
			source.getVesselRegistrationPeriod().setStartDate(startDate);
		}

		vesselService.save(Lists.newArrayList(previousVessel, source));

		return true;
	}

	protected void disable(int vesselId, Date startDate) {

		// Retrieve existing vessel
		VesselVO vessel = vesselService.get(vesselId);
		Preconditions.checkNotNull(vessel);

		// Close previous data periods
		Date previousEndDate = Dates.addSeconds(startDate, -1);
		if (vessel.getVesselFeatures() != null) {
			vessel.getVesselFeatures().setEndDate(previousEndDate);
		}
		if (vessel.getVesselRegistrationPeriod() != null) {
			vessel.getVesselRegistrationPeriod().setEndDate(previousEndDate);
		}

		vesselService.save(vessel);
	}

	protected boolean hasPropertySet(Object obj, String propertyName) {
		try {
			return Beans.getProperty(obj, propertyName) != null;
		}
		catch (SumarisTechnicalException e) {
			return false;
		}
	}

	protected void setProperty(@NonNull VesselVO target, @NonNull String propertyName, String value) throws ParseException {
		if (propertyName.endsWith(".1")
			|| propertyName.endsWith(".2")
			|| propertyName.endsWith(".3")
			|| propertyName.endsWith(".4")) {
			propertyName = propertyName.substring(0, propertyName.length() - 2);
			if (hasPropertySet(target, propertyName) || StringUtils.isBlank(value)) return; // Already set: ignore
		}

		if (StringUtils.isBlank(value)) {
			return; // Nothing to do
		}

		switch (propertyName) {
			case "vesselFeatures.startDate":
			case "vesselFeatures.endDate":
				Date date = Dates.parseDate(value, INPUT_DATE_PATTERNS);
				Beans.setProperty(target, propertyName, date);
				break;
			case "vesselFeatures.grossTonnageGrt":
			case "vesselFeatures.grossTonnageGt":
			case "vesselFeatures.lengthOverAll":
				Double dblValue = Double.parseDouble(value.replaceAll(",", "."));
				Beans.setProperty(target, propertyName, dblValue);
				break;
			case "vesselFeatures.administrativePower":
			case "vesselFeatures.constructionYear":
				Integer intValue = Integer.parseInt(value);
				Beans.setProperty(target, propertyName, intValue);
				break;

			case "vesselFeatures.basePortLocation":
			case "vesselRegistrationPeriod.registrationLocation":
				if (harbourReplacements.containsKey(value)) {
					LocationVO location = harbourReplacements.get(value);
					Beans.setProperty(target, propertyName, location);
				}
				else {
					String[] parts = value.split(LABEL_NAME_SEPARATOR_REGEXP);
					Object existingObject = Beans.getProperty(target, propertyName);
					LocationVO location = existingObject != null ? (LocationVO) existingObject : new LocationVO();
					if (parts.length == 1) {
						location.setName(parts[0]);
					} else if (parts.length == 2) {
						location.setLabel(parts[0]);
						location.setName(parts[1]);
					} else {
						throw new SumarisTechnicalException(String.format("Unknown format for a location: '%s'", value));
					}
					Beans.setProperty(target, propertyName, location);
				}
				break;
			default:
				Beans.setProperty(target, propertyName, value);
		}
	}

	protected VesselVO toVO(Map<String, String> source) {
		VesselVO target = new VesselVO();
		VesselFeaturesVO features = new VesselFeaturesVO();
		target.setVesselFeatures(features);

		VesselRegistrationPeriodVO registrationPeriod = new VesselRegistrationPeriodVO();
		target.setVesselRegistrationPeriod(registrationPeriod);

		// Vessel type
		ReferentialVO vesselType = new ReferentialVO();
		vesselType.setId(VesselTypeEnum.FISHING_VESSEL.getId());
		target.setVesselType(vesselType);

		// Program
		ProgramVO program = new ProgramVO();
		program.setId(ProgramEnum.SIH.getId());
		program.setLabel(ProgramEnum.SIH.getLabel());
		target.setProgram(program);

		// Fill properties, using the map key (= replaced headers) as propertyName
		source.forEach((propertyName, value) -> {
			try {
				setProperty(target, propertyName, value);
			} catch (ParseException e) {
				throw new IllegalArgumentException(String.format("Cannot set property '%s' with value '%s'", propertyName, value), e);
			}
		});

		return target;
	}

	protected void fillVessel(@NonNull VesselVO target, @NonNull PersonVO recorderPerson) {

		VesselRegistrationPeriodVO vrp = target.getVesselRegistrationPeriod();
		VesselFeaturesVO features = target.getVesselFeatures();

		// Fill status
		if (target.getStatusId() == null) {
			target.setStatusId(StatusEnum.ENABLE.getId());
		}

		// Fill recorder department
		if (target.getRecorderDepartment() == null) {
			target.setRecorderDepartment(recorderPerson.getDepartment());
		}

		// Fill recorder department
		if (target.getRecorderPerson() == null) {
			target.setRecorderPerson(recorderPerson);
		}

		// Fill Vessel features
		{
			// Fill base port location
			if (features.getBasePortLocation() != null) {
				fillOrCreateLocation(features.getBasePortLocation(), LocationLevelEnum.HARBOUR.getId());
			}

			// Fill start Date (if not set)
			if (features.getStartDate() == null) {
				features.setStartDate(DEFAULT_START_DATE);
			}

			// Exterior marking
			if (features.getExteriorMarking() == null) {
				if (StringUtils.isNotBlank(vrp.getRegistrationCode())
					&& !DEFAULT_REGISTRATION_COUNTRY_LABEL.equals(vrp.getRegistrationLocation().getLabel())) {
					String exteriorMarking = vrp.getRegistrationLocation().getLabel()
						+ vrp.getRegistrationCode();
					features.setExteriorMarking(exteriorMarking);
				} else {
					features.setExteriorMarking(vrp.getIntRegistrationCode());
				}
			}

			// Fill recorder department
			if (features.getRecorderDepartment() == null) {
				features.setRecorderDepartment(recorderPerson.getDepartment());
			}

			// Fill recorder department
			if (features.getRecorderPerson() == null) {
				features.setRecorderPerson(recorderPerson);
			}
		}

		// Fill registration period
		{
			// Fill registration start date
			if (vrp.getStartDate() == null) {
				vrp.setStartDate(features.getStartDate());
			}

			// Fill registration location
			if (vrp.getRegistrationLocation() != null) {
				boolean found;
				try {
					fillLocation(vrp.getRegistrationLocation(), LocationLevelEnum.MARITIME_DISTRICT.getId());
					found = vrp.getRegistrationLocation().getId() != null;
				} catch (DataNotFoundException e) {
					found = false;
				}
				// Use country, if cannot be found
				if (!found) {
					LocationVO registrationCountry = new LocationVO();
					registrationCountry.setLabel(DEFAULT_REGISTRATION_COUNTRY_LABEL);
					vrp.setRegistrationLocation(registrationCountry);
					fillOrCreateLocation(vrp.getRegistrationLocation(), LocationLevelEnum.COUNTRY.getId());
				}
			}
		}
	}

	protected void fillOrCreateLocation(@NonNull LocationVO target,
										int levelId) throws DataNotFoundException{
		boolean found;
		try {
			fillLocation(target, levelId);
			found = target.getId() != null;
		} catch (DataNotFoundException e) {
			found = false;
		}

		if (!found) {
			createLocation(target, levelId);
		}
	}

	protected void fillLocation(@NonNull LocationVO target,
								Integer... levelIds) throws DataNotFoundException{
		if (target.getId() != null) return; // OK

		List<LocationFilterVO> filters = Lists.newArrayList();
		// Exact match
		if (StringUtils.isNotBlank(target.getLabel()) && StringUtils.isNotBlank(target.getName())) {
			filters.add(LocationFilterVO.builder()
				.name(target.getName())
				.levelIds(levelIds)
				.statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
				.build());
		}
		// Match name
		if (StringUtils.isNotBlank(target.getName())) {
			filters.add(LocationFilterVO.builder()
				.name(target.getName())
				.levelIds(levelIds)
				.statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
				.build());
			filters.add(LocationFilterVO.builder()
				.searchText(target.getName())
				.levelIds(levelIds)
				.statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
				.build());
		}
		// Match label (enable only)
		if (StringUtils.isNotBlank(target.getLabel())) {
			filters.add(LocationFilterVO.builder()
				.label(target.getLabel())
				.levelIds(levelIds)
				.statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
				.build());
			filters.add(LocationFilterVO.builder()
				.searchText(target.getLabel())
				.levelIds(levelIds)
				.statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
				.build());
		}

		// Search, using filters in the given order
		LocationVO resolvedLocation = null;
		for (LocationFilterVO filter: filters) {
			resolvedLocation = findLocations(filter);
			// OK found: stop search
			if (resolvedLocation != null) break;
		}

		// Not found
		if (resolvedLocation == null) {
			throw new DataNotFoundException(String.format("Cannot resolve location {label: %s; name: %s} on levelIds: %s", target.getLabel(), target.getName(),
				Beans.getStream(levelIds).map(Object::toString).collect(Collectors.joining(",")))
			);
		}

		target.setId(resolvedLocation.getId());
		target.setLevelId(resolvedLocation.getLevelId());
	}

	protected LocationVO findLocations(LocationFilterVO filter) {
		LocationVO result;
		int cacheKey = filter.hashCode();
		if (locationByFilterCache.containsKey(cacheKey)) {
			result = locationByFilterCache.get(cacheKey);
		}
		else {
			List<LocationVO> matches = locationService.findByFilter(filter,
				Page.builder().offset(0).size(2).build(),
				ReferentialFetchOptions.builder().build());
			int matchCount = matches.size();

			if (matchCount > 1) {
				matches = matches.stream()
					.filter(item -> StatusEnum.ENABLE.getId().equals(item.getStatusId()))
					.collect(Collectors.toList());
				if (StringUtils.isNotBlank(filter.getLabel()) && StringUtils.isNotBlank(filter.getName())) {
					log.warn("More than one match for location {label: {}, name: {}} on level {}", filter.getLabel(), filter.getName(),
						Arrays.stream(filter.getLevelIds()).map(Object::toString).collect(Collectors.joining(",")));
				}
			}
			if (CollectionUtils.isEmpty(matches)) {
				result = UNRESOLVED_LOCATION;
			}
			else {
				result = matches.get(0);
			}

			// Add to cache
			locationByFilterCache.put(filter.hashCode(), result);
		}

		return (result != UNRESOLVED_LOCATION) ? result : null;
	}

	protected LocationVO createLocation(@NonNull LocationVO source,
										int levelId) {
		return createLocation(source, levelId, StatusEnum.ENABLE.getId());
	}

	protected LocationVO createLocation(@NonNull LocationVO source,
										int levelId,
										int statusId) {

		source.setLevelId(levelId);
		source.setStatusId(statusId);
		Preconditions.checkArgument(StringUtils.isNotBlank(source.getLabel())
			|| StringUtils.isNotBlank(source.getName()), "Missing label or name");

		// Fill required label
		if (StringUtils.isBlank(source.getLabel())) {
			source.setLabel("?");
			// Mark as temporary
			source.setStatusId(StatusEnum.TEMPORARY.getId());
		}

		// Fill required name
		if (StringUtils.isBlank(source.getName())) {
			source.setName("?");
			// Mark as temporary
			source.setStatusId(StatusEnum.TEMPORARY.getId());
		}

		log.info("Creating Location {label: '{}', name: '{}', levelId: {}, statusId: {}}",
			source.getLabel(), source.getName(), source.getLevelId(), source.getStatusId());
		ReferentialVO target = referentialService.save(source);
		source.setId(target.getId());

		// Invalidate cache
		locationByFilterCache.clear();

		return source;
	}

	protected List<LocationVO> findAllTemporaryLocation(int levelId) {
		LocationFilterVO filter = LocationFilterVO.builder()
			.label("?")
			.statusIds(new Integer[]{StatusEnum.TEMPORARY.getId()})
			.build();
		return locationService.findByFilter(filter,
			Page.builder().offset(0).size(1000).build(),
			ReferentialFetchOptions.builder().build());
	}

	protected Set<String> findAllTemporaryLocationNames(int levelId) {
		return findAllTemporaryLocation(levelId)
			.stream().map(LocationVO::getName)
			.collect(Collectors.toSet());
	}

	protected <T> boolean equals(T o1, T o2, String... excludePropertyNames) {
		if (ArrayUtils.isNotEmpty(excludePropertyNames)) {
			o1 = Beans.clone(o1, (Class<T>)o1.getClass(), excludePropertyNames);
			o2 = Beans.clone(o2, (Class<T>)o2.getClass(), excludePropertyNames);
		}
		return o1 == o2 || o1.equals(o2);
	}


}


