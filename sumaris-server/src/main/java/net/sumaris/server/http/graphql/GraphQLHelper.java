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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.AbortExecutionException;
import graphql.execution.ResultPath;
import graphql.kickstart.execution.error.GenericGraphQLError;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisBusinessException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.exception.ErrorHelper;
import net.sumaris.server.http.security.AnonymousUserDetails;
import net.sumaris.server.http.security.AuthService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class GraphQLHelper extends GraphQLUtils {

    protected final static ObjectMapper jacksonMapper = new ObjectMapper();

    private GraphQLHelper() {
        // helper class
    }

    public static Map<String, Object> getVariables(Map<String, Object> request, ObjectMapper objectMapper) {
        Object variablesObj = request.get("variables");
        if (variablesObj == null) {
            return Maps.newHashMap(); // Need empty map
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

    public static Map<String, Object> processExecutionResult(ExecutionResult executionResult) {
        if (CollectionUtils.isEmpty(executionResult.getErrors())) return executionResult.toSpecification();

        Map<String, Object> specifications = Maps.newHashMap();
        specifications.putAll(executionResult.toSpecification());

        List<Map<String, Object>> errors = Lists.newArrayList();
        for (GraphQLError error: executionResult.getErrors()) {
            error = processGraphQLError(error);
            Map<String, Object> newError = Maps.newLinkedHashMap();
            newError.put("message", error.getMessage());
            newError.put("locations", error.getLocations());
            newError.put("path", error.getPath());
            errors.add(newError);
        }
        specifications.put("errors", errors);

        return specifications;
    }

    public static Map<String, Object> processError(Throwable throwable) {

        Map<String, Object> payload = Maps.newHashMap();
        List<Map<String, Object>> errors = Lists.newArrayList();

        Throwable cause = getSqlExceptionOrRootCause(throwable);
        GraphQLError error = tryCreateGraphQLError(cause);
        if (error == null) {
            error = new GenericGraphQLError(cause.getMessage());
        }

        Map<String, Object> newError = Maps.newLinkedHashMap();
        newError.put("message", error.getMessage());
        newError.put("locations", error.getLocations());
        newError.put("path", error.getPath());
        errors.add(newError);

        payload.put("errors", errors);

        return payload;
    }

    public static GraphQLError processGraphQLError(final GraphQLError error) {
        if (error instanceof ExceptionWhileDataFetching) {
            ExceptionWhileDataFetching exError = (ExceptionWhileDataFetching) error;
            Throwable baseException = getSqlExceptionOrRootCause(exError.getException());

            // try to create it from the exception
            GraphQLError result = tryCreateGraphQLError(baseException);
            if (result != null) return result;

            return new ExceptionWhileDataFetching(ResultPath.fromList(exError.getPath()), // TODO migrate to ResultPath (in graphql-java 16)
                    new GraphQLException(baseException.getMessage()),
                    exError.getLocations().get(0));
        }
        return error;
    }

    public static GraphQLError tryCreateGraphQLError(final Throwable error) {
        String jsonString = toJsonErrorString(error);
        if (jsonString == null) return null;

        return new AbortExecutionException(jsonString);
    }


    public static Throwable getSqlExceptionOrRootCause(Throwable t) {
        if (t instanceof java.sql.SQLException) {
            return t;
        }
        if (t.getCause() != null) {
            return getSqlExceptionOrRootCause(t.getCause());
        }
        return t;
    }


    public static String toJsonErrorString(final Throwable error) {
        Throwable cause = getSqlExceptionOrRootCause(error);

        // Sumaris exceptions
        if (cause instanceof SumarisBusinessException) {
            SumarisBusinessException exception = (SumarisBusinessException) cause;
            return toJsonErrorString(exception.toSpecification());
        }
        else if (cause instanceof SumarisTechnicalException) {
            SumarisTechnicalException exception = (SumarisTechnicalException) cause;
            return toJsonErrorString(exception.toSpecification());
        }

        // SQL exceptions
        else if (cause instanceof java.sql.SQLException) {
            return toJsonErrorString(ErrorCodes.INTERNAL_ERROR, error.getMessage());
        }

        // Spring exceptions
        else if (cause instanceof DataRetrievalFailureException) {
            DataRetrievalFailureException exception = (DataRetrievalFailureException) cause;
            return toJsonErrorString(ErrorCodes.NOT_FOUND, exception.getMessage());
        }

        // Spring Security exceptions
        else if (cause instanceof AccessDeniedException) {
            return toJsonErrorString(ErrorCodes.UNAUTHORIZED, cause.getMessage());
        }

        return null;
    }

    public static String toJsonErrorString(Map<String, Object> error){
        try {
            return jacksonMapper.writeValueAsString(error);
        }
        catch(JsonProcessingException e) {
            log.error("Failed to serialize map to JSON: " + e.getMessage(), e);
            return toJsonErrorString((Integer)error.get("code"), (String)error.get("message"));
        }
    }
    public static String toJsonErrorString(int code, String message) {
        return ErrorHelper.toJsonErrorString(code, message);
    }

    public static void logDeprecatedUse(AuthService authService, String functionName, String appVersion) {
        String userId = authService.getAuthenticatedUserId().map(Object::toString)
            .orElse(AnonymousUserDetails.TOKEN);
        log.warn("User {id: {}} used {{}} that is deprecated since {appVersion: {}}.", userId, functionName, appVersion);
    }
}
