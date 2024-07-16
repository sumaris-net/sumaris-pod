package net.sumaris.importation.core.service.activitycalendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.importation.core.service.activitycalendar.SiopActivityCalendarImportService;
import net.sumaris.importation.core.service.activitycalendar.vo.SiopActivityCalendarImportResultVO;
import net.sumaris.importation.core.service.activitycalendar.vo.SiopActivityImportCalendarContextVO;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.Future;

@Service("siopActivityCalendarLoaderService")
@RequiredArgsConstructor
@Slf4j
public class SiopActivityCalendarImportServiceImpl implements SiopActivityCalendarImportService {
    protected final static String LABEL_NAME_SEPARATOR_REGEXP = "[ \t]+-[ \t]+";
    protected static final String[] INPUT_DATE_PATTERNS = new String[]{
            "dd/MM/yyyy"
    };

    @Override
    public SiopActivityCalendarImportResultVO importFromFile(SiopActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) throws IOException {
        return null;
    }

    @Override
    public Future<SiopActivityCalendarImportResultVO> asyncImportFromFile(SiopActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) {
        return null;
    }
}
