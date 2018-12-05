package net.sumaris.server.http.graphql;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.*;
import graphql.schema.GraphQLSchema;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.http.HttpHeaders;
import net.sumaris.server.http.security.AuthService;
import org.hibernate.tool.hbm2x.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class GraphQLRestController {

    private static final Logger log = LoggerFactory.getLogger(GraphQLRestController.class);

    private final GraphQL graphQL;

    private final ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    public GraphQLRestController(GraphQLSchema schema,
                                 ObjectMapper objectMapper) {
        this.graphQL = GraphQL.newGraphQL(schema).build();
        this.objectMapper = objectMapper;
        log.info(String.format("Starting GraphQL rest controller at {%s}...", GraphQLPaths.BASE_PATH));
    }

    @PostMapping(value = GraphQLPaths.BASE_PATH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Object> indexFromAnnotated(@RequestBody Map<String, Object> request, HttpServletRequest rawRequest) {
        String token = rawRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (token != null) {
            log.debug("Checking auth for token " + token);
            if (!authService.authenticate(token)) {
                Throwable error = new SumarisTechnicalException(GraphQLHelper.toJsonErrorString(ErrorCodes.UNAUTHORIZED, "Authentication required"));
                return GraphQLHelper.processError(error);
            }
        }
        else {
            log.warn("Executing GraphQL rest request without auth token!");
            // TODO
            //Throwable error = new SumarisTechnicalException(GraphQLHelper.toJsonErrorString(ErrorCodes.UNAUTHORIZED, "Authentication required"));
            //return GraphQLHelper.processError(error);
        }
        ExecutionResult executionResult = graphQL.execute(ExecutionInput.newExecutionInput()
                .query((String)request.get("query"))
                .operationName((String)request.get("operationName"))
                .variables(GraphQLHelper.getVariables(request, objectMapper))
                .context(rawRequest)
                .build());

        return GraphQLHelper.processExecutionResult(executionResult);
    }

    /* -- private methods -- */

}
