package net.sumaris.server.exception;

import org.apache.http.HttpStatus;

public interface ErrorCodes extends net.sumaris.core.exception.ErrorCodes {

    // >= 400
    int UNAUTHORIZED = HttpStatus.SC_UNAUTHORIZED; // 401
    int FORBIDDEN = HttpStatus.SC_FORBIDDEN;

    // >= 550
    int INVALID_EMAIL_CONFIRMATION = 550;
    int INVALID_QUERY_VARIABLES = 551;
    int ACCOUNT_ALREADY_EXISTS = 552;

}
