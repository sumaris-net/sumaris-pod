package net.sumaris.server.http.security;

import org.springframework.security.web.RedirectStrategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * As we’re securing a REST API, in case of authentication failure, the server should not redirect to any error page. The server will simply return an HTTP 401 (Unauthorized)
 *
 * @author peck7 on 03/12/2018.
 */
public class NoRedirectStrategy implements RedirectStrategy {

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) {
        // No redirect is required with pure REST
    }
}
