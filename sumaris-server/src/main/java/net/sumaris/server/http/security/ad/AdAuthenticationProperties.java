package net.sumaris.server.http.security.ad;

/*-
 * #%L
 * Quadrige3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2020 Ifremer
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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
    prefix = "spring.security.ad"
)
@Data
public class AdAuthenticationProperties {
    /**
     * Main property for Active Directory authentication: the AD server url
     * ex: ldap://localhost:389
     */
    private String url;

    /**
     * The Active Directory domain
     */
    private String domain;

    /**
     * Base distinguished name for authentication/user
     * ex: cn=Users
     */
    private String baseDn;

}
