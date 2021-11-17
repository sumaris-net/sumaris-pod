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

package net.sumaris.server.http.security.ad;

import net.sumaris.server.http.security.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;

@Configuration
@ConditionalOnProperty(name = "spring.security.ad.enabled", havingValue = "true")
@EnableConfigurationProperties({AdAuthenticationProperties.class})
public class AdAuthenticationConfiguration {

    private final AdAuthenticationProperties adAuthenticationProperties;
    private final AuthService authService;

    public AdAuthenticationConfiguration(AdAuthenticationProperties adAuthenticationProperties, AuthService authService) {
        this.adAuthenticationProperties = adAuthenticationProperties;
        this.authService = authService;
    }

    @Bean
    AuthenticationProvider adAuthenticationProvider() {
        AdAuthenticationProvider provider = new AdAuthenticationProvider(
            adAuthenticationProperties.getDomain(),
            adAuthenticationProperties.getUrl(),
            adAuthenticationProperties.getBaseDn(),
            authService
        );
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        return provider;
    }

}
