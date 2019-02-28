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
import com.google.common.collect.Lists;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Stream;

@RestController
public class GraphQLRestController {

    private static final Logger log = LoggerFactory.getLogger(GraphQLRestController.class);

    private static final List<Locale> AVAILABLE_LANG =  Lists.newArrayList( Locale.FRENCH, Locale.ENGLISH  );

    private final GraphQL graphQL;

    private final ObjectMapper objectMapper;

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


        Map<String, Object> variables = GraphQLHelper.getVariables(request, objectMapper);

        String lang = findLanguageFromHttpHeaders(rawRequest, AVAILABLE_LANG);

        ExecutionResult executionResult = graphQL.execute(ExecutionInput.newExecutionInput()
                .query((String)request.get("query"))
                .operationName((String)request.get("operationName"))
                .variables(variables)
                .context(lang)
                .build());

        return GraphQLHelper.processExecutionResult(executionResult);
    }

    /* -- private methods -- */

    /**
     * This method sort languages by the book
     *  ex : Accept-Language: de; q=1.0, en; q=0.5
     * would prioritize 'de' because 'q=1.0' is higher priority than 'q=0.5'
     *
     * @param rawRequest the httpRequest containing the Accept-Language headers to use
     * @return the lang suffix
     */
    protected String findLanguageFromHttpHeaders(HttpServletRequest rawRequest, List<Locale> available){
        String acceptedLang = rawRequest.getHeader("Accept-Language");
        return Stream.of(acceptedLang.split(","))
                .map(s -> {
                    String[] split = s.trim().split(";");
                    if (!(split.length > 1))
                        return null;
                    String[] subSplit = split[1].split("=");
                    if (!(subSplit.length > 1))
                        return null;

                    Double v = Double.parseDouble(subSplit[1]);
                    return new AbstractMap.SimpleEntry<>(split[0], v);

                })
                .filter(Objects::nonNull)
                .filter(l ->  available.stream() .anyMatch(loca ->
                        l.getKey().startsWith(loca.toString())))
                .sorted((f1, f2) -> Double.compare(f2.getValue(), f1.getValue() ))
                .map(AbstractMap.SimpleEntry::getKey)
                .findFirst()
                .orElse("en-GB"); // FIXME default value from ... ?

    }

}
