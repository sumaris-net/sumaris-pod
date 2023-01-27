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

package net.sumaris.server.http.security;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * @author peck7 on 03/12/2018.
 */
@Slf4j
public class AuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final String TOKEN = "token";
    private static final String BASIC = "Basic";

    private SumarisServerConfiguration configuration;
    private AtomicBoolean ready = new AtomicBoolean(false);
    private boolean enableAuthBasic;
    private boolean enableAuthToken;

    protected AuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher,
                                   SumarisServerConfiguration configuration) {
        super(requiresAuthenticationRequestMatcher);
        this.configuration = configuration;

        this.setEnableAuthBasic(configuration.enableAuthBasic());
        this.setEnableAuthToken(configuration.enableAuthToken());
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {

        this.configuration = this.configuration != null ? this.configuration : SumarisServerConfiguration.getInstance();

        // Update configuration
        setEnableAuthBasic(configuration.enableAuthBasic());
        setEnableAuthToken(configuration.enableAuthToken());

        if (!this.ready.get()) {
            log.info("Started authenticated filter, using {authBasic: {}, authToken: {}}...", enableAuthBasic, enableAuthToken);
            this.ready.set(true);
        }
        else {
            log.info("Updated authenticated filter, using {authBasic: {}, authToken: {}}...", enableAuthBasic, enableAuthToken);
        }
    }

    public boolean isReady() {
        return this.ready.get();
    }

    public void setEnableAuthToken(boolean enableAuthToken) {
        this.enableAuthToken = enableAuthToken;
    }

    public void setEnableAuthBasic(boolean enableAuthBasic) {
        this.enableAuthBasic = enableAuthBasic;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        // When not ready, force to stop the security chain
        if (!this.ready.get()) {
            throw new AuthenticationServiceException(I18n.l(request.getLocale(), "sumaris.error.starting"));
        }

        String authorization = request.getHeader(AUTHORIZATION);
        String[] values = StringUtils.isNotBlank(authorization)
            ? authorization.split(",")
            : new String[]{request.getParameter("t")};

        // Extract Basic authentication
        Optional<Authentication> authentication = !enableAuthBasic ? Optional.empty() : Arrays.stream(values)
            .filter(value -> value != null && value.startsWith(BASIC))
            .map(value -> removeStart(value, BASIC))
            .map(StringUtils::trimToNull)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .map(base64 -> {
                try {
                    String value = new String(Base64.getDecoder().decode(base64));
                    String[] parts = value.split(":", 2);
                    return new UsernamePasswordAuthenticationToken(parts[0], parts[1]);
                }
                catch (Exception e) {
                    throw new BadCredentialsException("Invalid format of basic authentication token.");
                }
            });

        // Extract token authentication
        if (!authentication.isPresent() && enableAuthToken) {
            authentication = Arrays.stream(values)
                .filter(value -> value != null && value.startsWith(TOKEN))
                .map(value -> removeStart(value, TOKEN))
                .map(StringUtils::trimToNull)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(token -> new UsernamePasswordAuthenticationToken(token, token));
        }

        return getAuthenticationManager().authenticate(authentication.orElseGet(() ->
            new UsernamePasswordAuthenticationToken(AnonymousUserDetails.TOKEN, AnonymousUserDetails.TOKEN))
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }
}
