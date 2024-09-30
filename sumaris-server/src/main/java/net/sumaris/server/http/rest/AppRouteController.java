package net.sumaris.server.http.rest;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Match some angular route, then redirect to the .
 * This is useful when POD and APP have NOT the same base url (e.g. When using 2 distinct dockers for POD and APP)
 */
@Controller
@Slf4j
public class AppRouteController {

    private final SumarisServerConfiguration configuration;

    public AppRouteController(SumarisServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @RequestMapping(value= {
        RestPaths.APP_SHARE_PATH,
        RestPaths.APP_EMAIL_CONFIRM_PATH,
        RestPaths.APP_RESET_PASSWORD_PATH
    })
    public ResponseEntity<?> redirectToApp(HttpServletRequest httpServletRequest) {

        String appUrl = configuration.getAppUrl();
        String serverUrl = configuration.getServerUrl();

        // Cannot redirect to app (no url or app = himself) => try to get resource
        // Redirect to himself should never happen, if web server (e.g. nginx) do the redirection to the app's site
        if (StringUtils.isBlank(appUrl) || StringUtils.equalsIgnoreCase(appUrl, serverUrl)) {

            log.warn("Reject request to an app url: {}", httpServletRequest.getRequestURI());
            return ResponseEntity.badRequest().build();
        }

        // Redirect to the app (better solution, in most case)
        String requestUri = httpServletRequest.getRequestURI();
        String queryString = httpServletRequest.getQueryString();
        String location = StringUtils.removeTrailingSlash(appUrl) + requestUri
            + (StringUtils.isNotBlank(queryString) ? "?" + queryString : "");
        int cacheMaxAge = configuration.getAppRedirectionCacheMaxAge();
        String cacheControl = (cacheMaxAge <= 0)
            ? HttpHeaders.Values.NO_CACHE
            : StringUtils.join(new String[]{HttpHeaders.Values.MAX_AGE + cacheMaxAge, HttpHeaders.Values.MUST_REVALIDATE}, ", ");
        return ResponseEntity
            .status(HttpServletResponse.SC_MOVED_TEMPORARILY)
            .header(HttpHeaders.LOCATION, location)
            .header(HttpHeaders.CACHE_CONTROL, cacheControl)
            .build();
    }
}
