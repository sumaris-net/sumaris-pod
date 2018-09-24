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
import com.google.common.collect.Maps;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionPath;
import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import net.sumaris.server.exception.ErrorCodes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GraphQLHelper {

    private GraphQLHelper() {
        // helper class
    }


    public static Map<String, Object> getVariables(Map<String, Object> request, ObjectMapper objectMapper) {
        Object variablesObj = request.get("variables");
        if (variablesObj == null) {
            return null;
        }
        if (variablesObj instanceof String) {
            String variableStr = (String) variablesObj;
            if (StringUtils.isBlank(variableStr)) {
                return null;
            }
            // Parse String
            try {
                return objectMapper.readValue(variableStr, Map.class);
            } catch (IOException e) {
                throw new SumarisTechnicalException(ErrorCodes.INVALID_QUERY_VARIABLES, e);
            }
        } else if (variablesObj instanceof Map) {
            return (Map<String, Object>) variablesObj;
        }
        else {
            throw new SumarisTechnicalException(ErrorCodes.INVALID_QUERY_VARIABLES, "Unable to read param [variables] from the GraphQL request");
        }
    }

    public static Map<String, Object> processResult(ExecutionResult executionResult) {
        if (CollectionUtils.isEmpty(executionResult.getErrors())) return executionResult.toSpecification();

        Map<String, Object> specifications = Maps.newHashMap();
        specifications.putAll(executionResult.toSpecification());

        List<Map<String, Object>> errors = Lists.newArrayList();
        for (GraphQLError error: executionResult.getErrors()) {
            if (error instanceof ExceptionWhileDataFetching) {
                ExceptionWhileDataFetching exError = (ExceptionWhileDataFetching)error;
                Throwable baseException = getSqlExceptionCause(exError.getException());
                if (baseException instanceof SumarisBusinessException) {
                    SumarisBusinessException exception = (SumarisBusinessException) baseException;
                    error = new AbortExecutionException(String.format("{\"code\": %s, \"message\": \"%s\"}", exception.getCode(), baseException.getMessage()));
                }
                else if (baseException instanceof SumarisTechnicalException) {
                    SumarisTechnicalException exception = (SumarisTechnicalException) baseException;
                    error = new AbortExecutionException(String.format("{\"code\": %s, \"message\": \"%s\"}", exception.getCode(), baseException.getMessage()));
                }
                else {
                    error = new ExceptionWhileDataFetching(ExecutionPath.fromList(exError.getPath()),
                            new GraphQLException(baseException.getMessage()),
                            exError.getLocations().get(0));
                }
            }
            Map<String, Object> newError = Maps.newLinkedHashMap();
            newError.put("message", error.getMessage());
            newError.put("locations", error.getLocations());
            newError.put("path", error.getPath());
            errors.add(newError);
        }
        specifications.put("errors", errors);

        return specifications;
    }


    public static Throwable getSqlExceptionCause(Throwable t) {
        if (t instanceof java.sql.SQLException) {
            return t;
        }
        if (t.getCause() != null) {
            return getSqlExceptionCause(t.getCause());
        }
        return t;
    }
}
