package net.sumaris.importation.core.service.activitycalendar;

import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisBusinessException;

public class ActivityCalendarImportAlreadyRunningException extends SumarisBusinessException {

    public ActivityCalendarImportAlreadyRunningException(Throwable t) {
        super(ErrorCodes.ACTIVITY_CALENDAR_IMPORTATION_ALREADY_RUNNING, t);
    }

    public ActivityCalendarImportAlreadyRunningException(String message) {
        super(ErrorCodes.ACTIVITY_CALENDAR_IMPORTATION_ALREADY_RUNNING, message);
    }
}
