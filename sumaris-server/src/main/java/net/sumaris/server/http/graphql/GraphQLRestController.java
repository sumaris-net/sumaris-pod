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
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@RestController
@Slf4j
public class GraphQLRestController {

    private final GraphQL graphQL;
    private final ObjectMapper objectMapper;
    private boolean ready;

    @Autowired
    public GraphQLRestController(GraphQLSchema graphQLSchema,
                                 ObjectMapper objectMapper,
                                 SumarisConfiguration configuration) {
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        this.objectMapper = objectMapper;
        this.ready = !configuration.enableConfigurationDbPersistence(); // Mark as ready, if configuration not loaded from DB
        log.info(String.format("Starting GraphQL endpoint {%s}...", GraphQLPaths.BASE_PATH));
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        this.ready = true;
    }

    @PostMapping(value = GraphQLPaths.BASE_PATH,
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_UTF8_VALUE
            },
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_UTF8_VALUE
            })
    @ResponseBody
    public Map<String, Object> indexFromAnnotated(@RequestBody Map<String, Object> request, HttpServletRequest rawRequest) {
        if (!this.ready) {
            throw new SumarisTechnicalException("Pod not ready. Please wait");
        }
        ExecutionResult executionResult = graphQL.execute(ExecutionInput.newExecutionInput()
                .query((String) request.get("query"))
                .operationName((String) request.get("operationName"))
                .variables(GraphQLHelper.getVariables(request, objectMapper))
                .context(rawRequest)
                .build());

        return GraphQLHelper.processExecutionResult(executionResult);
    }

    /* -- private methods -- */

}
