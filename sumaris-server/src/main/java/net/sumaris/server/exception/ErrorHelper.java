package net.sumaris.server.exception;

/*-
 * #%L
 * SUMARiS:: Server
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

import net.sumaris.core.util.StringUtils;

public final class ErrorHelper {

    private ErrorHelper() {
        // helper class
    }

    /**
     * <p>getInternalServerErrorMessage.</p>
     *
     * @param errorCode a int.
     * @param message a {@link String} object.
     * @return a {@link String} object.
     */
    public static String toJsonErrorString(int errorCode, String message) {
        if (StringUtils.isBlank(message)) {
            return String.format("{\"code\": %s}", errorCode);
        }
        return String.format("{\"code\": %s, \"message\": \"%s\"}",
            errorCode,
            // Escape quotes
            message.replaceAll("\"", "")
        );
    }
}
