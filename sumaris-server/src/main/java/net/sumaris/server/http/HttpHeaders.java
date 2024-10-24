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

package net.sumaris.server.http;

/**
 * Application specific HTTP headers
 */
public final class HttpHeaders extends org.springframework.http.HttpHeaders {

    /**
     * Allow to store entities ID that some deletion have been denied
     */
    public static String ACCESS_CONTROL_DENY_DELETION_ENTITIES = "Access-Control-Deny-Deletion-Entities";

    public interface Values {
        String NO_CACHE = "no-cache";
        String MAX_AGE = "max-age=";
        String MUST_REVALIDATE = "must-revalidate";
    }

}
