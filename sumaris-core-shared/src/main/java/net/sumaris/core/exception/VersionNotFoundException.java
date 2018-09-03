package net.sumaris.core.exception;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
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
 * <p>VersionNotFoundException class.</p>
 *
 */
public class VersionNotFoundException extends Exception {

    private static final long serialVersionUID = 3181414373998539962L;

    /**
     * <p>Constructor for VersionNotFoundException.</p>
     */
    public VersionNotFoundException() {
        super();
    }

    /**
     * <p>Constructor for VersionNotFoundException.</p>
     *
     * @param message a {@link String} object.
     */
    public VersionNotFoundException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for VersionNotFoundException.</p>
     *
     * @param message a {@link String} object.
     * @param cause a {@link Throwable} object.
     */
    public VersionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
