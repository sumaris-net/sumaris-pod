package net.sumaris.importation.core.service.activitycalendar;

/*-
 * #%L
 * SUMARiS:: Importation
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.data.activity.ActivityCalendarService;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarSaveOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ActivityCalendarImportContextVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ActivityCalendarImportResultVO;
import net.sumaris.importation.core.util.csv.CSVFileReader;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableShort;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.nuiton.i18n.I18n.t;

@Service("activityCalendarImportService")
@RequiredArgsConstructor
@Slf4j
public class ActivityCalendarImportServiceImpl implements ActivityCalendarImportService {
    protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
            // Siop vessel synonyms (for LPDB)
            .put("ANN.E[.]DE[.]R.F.RENCE", ActivityCalendarVO.Fields.YEAR)
            .put("ENQUETE_DIRECTE", ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION)
            .put("ENQUETE_ECONOMIQUE", ActivityCalendarVO.Fields.ECONOMIC_SURVEY)
            .put("IMMATRICULATION", VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            .build();

    protected final SumarisConfiguration config;

    protected final ActivityCalendarService activityCalendarService;

    protected final VesselSnapshotService VesselSnapshotService;

    protected final PersonService personService;

    protected final ApplicationContext applicationContext;
    private boolean running = false;


    @Override
    public ActivityCalendarImportResultVO importFromFile(ActivityCalendarImportContextVO context, @Nullable IProgressionModel progressionModel) throws IOException {
        Files.checkExists(context.getProcessingFile());
        Preconditions.checkNotNull(context.getRecorderPersonId());

        ActivityCalendarImportResultVO result = context.getResult();
        progressionModel = Optional.ofNullable(progressionModel).orElseGet(ProgressionModel::new);

        // Make sure this job run once, to avoid duplication
        if (running) {
            String message = t("sumaris.import.error.alreadyRunning");
            progressionModel.setTotal(1);
            progressionModel.setMessage(message);
            progressionModel.setCurrent(1);
            throw new ActivityCalendarImportAlreadyRunningException(message);
        }
        running = true;

        try {
            // Init progression model
            progressionModel.setTotal(1);
            progressionModel.setMessage(t("sumaris.import.start", context.getProcessingFile().getName()));
            PersonVO recorderPerson = personService.getById(context.getRecorderPersonId());
            File tempFile = null;

            try {
                tempFile = prepareFile(context.getProcessingFile());
                Set<String> includedHeaders = Sets.newHashSet(headerReplacements.values());
                Set<String> requiredHeaders = Sets.newHashSet(headerReplacements.values());
                //Remove because is not in csv actually
                requiredHeaders.remove(ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION);

                // Do load
                try (CSVFileReader reader = new CSVFileReader(tempFile, true, true, Charsets.UTF_8.name())) {

                    MutableShort inserts = new MutableShort(0);
                    MutableShort updates = new MutableShort(0);
                    MutableShort errors = new MutableShort(0);
                    MutableShort warnings = new MutableShort(0);
                    MutableShort rowCounter = new MutableShort(1);
                    List<String> messages = new ArrayList<>();
                    List<ActivityCalendarVO> processedActivityCalendar = Lists.newArrayList();

                    // Read file rows
                    List<Map<String, String>> rows = readRows(reader, includedHeaders);

                    // Check if headers is valid
                    if (!containsAllHeaders(reader.getHeaders(), requiredHeaders)) {
                        errors.increment();
                        String message = t("sumaris.import.activityCalendar.error.invalidHeaderRow");
                        messages.add(message);
                        log.warn(message);
                    }

                    progressionModel.setTotal(rows.size() + 3 /*read file + get existing + update comments step*/);
                    progressionModel.setCurrent(0);
                    progressionModel.setMessage(t("sumaris.import.activityCalendar.readingFile"));

                    // Convert csv calendar to vo
                    List<ActivityCalendarVO> sources = rows.stream()
                        .map(this::toVO)
                        .toList();

                    // Get all years in csv
                    Set<String> years = rows.stream()
                            .map(row -> row.get(ActivityCalendarVO.Fields.YEAR))
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toSet());
                    Set<String> processedKeys = Sets.newHashSet();

                    progressionModel.increments(t("sumaris.import.activityCalendar.loadingExistingData"));

                    // Load existing activity calendars
                    Map<String, ActivityCalendarVO> existingCalendarIdsByUniqueKey = findAllActivityCalendarByYears(years)
                        .stream()
                        .filter(this::hasValidVessel)
                        .distinct()
                        .collect(
                            Collectors.toMap(this::getUniqueKey, calendar -> calendar,
                                // Merge (should not occur, maybe on test data)
                                (a1, a2) -> a1)
                        );

                    progressionModel.increments(t("sumaris.import.activityCalendar.progress", 0, sources.size()));

                    for (ActivityCalendarVO source : sources) {

                        try {

                            //  Check if is valid calendar
                            if (!hasValidVessel(source)) {
                                warnings.increment();

                                String message = String.format(t("sumaris.import.activityCalendar.error.invalidRow", rowCounter, source.getQualificationComments(), source.getYear() ));
                                messages.add(message);
                                log.warn(message);
                            }
                            else {
                                // Fill unique key for the calendar (year + registration code)
                                String uniqueKey = getUniqueKey(source);

                                // Get the ID if it exists
                                ActivityCalendarVO existingCalendar = existingCalendarIdsByUniqueKey.get(uniqueKey);

                                // Fill recorder department and person
                                fillActivityCalendar(source, recorderPerson);

                                //  Check if it has already been executed
                                if (processedKeys.contains(uniqueKey)) {
                                    warnings.increment();
                                    String message = String.format(t("sumaris.import.activityCalendar.error.duplicateRow", rowCounter, source.getQualificationComments(), source.getYear()));
                                    messages.add(message);
                                    log.warn(message);
                                }
                                else {

                                    // Check if already exist in database
                                    if (existingCalendar != null) {
                                        source.setId(existingCalendar.getId());
                                        // Keep the existing qualification comments (as we use it to store the registration code)
                                        source.setQualificationComments(existingCalendar.getQualificationComments());
                                        source = update(source, existingCalendar);
                                        updates.increment();
                                    }
                                    // Not exists: create it
                                    else {
                                        // Clear qualification comments (as we use it to store the registration code)
                                        source.setQualificationComments(null);

                                        source = insert(source);
                                        inserts.increment();
                                    }
                                }

                                processedActivityCalendar.add(source);
                                processedKeys.add(uniqueKey);
                            }


                        } catch (Exception e) {
                            errors.increment();
                            String message = t("sumaris.import.error.row", rowCounter, e.getMessage());
                            log.error(message);
                            messages.add(message);
                        } finally {
                            rowCounter.increment();
                            if (rowCounter.intValue() % 10 == 0) {
                                progressionModel.setCurrent(rowCounter.intValue());
                                progressionModel.setMessage(t("sumaris.import.activityCalendar.progress", rowCounter, sources.size()));
                            }
                        }

                    }
                    progressionModel.setCurrent(progressionModel.getTotal() - 1);

                    // Update comments, using previous year comments (see issue sumaris-app#866)
                    {
                        ListMultimap<Integer, ActivityCalendarVO> activityCalendarsByYear = Beans.splitByNotUniqueProperty(
                            processedActivityCalendar,
                            ActivityCalendarVO.Fields.YEAR);
                        List<Integer> sortedYears = activityCalendarsByYear.keySet().stream().sorted().toList();

                        try {
                            for (Integer year : sortedYears) {
                                List<Integer> ids = activityCalendarsByYear.get(year)
                                    .stream().filter(ac -> StringUtils.isBlank(ac.getComments()))
                                    .map(ActivityCalendarVO::getId)
                                    .filter(Objects::nonNull)
                                    .toList();

                                // Copy previous year comments - see issue sumaris-app#866
                                activityCalendarService.copyPreviousYearCommentsByIds(ids);
                            }
                        } catch (Exception e) {
                            errors.increment();
                            String message = t("sumaris.import.activityCalendar.error.copyComments", e.getMessage());
                            log.error(message);
                            messages.add(message);
                            // Continue
                        }
                    }

                    // Update result
                    result.setInserts(inserts.intValue());
                    result.setUpdates(updates.intValue());
                    result.setWarnings(warnings.intValue());
                    result.setErrors(errors.intValue());

                    if (CollectionUtils.isNotEmpty(messages)) {
                        result.setMessage(String.join("\n", messages));
                    }

                    progressionModel.setCurrent(progressionModel.getTotal());
                    return result;
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new SumarisTechnicalException(e);

            } finally {
                Files.deleteQuietly(tempFile);
                Files.deleteTemporaryFiles(context.getProcessingFile());
                progressionModel.setCurrent(progressionModel.getTotal());
            }
        } finally {
            running = false;
        }
    }

    @Override
    public Future<ActivityCalendarImportResultVO> asyncImportFromFile(ActivityCalendarImportContextVO context, @Nullable IProgressionModel progressionModel) {

        ActivityCalendarImportResultVO result;
        try {
            result = applicationContext.getBean(ActivityCalendarImportService.class)
                    .importFromFile(context, progressionModel);

            // Set result status
            result.setStatus(result.hasError() ? JobStatusEnum.ERROR : JobStatusEnum.SUCCESS);

        } catch (Exception e) {
            // Result is kept in context
            result = context.getResult();
            result.setMessage(t("sumaris.import.error.detail", ExceptionUtils.getStackTrace(e)));

            // Set failed status
            result.setStatus(JobStatusEnum.FATAL);
        }

         return new AsyncResult<>(result);
    }

    protected File prepareFile(File inputFile) throws IOException {
        char separator = detectSeparator(inputFile);

        return prepareFile(inputFile, separator);
    }

    protected File prepareFile(File inputFile, char separator) {

        try {
            File tempFile = Files.getNewTemporaryFile(inputFile);

            // Replace in headers (exact match
            Map<String, String> exactHeaderReplacements = Maps.newHashMap();
            for (String header : headerReplacements.keySet()) {
                // WARN: match start OR \ufeff (BOM UTF-8) character
                String regexp = "(^\ufeff?|" + separator + ")\"?" + header + "\"?(" + separator + "|$)";
                String replacement = "$1" + headerReplacements.get(header) + "$2";
                exactHeaderReplacements.put(regexp, replacement);
            }

            Files.replaceAllInHeader(inputFile, tempFile, exactHeaderReplacements);

            return tempFile;
        } catch (IOException e) {
            throw new SumarisTechnicalException("Could not preparing file: " + inputFile.getPath(), e);
        }
    }

    protected char detectSeparator(File inputFile) throws IOException {
        try (CSVFileReader reader = new CSVFileReader(inputFile, true, true, Charsets.UTF_8.name())) {
            return reader.getSeparator();
        }
    }

    protected List<ActivityCalendarVO> findAllActivityCalendarByYears(Set<String> years) {
        List<ActivityCalendarVO> result = new ArrayList<>();

        for (String year : years) {
            Page page = Page.builder()
                    .offset(0)
                    .size(100)
                    .sortBy(ActivityCalendarVO.Fields.ID)
                    .sortDirection(SortDirection.ASC)
                    .build();
            boolean fetchMore;

            do {
                List<ActivityCalendarVO> activityCalendars = activityCalendarService.findAll(
                        ActivityCalendarFilterVO.builder()
                                .programLabel(ProgramEnum.SIH_ACTIFLOT.getLabel())
                                .year(Integer.parseInt(year))
                                .build(),
                        page, ActivityCalendarFetchOptions.builder().withVesselSnapshot(true).withChildrenEntities(false).build()
                );

                activityCalendars.forEach(calendar -> {
                    try {
                        result.addAll(activityCalendars);

                    } catch (NestedNullException ne) {
                        log.debug("Getting an unexpected error NestedNullException:", ne);
                        // Skip entries with nested null exceptions
                    }
                });

                fetchMore = activityCalendars.size() >= page.getSize();
                page.setOffset(page.getOffset() + page.getSize());
            } while (fetchMore);
        }
        return result;
    }

    /**
     * Read file's rows, as map
     *
     * @param reader CSV reader
     * @param includedHeaders headers to include
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
                String headerName = headers[colIndex++];
                if (includedHeaders.contains(headerName)) {
                    if (Objects.equals(cellValue, "")) {
                        cellValue = null;
                    }
                    row.put(headerName, cellValue);
                }
            }

            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return rows;
    }

    protected ActivityCalendarVO toVO(Map<String, String> source) {
        ActivityCalendarVO target = new ActivityCalendarVO();
        //Check if all mandatory fields are present
        if (source.get(ActivityCalendarVO.Fields.YEAR) == null
                || source.get(VesselSnapshotVO.Fields.REGISTRATION_CODE) == null) {
            return target;
        }

        //year
        int year = Integer.parseInt(source.get(ActivityCalendarVO.Fields.YEAR));
        target.setYear(year);

        //directSurveyInvestigation
        String directSurveyInvestigation = source.get(ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION);
        target.setDirectSurveyInvestigation(Integer.parseInt(directSurveyInvestigation));

        //economicSurvey
        String economicSurvey = source.get(ActivityCalendarVO.Fields.ECONOMIC_SURVEY);
        Integer economicSurveyValue = StringUtils.isNotBlank(economicSurvey) ? Integer.parseInt(economicSurvey) : null;
        target.setEconomicSurvey(economicSurveyValue != null && economicSurveyValue == 1);

        // Clear control/validation
        target.setControlDate(null);
        target.setValidationDate(null);

        // Qualification
        target.setQualityFlagId(QualityFlagEnum.NOT_QUALIFIED.getId());
        target.setQualificationComments(null);
        target.setQualificationDate(null);

        //program
        ProgramVO program = new ProgramVO();
        program.setId(ProgramEnum.SIH_ACTIFLOT.getId());
        program.setLabel(ProgramEnum.SIH_ACTIFLOT.getLabel());
        target.setProgram(program);

        //vesselId
        String registrationCode = source.get(VesselSnapshotVO.Fields.REGISTRATION_CODE);
        if (StringUtils.isNotBlank(registrationCode)) {
            VesselSnapshotVO vesselSnapshot = new VesselSnapshotVO();
            vesselSnapshot.setRegistrationCode(registrationCode);
            target.setVesselSnapshot(vesselSnapshot);
        }
//        List<VesselSnapshotVO> vesselSnapshots = findVesselByRegistrationCode(year, registrationCode);
//        if (CollectionUtils.isNotEmpty(vesselSnapshots)) {
//            target.setVesselSnapshot(vesselSnapshots.get(0));
//            target.setVesselId(target.getVesselSnapshot().getVesselId());
//        }

        //Use the qualificationComments for storing the registration code
        target.setQualificationComments(source.get(VesselSnapshotVO.Fields.REGISTRATION_CODE));

        return target;
    }

    protected void fillActivityCalendar(@NonNull ActivityCalendarVO target, @NonNull PersonVO person) {
        // Fill recorder department
        if (target.getRecorderDepartment() == null) {
            target.setRecorderDepartment(person.getDepartment());
        }
    }

    protected VesselSnapshotVO findVesselByRegistrationCode(int year, String registrationCode) {
        Date startDate = Dates.getFirstDayOfYear(year);
        Date endDate = Dates.getLastSecondOfYear(year);
        List<VesselSnapshotVO> vessels = VesselSnapshotService.findAll(
            VesselFilterVO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .searchText(registrationCode)
                .searchAttributes(new String[]{VesselSnapshotVO.Fields.REGISTRATION_CODE})
                .build(),
            Page.builder()
                .offset(0)
                .size(1)
                .sortBy(VesselSnapshotVO.Fields.START_DATE)
                .sortDirection(SortDirection.ASC)
                .build(),
            VesselFetchOptions.DEFAULT
        );
        if (CollectionUtils.isNotEmpty(vessels)) return vessels.get(0);

        throw new DataNotFoundException(String.format("No vessel found for registration code '%s' in year %s", registrationCode, year));
    }

    protected ActivityCalendarVO insert(ActivityCalendarVO source) {

        // Resolve vessel
        VesselSnapshotVO vesselSnapshot = findVesselByRegistrationCode(source.getYear(), source.getVesselSnapshot().getRegistrationCode());
        source.setVesselSnapshot(vesselSnapshot);
        source.setVesselId(vesselSnapshot.getVesselId());

        return activityCalendarService.save(source, ActivityCalendarSaveOptions.WITHOUT_CHILDREN);
    }

    protected ActivityCalendarVO update(ActivityCalendarVO source, ActivityCalendarVO target) {

        target.setDirectSurveyInvestigation(source.getDirectSurveyInvestigation());
        target.setEconomicSurvey(source.getEconomicSurvey());
        target.setControlDate(source.getControlDate());
        target.setValidationDate(source.getValidationDate());
        target.setQualityFlagId(source.getQualityFlagId());
        target.setQualificationComments(source.getQualificationComments());
        target.setQualificationDate(source.getQualificationDate());

        return activityCalendarService.save(target,
            ActivityCalendarSaveOptions.WITHOUT_CHILDREN // fix issue sumaris-app#916
        );
    }

    protected boolean containsAllHeaders(String[] actualHeaders, Collection<String> expectedHeaders) {
        return Sets.newHashSet(actualHeaders).containsAll(expectedHeaders);
    }

    protected boolean hasValidVessel(@NonNull ActivityCalendarVO activityCalendar) {
        return activityCalendar.getVesselSnapshot() != null && StringUtils.isNotBlank(activityCalendar.getVesselSnapshot().getRegistrationCode());
    }

    protected String getUniqueKey(@NonNull ActivityCalendarVO activityCalendar) {
        Preconditions.checkNotNull(activityCalendar.getVesselSnapshot());
        Preconditions.checkArgument(StringUtils.isNotBlank(activityCalendar.getVesselSnapshot().getRegistrationCode()), "Missing registration code");
        return getUniqueKey(activityCalendar.getYear(), activityCalendar.getVesselSnapshot().getRegistrationCode());
    }

    protected String getUniqueKey(int year, String registrationCode) {
        return String.format("%s-%s", year, registrationCode);
    }
}
