package net.sumaris.importation.core.service.activitycalendar;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.ProgressionModel;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.data.activity.ActivityCalendarService;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityCalendarImportResultVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityImportCalendarContextVO;
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

@Service("listActivityCalendarLoaderService")
@RequiredArgsConstructor
@Slf4j
public class ListActivityCalendarImportServiceImpl implements ListActivityCalendarImportService {
    protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
            // Siop vessel synonyms (for LPDB)
            .put("ANN.E[.]DE[.]R.F.RENCE", StringUtils.doting(ActivityCalendarVO.Fields.YEAR))
            .put("ENQUETE_DIRECTE", StringUtils.doting(ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION))
            .put("ENQUETE_ECONOMIQUE", StringUtils.doting(ActivityCalendarVO.Fields.ECONOMIC_SURVEY))
            .put("IMMATRICULATION", StringUtils.doting(VesselRegistrationPeriod.Fields.REGISTRATION_CODE))
            .build();

    protected final SumarisConfiguration config;

    protected final ReferentialService referentialService;

    protected final LocationService locationService;

    protected final ActivityCalendarService activityCalendarService;

    protected final VesselSnapshotService VesselSnapshotService;

    protected final PersonService personService;

    protected final ApplicationContext applicationContext;
    private boolean running = false;


