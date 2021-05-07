package net.sumaris.server.http.security.ldap;

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

import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.service.crypto.CryptoService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.server.http.security.AnonymousUserDetails;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.AuthUserDetails;
import net.sumaris.server.util.security.AuthTokenVO;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import javax.annotation.Resource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LdapAuthenticationProvider
    extends org.springframework.security.ldap.authentication.LdapAuthenticationProvider {

    private final String userDn;

    @Resource
    private AuthService authService;

    public LdapAuthenticationProvider(LdapAuthenticator authenticator, LdapProperties properties) {
        super(authenticator);
        this.userDn = properties.getUserDn();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // First check anonymous user
        if (AnonymousUserDetails.TOKEN.equals(authentication.getPrincipal())) return authentication;

        // Authenticate on LDAP server
        authentication = super.authenticate(authentication);

        // Extract user login, to use as principal
        Object principal = authentication.getPrincipal();
        if (principal instanceof LdapUserDetails) {
            LdapUserDetails ldapUserDetails = (LdapUserDetails) principal;
            String username = ldapUserDetails.getUsername();

            // Retrieve login by finding uid={} somewhere in the distinguished name
            if (StringUtils.isNotBlank(this.userDn)) {
                Pattern pattern = Pattern.compile(userDn + "=(.*?)(,|$)");
                Matcher matcher = pattern.matcher(ldapUserDetails.getDn());
                if (matcher.find()) {
                    username = matcher.group(1);
                } else {
                    throw new BadCredentialsException(String.format("Ldap attribute '%s' not found", userDn));
                }
            }

            UsernamePasswordAuthenticationToken userToken = new UsernamePasswordAuthenticationToken(
                username,
                authentication.getCredentials(),
                authentication.getAuthorities());

            UserDetails userDetails = retrieveUser(username, userToken);
            userToken.setDetails(userDetails);

            return userToken;
        }

        return authentication;
    }


    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        return authService.authenticateByUsername(username, authentication);
    }
}
