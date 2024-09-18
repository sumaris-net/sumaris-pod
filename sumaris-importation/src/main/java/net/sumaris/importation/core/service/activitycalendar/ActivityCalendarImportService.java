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
