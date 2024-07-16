package net.sumaris.importation.core.service.activitycalendar;

import net.sumaris.core.model.IProgressionModel;
import net.sumaris.importation.core.service.activitycalendar.vo.SiopActivityImportCalendarContextVO;
import net.sumaris.importation.core.service.activitycalendar.vo.SiopActivityCalendarImportResultVO;
import net.sumaris.importation.core.service.vessel.vo.SiopVesselImportContextVO;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.concurrent.Future;

public interface SiopActivityCalendarImportService {


    /**
     * Import a CL file (landing statistics) into the database
     *
     * @param recorderPersonId the recorder person to store VESSEL_xxx tables
     * @param inputFile        the input data file to import
     * @return
     * @throws IOException
     */
    @Transactional
    SiopActivityCalendarImportResultVO importFromFile(SiopActivityImportCalendarContextVO context, @Nullable IProgressionModel progressionModel) throws IOException;


    @Async("jobTaskExecutor")
    Future<SiopActivityCalendarImportResultVO> asyncImportFromFile(SiopActivityImportCalendarContextVO context,
                                                                   @Nullable IProgressionModel progressionModel);
}
