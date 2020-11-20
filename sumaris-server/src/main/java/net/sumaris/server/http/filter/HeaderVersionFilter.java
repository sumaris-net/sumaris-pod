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

package net.sumaris.server.http.filter;

import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.exception.ErrorHelper;
import net.sumaris.server.http.HttpHeaders;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author benoit.lavenier@e-is.pro
 */
public class HeaderVersionFilter extends GenericFilterBean {

    private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
    private Version appMinVersion = null;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        SumarisServerConfiguration config = (SumarisServerConfiguration)event.getConfiguration();
        this.appMinVersion = config.getAppMinVersion();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // No min version to check: check has been disabled, so continue
        if (appMinVersion == null) {
            chain.doFilter(request, response);
            return;
        }

        // Read headers
        String appVersionHeader = request.getHeader(HttpHeaders.X_APP_VERSION);

        // TODO check app Name, to skip if unknown appName ?
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        //String appNameHeader = request.getHeader(HttpHeaders.X_APP_NAME);

        if (StringUtils.isBlank(appVersionHeader)) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug(String.format("No header '%s' found in request: cannot check if app version is compatible. Continue.",
                        HttpHeaders.X_APP_VERSION));
            }
        }
        else {
            if (this.logger.isDebugEnabled()) {
                logger.debug(String.format("Checking header '%s': %s", HttpHeaders.X_APP_VERSION, appVersionHeader));
            }
            Version appVersion = null;
            try {
                appVersion = VersionBuilder.create(appVersionHeader).build();
            } catch (Exception e) {
                logger.error(String.format("Invalid version found in header '%s': %s - Expected format is 'x.y.z'", HttpHeaders.X_APP_VERSION, e.getMessage()));
            }

            if (appVersion != null && appVersion.before(appMinVersion)) {
                handleBadVersion(request,  response, appVersion);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    protected void handleBadVersion(HttpServletRequest request, HttpServletResponse response, Version appVersion) throws IOException, ServletException {
        SecurityContextHolder.clearContext();

        String message = "Invalid App version. Expected: " + appMinVersion + ", actual: " + appVersion;
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Authentication request failed: " + message);
            this.logger.debug("Updated SecurityContextHolder to contain null Authentication");
            this.logger.debug("Delegating to authentication failure handler " + this.failureHandler);
        }

        response.setStatus(ErrorCodes.FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = ErrorHelper.toJsonErrorString(ErrorCodes.BAD_APP_VERSION, message);
        CORSFilter.setCorsHeaders(request, response);
        response.getWriter().print(json);
    }
}
