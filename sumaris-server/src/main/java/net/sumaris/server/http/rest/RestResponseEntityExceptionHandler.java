/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.http.rest;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.BadUpdateDateException;
import net.sumaris.core.exception.DataLockedException;
import net.sumaris.core.exception.DenyDeletionException;
import net.sumaris.server.exception.BadAppVersionException;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.exception.ErrorHelper;
import net.sumaris.server.exception.InvalidEmailConfirmationException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@ControllerAdvice
@Slf4j
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    public RestResponseEntityExceptionHandler() {
        super();
    }

    /**
     * Transform an exception on bad update_dt, into a HTTP error 500 + a body response with the exact error code.
     */
    @ExceptionHandler(value = {InvalidEmailConfirmationException.class })
    protected ResponseEntity<Object> handleInvalidEmailException(RuntimeException ex, WebRequest request) {
        String message = ErrorHelper.toJsonErrorString(ErrorCodes.INVALID_EMAIL_CONFIRMATION, ex.getMessage());
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, message, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Transform an exception on bad update_dt, into a HTTP error 500 + a body response with the exact error code.
     */
    @ExceptionHandler(value = {BadUpdateDateException.class })
    protected ResponseEntity<Object> handleBadUpdateDt(RuntimeException ex, WebRequest request) {
        String message = ErrorHelper.toJsonErrorString(ErrorCodes.BAD_UPDATE_DATE, ex.getMessage());
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        return handleExceptionInternal(ex, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Transform an exception on locked data, into a HTTP error 500 + a body response with the exact error code.
     */
    @ExceptionHandler(value = {DataLockedException.class })
    protected ResponseEntity<Object> handleLock(RuntimeException ex, WebRequest request) {
        String message = ErrorHelper.toJsonErrorString(ErrorCodes.DATA_LOCKED, ex.getMessage());
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        return handleExceptionInternal(ex, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Transform an exception on bad update_dt, into a HTTP error 500 + a body response with the exact error code.
     */
    @ExceptionHandler(value = {BadAppVersionException.class })
    protected ResponseEntity<Object> handleBadAppVersionException(RuntimeException ex, WebRequest request) {
        String message = ErrorHelper.toJsonErrorString(ErrorCodes.BAD_APP_VERSION, ex.getMessage());
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        return handleExceptionInternal(ex, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Transform an exception on delete data, into a HTTP error 500 + a body response with the exact error code.
     */
    @ExceptionHandler(value = {DenyDeletionException.class })
    protected ResponseEntity<Object> handleDenyDeletionException(RuntimeException ex, WebRequest request) {
        String message = ErrorHelper.toJsonErrorString(ErrorCodes.DENY_DELETION, ex.getMessage());
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        HttpHeaders httpHeaders = new HttpHeaders();

        // Add entities in a specific header
        List<String> entitiesIds = ((DenyDeletionException)ex).getObjectIds();
        if (CollectionUtils.isNotEmpty(entitiesIds)) {
            httpHeaders.add(
                    net.sumaris.server.http.HttpHeaders.ACCESS_CONTROL_DENY_DELETION_ENTITIES,
                    Joiner.on(", ").join(entitiesIds));
        }
        return handleExceptionInternal(ex, message, httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


}
