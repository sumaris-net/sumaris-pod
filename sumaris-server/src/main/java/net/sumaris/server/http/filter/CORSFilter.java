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

import net.sumaris.server.http.HttpHeaders;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Order(0)
public class CORSFilter implements Filter {


    public static final String[] ALLOWED_METHODS = new String[]{
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.HEAD.name(),
        HttpMethod.OPTIONS.name()
    };

    public static final String[] ALLOWED_HEADERS = new String[]{
        HttpHeaders.ACCEPT,
        HttpHeaders.ACCEPT_LANGUAGE,
        HttpHeaders.CONTENT_LANGUAGE,
        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
        HttpHeaders.CONTENT_TYPE,
        HttpHeaders.AUTHORIZATION
    };

    public static final boolean ALLOWED_CREDENTIALS = true;

    public static final int MAX_AGE = 3600;

    protected static final String ALLOWED_METHODS_STR = Arrays.stream(ALLOWED_METHODS).collect(Collectors.joining(", "));
    protected static final String ALLOWED_HEADERS_STR = Arrays.stream(ALLOWED_HEADERS).collect(Collectors.joining(", "));
    protected static final String MAX_AGE_STR = String.valueOf(MAX_AGE);
    protected static final String ALLOWED_CREDENTIALS_STR = Boolean.toString(ALLOWED_CREDENTIALS);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Add CORS headers
        setCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy() {

    }

    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS_STR);
        response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS_STR);
        response.setHeader("Access-Control-Allow-Credentials", ALLOWED_CREDENTIALS_STR);
        response.setHeader("Access-Control-Max-Age", MAX_AGE_STR);
    }
}
