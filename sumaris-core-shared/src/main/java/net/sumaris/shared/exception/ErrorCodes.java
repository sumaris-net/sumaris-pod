package net.sumaris.shared.exception;

public interface ErrorCodes  {

    // >= 400
    int BAD_REQUEST = 400;
    int NOT_FOUND = 404;

    // >= 500
    int INTERNAL_ERROR = 500;

    // Application specific errors
    int DATA_LOCKED = 520;
    int BAD_UPDATE_DATE = 521;
    int DENY_DELETION = 522;

}
