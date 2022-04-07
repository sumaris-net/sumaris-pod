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

public interface ErrorCodes extends net.sumaris.core.exception.ErrorCodes {

    // >= 400
    int UNAUTHORIZED = org.springframework.http.HttpStatus.UNAUTHORIZED.value(); // 401
    int FORBIDDEN = org.springframework.http.HttpStatus.FORBIDDEN.value();

    // >= 550
    int INVALID_EMAIL_CONFIRMATION = 550;
    int INVALID_QUERY_VARIABLES = 551;
    int ACCOUNT_ALREADY_EXISTS = 552;
    int BAD_APP_VERSION = 553;
    int INVALID_MESSAGE = 554;

}
