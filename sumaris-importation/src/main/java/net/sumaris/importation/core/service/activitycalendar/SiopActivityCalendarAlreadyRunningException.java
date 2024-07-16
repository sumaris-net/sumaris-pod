package net.sumaris.importation.core.service.activitycalendar;

import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisBusinessException;

public class SiopActivityCalendarAlreadyRunningException extends SumarisBusinessException {

    public SiopActivityCalendarAlreadyRunningException(Throwable t) {
        super(ErrorCodes.SIOP_ACTIVITY_CALENDAR_IMPORT_ALREADY_RUNNING, t);
    }

    public SiopActivityCalendarAlreadyRunningException(String message) {
        super(ErrorCodes.SIOP_ACTIVITY_CALENDAR_IMPORT_ALREADY_RUNNING, message);
    }
}
