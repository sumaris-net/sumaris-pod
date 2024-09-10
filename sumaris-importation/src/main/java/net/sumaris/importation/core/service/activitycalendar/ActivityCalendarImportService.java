package net.sumaris.importation.core.service.activitycalendar;

import net.sumaris.core.model.IProgressionModel;
import net.sumaris.importation.core.service.activitycalendar.vo.ActivityCalendarImportContextVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ActivityCalendarImportResultVO;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.concurrent.Future;

public interface ActivityCalendarImportService {


    /**
     * Import a list of activity calendars into the database
     *
     * @param context the importation context
     * @param progressionModel a progression model
     * @return
     * @throws IOException
     */
    @Transactional
    ActivityCalendarImportResultVO importFromFile(ActivityCalendarImportContextVO context, @Nullable IProgressionModel progressionModel) throws IOException;


    @Async("jobTaskExecutor")
    Future<ActivityCalendarImportResultVO> asyncImportFromFile(ActivityCalendarImportContextVO context,
                                                               @Nullable IProgressionModel progressionModel);
}
