package net.sumaris.server.http.rest;

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
