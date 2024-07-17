package net.sumaris.importation.core.service.activitycalendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityCalendarImportResultVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityImportCalendarContextVO;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.Future;

@Service("listActivityCalendarLoaderService")
@RequiredArgsConstructor
@Slf4j
public class ListActivityCalendarImportServiceImpl implements ListActivityCalendarImportService {
    protected final static String LABEL_NAME_SEPARATOR_REGEXP = "[ \t]+-[ \t]+";
    protected static final String[] INPUT_DATE_PATTERNS = new String[]{
            "dd/MM/yyyy"
    };

    @Override
    public ListActivityCalendarImportResultVO importFromFile(ListActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) throws IOException {
        return null;
    }

    @Override
    public Future<ListActivityCalendarImportResultVO> asyncImportFromFile(ListActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) {
        // TDO MFA : MOCK

        ListActivityCalendarImportResultVO result = new ListActivityCalendarImportResultVO();
        result.setStatus(JobStatusEnum.SUCCESS);
        result.setMessage("Import successful");
        result.setInserts(10);
        result.setUpdates(5);
        result.setDisables(2);
        result.setWarnings(1);
        result.setErrors(0);


//                = SiopActivityCalendarImportResultVO.builder()
//                .inserts(10)
//                .updates(5)
//                .disables(2)
//                .warnings(1)
//                .errors(0)
//                .status(JobStatusEnum.SUCCESS)
//                .message("Import successful")
//                .build();

        return new AsyncResult<>(result);
    }
}
