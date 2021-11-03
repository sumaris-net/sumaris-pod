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

package net.sumaris.extraction.core.exception;

import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.shared.exception.ErrorCodes;

public class UnknownFormatException extends SumarisBusinessException {

    public static final int ERROR_CODE = ErrorCodes.BAD_REQUEST;

    public UnknownFormatException() {
        super(ERROR_CODE, "Unknown format exception");
    }

    public UnknownFormatException(Throwable cause) {
        super(ERROR_CODE, cause);
    }

    public UnknownFormatException(String message) {
        super(ERROR_CODE, message);
    }
}
