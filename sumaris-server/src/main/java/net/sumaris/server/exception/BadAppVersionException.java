package net.sumaris.server.exception;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.exception.SumarisTechnicalException;

/**
 * @author benoit.lavenier@e-is.pro
 */
public class BadAppVersionException extends SumarisTechnicalException {

    public static final int ERROR_CODE = ErrorCodes.BAD_APP_VERSION;

    /**
     * <p>Constructor for DataLockedException.</p>
     *
     * @param message a {@link String} object.
     */
    public BadAppVersionException(String message) {
        super(ERROR_CODE, message);
    }

    /**
     * <p>Constructor for DataLockedException.</p>
     *
     * @param message a {@link String} object.
     * @param cause a {@link Throwable} object.
     */
    public BadAppVersionException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }

    /**
     * <p>Constructor for DataLockedException.</p>
     *
     * @param cause a {@link Throwable} object.
     */
    public BadAppVersionException(Throwable cause) {
        super(ERROR_CODE, cause);
    }
}
