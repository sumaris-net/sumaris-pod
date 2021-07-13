package net.sumaris.server.http.security.ad;

/*-
 * #%L
 * Quadrige3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2021 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryAuthenticationException;

/**
 * Copy of final class {@link ActiveDirectoryAuthenticationException}
 */
public class AdAuthenticationException extends AuthenticationException {

    private final String dataCode;

    AdAuthenticationException(String dataCode, String message, Throwable cause) {
        super(message, cause);
        this.dataCode = dataCode;
    }

    public String getDataCode() {
        return dataCode;
    }
}
