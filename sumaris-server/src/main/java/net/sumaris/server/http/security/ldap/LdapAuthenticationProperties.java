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

package net.sumaris.server.http.security.ldap;

import lombok.Data;
import net.sumaris.core.util.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
    prefix = "spring.security.ldap"
)
@Data
public class LdapAuthenticationProperties {
    /**
     * Main property for LDAP authentication: the LDAP server url
     * ex: ldap://localhost:389/dc=ifremer,dc=fr
     */
    private String url;

    /**
     * Determine LDAP attribute used for authentication
     * default: "uid"
     */
    private String userDn = "uid";

    /**
     * Base distinguished name for authentication
     * ex: ou=annuaire
     */
    private String baseDn = "";

    /**
     * Compute User DN patterns, using value of 'baseDn' and 'userDn'
     * @return
     */
    public String[] getUserDnPatterns() {
        String userDn = String.format("%s={0}", this.userDn);
        if (StringUtils.isNotBlank(this.baseDn)) {
            // Build patterns like "uid={0},ou=annuaire", "uid={0}" (the 2nd is here for compatibility)
            return new String[]{
                String.format("%s,%s", userDn, this.baseDn),
                userDn
            };
        } else {
            // Build patterns like "uid={0}"
            return new String[]{userDn};
        }
    }
}
