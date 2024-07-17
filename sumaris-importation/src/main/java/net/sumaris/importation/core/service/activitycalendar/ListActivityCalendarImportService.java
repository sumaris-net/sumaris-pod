package net.sumaris.importation.core.service.activitycalendar;

import net.sumaris.core.model.IProgressionModel;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityImportCalendarContextVO;
import net.sumaris.importation.core.service.activitycalendar.vo.ListActivityCalendarImportResultVO;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.concurrent.Future;

public interface ListActivityCalendarImportService {


    /**
     * Import a CL file (landing statistics) into the database
     *
     * @param recorderPersonId the recorder person to store VESSEL_xxx tables
     * @param inputFile        the input data file to import
     * @return
     * @throws IOException
     */
    @Transactional
    ListActivityCalendarImportResultVO importFromFile(ListActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) throws IOException;


    @Async("jobTaskExecutor")
    Future<ListActivityCalendarImportResultVO> asyncImportFromFile(ListActivityImportCalendarContextVO context,
                                                                   @Nullable IProgressionModel progressionModel);
}
