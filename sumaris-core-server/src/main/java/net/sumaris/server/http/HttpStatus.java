package net.sumaris.server.http;

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

/**
 * Application HTTP status
 */
public interface HttpStatus extends org.apache.http.HttpStatus {
    /** Constant <code>SC_DATA_LOCKED=520</code> */
    int SC_DATA_LOCKED = 520;
    /** Constant <code>SC_BAD_UPDATE_DT=521</code> */
    int SC_BAD_UPDATE_DT = 521;
    /** Constant <code>SC_DELETE_FORBIDDEN=522</code> */
    int SC_DELETE_FORBIDDEN = 522;
    /** Constant <code>INVALID_EMAIL_CONFIRMATION=522</code> */
    int INVALID_EMAIL_CONFIRMATION = 523;
}
