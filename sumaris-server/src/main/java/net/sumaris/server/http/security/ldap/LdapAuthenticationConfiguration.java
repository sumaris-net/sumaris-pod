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

import net.sumaris.server.http.security.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;

@Configuration
@ConditionalOnProperty(name = "spring.security.ldap.enabled", havingValue = "true")
@EnableConfigurationProperties({LdapAuthenticationProperties.class})
public class LdapAuthenticationConfiguration {

    private final LdapAuthenticationProperties ldapAuthenticationProperties;
    private final AuthService authService;

    public LdapAuthenticationConfiguration(LdapAuthenticationProperties ldapAuthenticationProperties, AuthService authService) {
        this.ldapAuthenticationProperties = ldapAuthenticationProperties;
        this.authService = authService;
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource(ldapAuthenticationProperties.getUrl());
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource());
        authenticator.setUserDnPatterns(ldapAuthenticationProperties.getUserDnPatterns());
        return new LdapAuthenticationProvider(authenticator, ldapAuthenticationProperties, authService);
    }
}