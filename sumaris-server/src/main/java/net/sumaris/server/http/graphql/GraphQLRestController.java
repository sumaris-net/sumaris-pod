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

package net.sumaris.server.http.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.*;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.service.technical.ConfigurationService;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@RestController
@Slf4j
public class GraphQLRestController {

    private final GraphQL graphQL;
    private final ObjectMapper objectMapper;
    private boolean ready = false;

    @Autowired
    public GraphQLRestController(GraphQL graphQL,
                                 ObjectMapper objectMapper) {
        this.graphQL = graphQL;
        this.objectMapper = objectMapper;
        log.info("Starting GraphQL endpoint {{}}...", GraphQLPaths.BASE_PATH);
    }

    @EventListener({ConfigurationReadyEvent.class})
    public void onConfigurationReady(ConfigurationReadyEvent event) {
        ready = true;
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
        ExecutionResult result;
        if (!this.ready) {
            result = new ExecutionResultImpl.Builder()
                .addError(GraphqlErrorBuilder.newError().message(I18n.l(rawRequest.getLocale(), "sumaris.error.starting")).build())
                .build();
        }
        else {
            result = graphQL.execute(ExecutionInput.newExecutionInput()
                .query((String) request.get("query"))
                .operationName((String) request.get("operationName"))
                .variables(GraphQLHelper.getVariables(request, objectMapper))
                .context(rawRequest)
                .build());
        }

        return GraphQLHelper.processExecutionResult(result);
    }

    /* -- private methods -- */

}
