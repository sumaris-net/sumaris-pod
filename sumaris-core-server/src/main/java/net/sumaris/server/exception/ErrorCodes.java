package net.sumaris.server.exception;

public interface ErrorCodes extends net.sumaris.core.exception.ErrorCodes {

    int INVALID_EMAIL_CONFIRMATION = 1001;
    int INVALID_QUERY_VARIABLES = 1002;
    int INVALID_EMAIL = 1003;

    int SERVER_INTERNAL_ERROR = 1004;

    int ACCOUNT_ALREADY_EXISTS = 1005;

}