    @Override
    public ListActivityCalendarImportResultVO importFromFile(ListActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) throws IOException {
        Files.checkExists(context.getProcessingFile());
        Preconditions.checkNotNull(context.getRecorderPersonId());

        ListActivityCalendarImportResultVO result = context.getResult();
        progressionModel = Optional.ofNullable(progressionModel).orElseGet(ProgressionModel::new);

        // Make sure this job run once, to avoid duplication
        if (running) {
            String message = t("sumaris.import.job.error.alreadyRunning");
            progressionModel.setMessage(message);
            progressionModel.setTotal(1);
            progressionModel.setCurrent(1);
            throw new ListActivityCalendarAlreadyRunningException(message);
        }
        running = true;

        try {
            // Init progression model
            progressionModel.setMessage(t("sumaris.import.job.start", context.getProcessingFile().getName()));
            PersonVO recorderPerson = personService.getById(context.getRecorderPersonId());
            File tempFile = null;

            try {
                tempFile = prepareFile(context.getProcessingFile());
                Set<String> includedHeaders = new HashSet<>(headerReplacements.values());
                // Do load
                try (CSVFileReader reader = new CSVFileReader(tempFile, true, true, Charsets.UTF_8.name())) {
                    List<Map<String, String>> rows = readRows(reader, includedHeaders);

                    //Get all year in csv
                    Set<String> yearsSet = rows.stream()
                            .map(row -> row.get(ActivityCalendarVO.Fields.YEAR))
                            .collect(Collectors.toSet());
                    Set<String> processedKeys = Sets.newHashSet();

                    //if a row is empty
                    yearsSet.remove(null);

                    List<ActivityCalendarVO> existingActivityCalendarsVO = collectExistingActivityCalendars(yearsSet);

                    Map<String, Integer> existingActivityCalendars = new HashMap<>();
                    existingActivityCalendarsVO.forEach(calendar -> {
                        try {
                            existingActivityCalendars.put(calendar.getYear().toString() + '.' + calendar.getVesselSnapshot().getRegistrationCode(), calendar.getId());

                        } catch (NestedNullException ne) {
                            // Skip entries with nested null exceptions
                        }
                    });

                    MutableShort inserts = new MutableShort(0);
                    MutableShort updates = new MutableShort(0);
                    MutableShort errors = new MutableShort(0);
                    MutableShort warnings = new MutableShort(0);
                    MutableShort rowCounter = new MutableShort(1);
                    List<String> messages = new ArrayList<>();

                    //Remove beacause is not in csv actualy
                    includedHeaders.remove(StringUtils.doting(ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION));
                    //Check if headers is valid
                    String[] requiredHeaders = includedHeaders.toArray(new String[0]);
                    if (!allHeaderArePresent(requiredHeaders, reader.getHeaders())) {
                        errors.increment();
                        String message = String.format("Invalid header found !");
                        messages.add(message);
                    }

                    // Get all VesselSnapshots
                    List<VesselSnapshotVO> vesselSnapshots = collectingVesselSnapshot();

                    // Convert csv calendar to vo
                    List<ActivityCalendarVO> activityCalendars = rows.stream()
                            .map(activityCalendar -> toVO(activityCalendar, vesselSnapshots))
                            .toList();

                    progressionModel.setTotal(activityCalendars.size() + 1 /*disable vessels*/);

                    for (ActivityCalendarVO activityCalendar : activityCalendars) {

                        try {
                            String uniqueKey = null;
                            // Get vessel snapshot by id
                            List<VesselSnapshotVO> vesselSnapshotFilteredById = vesselSnapshots.stream()
                                    .filter(vessel -> vessel.getId().equals(activityCalendar.getVesselId()))
                                    .toList();

                            // Fill unique key for the calendar (year + registration code)
                            if (!vesselSnapshotFilteredById.isEmpty()) {
                                uniqueKey = activityCalendar.getYear().toString() + '.' + vesselSnapshots.stream().filter(vessel -> vessel.getId().equals(activityCalendar.getVesselId())).findFirst().get().getRegistrationCode();
                            }

                            // Get the ID if it exists
                            Integer existingId = existingActivityCalendars.get(uniqueKey);

                            // Fill recorder department and person
                            fillActivityCalendar(activityCalendar, recorderPerson);

                            //  Check if is valid calendar
                            if (activityCalendar.getVesselId() == null) {
                                warnings.increment();

                                String message = String.format(t("sumaris.import.activity_calendar.list.error.invalid_row", rowCounter, activityCalendar.getQualificationComments(), activityCalendar.getYear() ));
                                messages.add(message);
                                log.warn(message);
                            }
                            //  Check if it has already been executed
                            else if (processedKeys.contains(uniqueKey)) {
                                warnings.increment();
                                String message = String.format(t("sumaris.import.activity_calendar.list.error.duplicate_row", rowCounter, activityCalendar.getQualificationComments(), activityCalendar.getYear()));
                                messages.add(message);
                                log.warn(message);
                            }
                            // Check if already exist in database
                            else if (existingId != null) {
                                //clean QualificationComments
                                activityCalendar.setQualificationComments(null);

                                activityCalendar.setId(existingId);
                                boolean updated = update(activityCalendar, existingActivityCalendarsVO.stream().filter(calendar -> calendar.getId().equals(existingId)).findFirst().get());
                                if (updated) updates.increment();
                            }
                            // Check if the vessel is in the BAD period
                            else if (!checkVesselPeriod(vesselSnapshotFilteredById, activityCalendar.getYear())) {


                                warnings.increment();
                                String message = String.format(t("sumaris.import.activity_calendar.list.error.invalid_period", rowCounter, activityCalendar.getQualificationComments(), activityCalendar.getYear()));
                                //clean QualificationComments
                                activityCalendar.setQualificationComments(null);
                                insert(activityCalendar);
                                messages.add(message);
                                log.warn(message);
                            } else {
                                //clean QualificationComments
                                activityCalendar.setQualificationComments(null);

                                insert(activityCalendar);
                                inserts.increment();
                            }
                            processedKeys.add(uniqueKey);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            errors.increment();
                            messages.add(t("Row #%s: sumaris.import.job.error.row #%s", rowCounter.getValue(), e.getMessage()));
                        } finally {
                            rowCounter.increment();
                        }

                    }

                    // Update result
                    result.setInserts(inserts.intValue());
                    result.setUpdates(updates.intValue());
                    result.setWarnings(warnings.intValue());
                    result.setErrors(errors.intValue());
                    result.setTotal(rowCounter.intValue() - 1);

                    if (CollectionUtils.isNotEmpty(messages)) {
                        result.setMessage(String.join("\n", messages));
                    }


                    progressionModel.setCurrent(progressionModel.getTotal());
                    progressionModel.setMessage(t("sumaris.import.job.activity.calendar.progress", rowCounter.intValue(), activityCalendars.size()));
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
    public Future<ListActivityCalendarImportResultVO> asyncImportFromFile(ListActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) {

        ListActivityCalendarImportResultVO result;
        try {
            result = applicationContext.getBean(ListActivityCalendarImportService.class)
                    .importFromFile(context, progressionModel);

            // Set result status
            result.setStatus(result.hasError() ? JobStatusEnum.ERROR : JobStatusEnum.SUCCESS);

        } catch (Exception e) {
            // Result is kept in context
            result = context.getResult();
            result.setMessage(t("sumaris.job.error.detail", ExceptionUtils.getStackTrace(e)));

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

    protected List<ActivityCalendarVO> collectExistingActivityCalendars(Set<String> years) {
        List<ActivityCalendarVO> result = new ArrayList<>();

        for (String year : years) {
            Page page = Page.builder()
                    .offset(0)
                    .size(100)
                    .sortBy(ActivityCalendarVO.Fields.ID)
                    .build();
            boolean fetchMore;

            do {
                List<ActivityCalendarVO> activityCalendars = activityCalendarService.findAll(
                        ActivityCalendarFilterVO.builder()
                                .programLabel(ProgramEnum.SIH_ACTIFLOT.getLabel())
                                .year(Integer.parseInt(year))
                                .build(),
                        page, ActivityCalendarFetchOptions.builder().withVesselSnapshot(true).build()
                );

                activityCalendars.forEach(calendar -> {
                    try {
                        result.addAll(activityCalendars);

                    } catch (NestedNullException ne) {
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

    protected ActivityCalendarVO toVO(Map<String, String> source, List<VesselSnapshotVO> vessels) {
        ActivityCalendarVO target = new ActivityCalendarVO();
        //Check if all mandatory fields are present
        if (source.get(ActivityCalendarVO.Fields.YEAR) == null
                || source.get(VesselSnapshotVO.Fields.REGISTRATION_CODE) == null) {
            return target;
        }

        //year
        String year = source.get(ActivityCalendarVO.Fields.YEAR);
        target.setYear(Integer.parseInt(year));

        //directSurveyInvestigation
        String directSurveyInvestigation = source.get(ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION);
        target.setDirectSurveyInvestigation(Boolean.parseBoolean(directSurveyInvestigation));

        //economicSurvey
        String economicSurvey = source.get(ActivityCalendarVO.Fields.ECONOMIC_SURVEY);
        target.setEconomicSurvey(Boolean.parseBoolean(economicSurvey));

        //updateDate
        target.setUpdateDate(new Date());

        //creationDate
        target.setCreationDate(new Date());

        //qualityFlagId
        target.setQualityFlagId(StatusEnum.ENABLE.getId());

        //program
        ProgramVO program = new ProgramVO();
        program.setId(ProgramEnum.SIH_ACTIFLOT.getId());
        program.setLabel(ProgramEnum.SIH_ACTIFLOT.getLabel());
        target.setProgram(program);

        //vesselId
        List<VesselSnapshotVO> vesselSnapshots = vesselFilterByRegistrationCode(vessels, source.get(VesselSnapshotVO.Fields.REGISTRATION_CODE));
        if (vesselSnapshots != null && !vesselSnapshots.isEmpty()) {
            target.setVesselId(vesselSnapshots.get(0).getId());
        }

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


    protected List<VesselSnapshotVO> collectingVesselSnapshot() {
        Page page = Page.builder()
                .offset(0)
                .size(100)
                .sortBy(ActivityCalendarVO.Fields.ID)
                .sortDirection(SortDirection.DESC)
                .build();
        return VesselSnapshotService.findAll(VesselFilterVO.builder()
                .build(), page, VesselFetchOptions.DEFAULT);
    }

    protected ActivityCalendarVO insert(ActivityCalendarVO source) {
        return activityCalendarService.save(source);
    }

    protected boolean update(ActivityCalendarVO source, ActivityCalendarVO origin) {

        origin.setYear(source.getYear());
        origin.setDirectSurveyInvestigation(source.getDirectSurveyInvestigation());
        origin.setEconomicSurvey(source.getEconomicSurvey());
        origin.setQualityFlagId(StatusEnum.ENABLE.getId());
        origin.setProgram(source.getProgram());

        //vesselId
        if (source.getVesselId() != null) {
            origin.setVesselId(source.getVesselId());
        }

        activityCalendarService.save(origin);
        return true;
    }

    protected List<VesselSnapshotVO> vesselFilterByRegistrationCode(List<VesselSnapshotVO> vessels, String registrationCode) {
        List<VesselSnapshotVO> filteredVessels = vessels.stream()
                .filter(vessel -> registrationCode.equals(vessel.getRegistrationCode()))
                .collect(Collectors.toList());

        return filteredVessels.isEmpty() ? null : filteredVessels;
    }

    protected boolean checkVesselPeriod(List<VesselSnapshotVO> vessels, Integer year) {

        Integer endYear = null;
        for (VesselSnapshotVO vessel : vessels) {
            Calendar calendar = Calendar.getInstance();
            Date startDate = vessel.getStartDate();
            Date endDate = vessel.getEndDate();
            calendar.setTime(startDate);
            Integer startYear = calendar.get(Calendar.YEAR);

            if (vessel.getEndDate() != null) {
                calendar.setTime(endDate);
                endYear = calendar.get(Calendar.YEAR);
            }

            if (year >= startYear && (endYear == null || year <= endYear)) {
                return true;
            }
        }
        return false;
    }

    protected boolean allHeaderArePresent(String[] array1, String[] array2) {
        Set<String> set2 = new HashSet<>();
        for (String s : array2) {
            // ECONOMIC_SURVEY is not mandatory
            set2.add(s);
        }
        for (String s : array1) {
            if (!set2.contains(s)) {
                return false;
            }
        }
        return true;
    }
}
