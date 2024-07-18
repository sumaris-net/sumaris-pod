package net.sumaris.importation.core.service.activitycalendar;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.data.vessel.VesselRegistrationPeriodRepositoryImpl;
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
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.VesselRegistrationPeriodVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.data.vessel.VesselOwnerVO;
import net.sumaris.core.vo.data.vessel.VesselRegistrationPeriodFetchOptions;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.core.vo.filter.VesselRegistrationFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityCalendarImportResultVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityImportCalendarContextVO;
import net.sumaris.importation.core.util.csv.CSVFileReader;
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
    protected final static String LABEL_NAME_SEPARATOR_REGEXP = "[ \t]+-[ \t]+";

    private final VesselRegistrationPeriodRepositoryImpl vesselRegistrationPeriodRepository;

    protected static final String[] INPUT_DATE_PATTERNS = new String[]{
            "dd/MM/yyyy"
    };

    protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
            // Siop vessel synonyms (for LPDB)
            .put("ANN.E[.]DE[.]R.F.RENCE", StringUtils.doting(ActivityCalendarVO.Fields.YEAR))
            .put("ENQUETE_DIRECTE", StringUtils.doting(ActivityCalendarVO.Fields.DIRECT_SURVEY_INVESTIGATION))
            .put("ENQUETE_ECONOMIQUE", StringUtils.doting(ActivityCalendarVO.Fields.ECONOMIC_SURVEY))
            .put("IMMATRICULATION", StringUtils.doting(VesselRegistrationPeriod.Fields.REGISTRATION_CODE))
            .build();


    protected Map<Integer, LocationVO> locationByFilterCache = Maps.newConcurrentMap();


    protected final static LocationVO UNRESOLVED_LOCATION = new LocationVO();

    protected static final Date DEFAULT_START_DATE = Dates.safeParseDate("01/01/1990", INPUT_DATE_PATTERNS);
    protected static final String DEFAULT_REGISTRATION_COUNTRY_LABEL = "FRA";


    protected final SumarisConfiguration config;

    protected final ReferentialService referentialService;

    protected final LocationService locationService;

    protected final ActivityCalendarService activityCalendarService;

    protected final VesselRegistrationPeriodRepositoryImpl vesselRegistrationPeriodService;

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

            String uniqueKeyPropertyName = StringUtils.doting(
                    ActivityCalendarVO.Fields.VESSEL_ID, VesselOwnerVO.Fields.REGISTRATION_CODE
            );

            Date startDate = Dates.resetTime(new Date());
            File tempFile = null;

            try {
                tempFile = prepareFile(context.getProcessingFile());

                Set<String> includedHeaders = new HashSet<>(headerReplacements.values());

                // Do load
                try (CSVFileReader reader = new CSVFileReader(tempFile, true, true, Charsets.UTF_8.name())) {

                    Map<String, Integer> existingActivityCalendars = collectExistingActivityCalendars();
//                    Set<String> processedKeys = Sets.newHashSet();

                    MutableShort inserts = new MutableShort(0);
                    MutableShort updates = new MutableShort(0);
                    MutableShort disables = new MutableShort(0);
                    MutableShort errors = new MutableShort(0);
                    MutableShort warnings = new MutableShort(0);
                    MutableShort rowCounter = new MutableShort(1);
                    List<String> messages = new ArrayList<>();

                    List<Map<String, String>> rows = readRows(reader, includedHeaders);

                    Set<String> yearsSet = rows.stream()
                            .map(row -> row.get(ActivityCalendarVO.Fields.YEAR))
                            .collect(Collectors.toSet());

                    Map<Integer, String> vesselIdByYearRegistrationCode = getVesselIdByYearRegistrationCode(yearsSet);


                    List<ActivityCalendarVO> activityCalendarsr = rows.stream()
                            .map(r -> toVO(r, vesselIdByYearRegistrationCode))
                            .toList();


                    System.out.println("activityCalendar: " + activityCalendarsr);

                    for (ActivityCalendarVO activityCalendar : activityCalendarsr) {


                    }

                }


            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new SumarisTechnicalException(e);
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SumarisTechnicalException(e);
        }

        return null;
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

            // Replace in lines
            //Files.replaceAll(tempFile, linesReplacements, 1, -1/*all rows*/);

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

    protected <T> Map<T, Integer> collectExistingActivityCalendars() {
        Page page = Page.builder()
                .offset(0)
                .size(100)
                .sortBy(ActivityCalendarVO.Fields.ID)
                .build();
        boolean fetchMore;
        Map<T, Integer> result = Maps.newHashMap();
        do {
            List<ActivityCalendarVO> activityCalendars = activityCalendarService.findAll(
                    ActivityCalendarFilterVO.builder()
                            .build(),
                    page, ActivityCalendarFetchOptions.builder()
                            .build()
            );

            fetchMore = activityCalendars.size() >= page.getSize();
            page.setOffset(page.getOffset() + page.getSize());
        } while (fetchMore);

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
                    row.put(headerName, cellValue);
                }
            }

            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return rows;
    }

    protected ActivityCalendarVO toVO(Map<String, String> source, Map<Integer, String> vesselIdByYearRegistrationCode) {
        ActivityCalendarVO target = new ActivityCalendarVO();

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

        // Program
        ProgramVO program = new ProgramVO();
        program.setId(ProgramEnum.SIH_ACTIFLOT.getId());
        program.setLabel(ProgramEnum.SIH_ACTIFLOT.getLabel());
        target.setProgram(program);

        //veselId
        Integer vesselId = getKeyByValue(vesselIdByYearRegistrationCode, source.get(VesselRegistrationPeriod.Fields.REGISTRATION_CODE));
        target.setVesselId(vesselId);


        return target;
    }

    protected Map<Integer, String> getVesselIdByYearRegistrationCode(Set<String> years) {
//        Set<Integer> yearsIntSet = years.stream()
//                .map(Integer::parseInt)
//                .collect(Collectors.toSet());

        Map<Integer, String> vesselIAndYearRegistrationCode = new HashMap<>();

        Page page = Page.builder()
                .offset(0)
                .size(100)
                .sortBy(ActivityCalendarVO.Fields.ID)
                .sortDirection(SortDirection.DESC)
                .build();
        List<VesselRegistrationPeriodVO> vesselRegistrationPeriods = vesselRegistrationPeriodService.findAll(
                VesselRegistrationFilterVO.builder()
                        .build(),
                page, VesselRegistrationPeriodFetchOptions.builder()
                        .withVesselFeatures(true)
                        .build()
        );
        // Étape 4 : Traiter les résultats


        vesselRegistrationPeriods.forEach(vesselRegistrationPeriodVO -> {
            vesselIAndYearRegistrationCode.put(vesselRegistrationPeriodVO.getVessel().getId(), vesselRegistrationPeriodVO.getRegistrationCode());
        });

        return vesselIAndYearRegistrationCode;

    }

    protected Integer getKeyByValue(Map<Integer, String> map, String valueToFind) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (valueToFind.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
