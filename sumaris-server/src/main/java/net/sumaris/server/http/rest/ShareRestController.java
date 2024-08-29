package net.sumaris.server.http.rest;

import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class ShareRestController {

    private final SumarisServerConfiguration configuration;

    public ShareRestController(SumarisServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @GetMapping(value= RestPaths.SHARE_PATH + "/**")
    public void redirectAppUrl(HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest) {
        String appUrl = configuration.getAppUrl();
        String serverUrl = configuration.getServerUrl();
        if (StringUtils.equalsIgnoreCase(appUrl, serverUrl) || StringUtils.isBlank(appUrl)) {
            throw  new DataNotFoundException("Not found");
        }
        String requestUri = httpServletRequest.getRequestURI();
        String requestParams = httpServletRequest.getQueryString();
        httpServletResponse.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        httpServletResponse.setHeader("Location", appUrl + requestUri + (requestParams != null ? "?" + requestParams : ""));
    }
}
