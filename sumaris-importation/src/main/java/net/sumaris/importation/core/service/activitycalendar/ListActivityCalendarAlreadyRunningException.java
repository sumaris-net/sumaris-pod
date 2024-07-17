package net.sumaris.importation.core.service.activitycalendar;

import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisBusinessException;

public class ListActivityCalendarAlreadyRunningException extends SumarisBusinessException {

    public ListActivityCalendarAlreadyRunningException(Throwable t) {
        super(ErrorCodes.LIST_ACTIVITY_CALENDAR_IMPORT_ALREADY_RUNNING, t);
    }

    public ListActivityCalendarAlreadyRunningException(String message) {
        super(ErrorCodes.LIST_ACTIVITY_CALENDAR_IMPORT_ALREADY_RUNNING, message);
    }
}
