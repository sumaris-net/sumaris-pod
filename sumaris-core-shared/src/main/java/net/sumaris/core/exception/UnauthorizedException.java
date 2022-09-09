package net.sumaris.core.exception;

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


import net.sumaris.shared.exception.ErrorCodes;
import org.nuiton.i18n.I18n;

import java.util.List;

/**
 * Throw when a deletion is not allowed
 */
public class UnauthorizedException extends SumarisTechnicalException {

    public static final int ERROR_CODE = ErrorCodes.UNAUTHORIZED;

    public UnauthorizedException(){
        super(ERROR_CODE, I18n.t("sumaris.error.account.unauthorized"));
    }

    /**
     * <p>Constructor for UnauthorizedException.</p>
     *
     * @param message a {@link String} object.
     */
    public UnauthorizedException(String message) {
        super(ERROR_CODE, message);
    }

    /**
     * <p>Constructor for DeleteForbiddenException.</p>
     *
     * @param message a {@link String} object.
     * @param cause a {@link Throwable} object.
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }

    /**
     * <p>Constructor for DeleteForbiddenException.</p>
     *
     * @param cause a {@link Throwable} object.
     */
    public UnauthorizedException(Throwable cause) {
        super(ERROR_CODE, cause);
    }

}
