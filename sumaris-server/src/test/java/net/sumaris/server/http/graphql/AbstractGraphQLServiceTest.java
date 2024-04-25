
package net.sumaris.server.http.graphql;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2021 SUMARiS Consortium
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.technical.ConfigurationVO;
import net.sumaris.server.AbstractServiceTest;
import net.sumaris.server.util.security.AuthTokenVO;
import org.junit.Assert;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Ignore
public class AbstractGraphQLServiceTest extends AbstractServiceTest {

    public static final String RESOURCE_PATTERN = "graphql/%s.graphql";
    public static final String DATA_HEADER = "data";
    public static final String ERROR_HEADER = "errors";

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private GraphQLTestTemplate graphQLTestTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected ObjectNode buildReferentialFilter(ReferentialFilterVO filter) {
        return buildReferentialFilter(filter, null);
    }

    protected ObjectNode buildReferentialFilter(ReferentialFilterVO filter, String entityName) {
        // build filter
        ObjectNode variables = objectMapper.createObjectNode();
        if (entityName != null)
            variables.put("entityName", entityName);
        variables.set("filter", objectMapper.valueToTree(filter));
        return variables;
    }

    protected boolean authenticate(String login, String password) throws SumarisTechnicalException {

        // Build header
        clearGraphQLHeaders();

        // Ask for challenge from server
        AuthTokenVO serverAuthData = getResponse("authChallenge", AuthTokenVO.class);
        assertNotNull(serverAuthData);
        assertNotNull(serverAuthData.getChallenge());
        assertNotNull(serverAuthData.getSignature());
        assertNotNull(serverAuthData.getPubkey());

        // Build user AuthData
        String token = createToken(serverAuthData.getChallenge(), login, password);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", token);

        addGraphQLHeader(HttpHeaders.AUTHORIZATION, "Basic " + CryptoUtils.encodeBase64(String.format("%s:%s", login, password).getBytes(StandardCharsets.UTF_8)));

        boolean auth = getResponse("authenticate", Boolean.class, variables);
        if (auth) {
            withGraphQLHeader(HttpHeaders.AUTHORIZATION, "token " + token);
        } else {
            clearGraphQLHeaders();
        }
        return auth;
    }

    protected void clearGraphQLHeaders() {
        graphQLTestTemplate.withClearHeaders();
    }

    protected void withGraphQLHeader(String name, String value) {
        graphQLTestTemplate.withClearHeaders().withAdditionalHeader(name, value);
    }

    protected void addGraphQLHeader(String name, String value) {
        graphQLTestTemplate.withAdditionalHeader(name, value);
    }

    protected <R> R getResponse(String queryName, Class<R> responseClass) throws SumarisTechnicalException {
        return getResponse(queryName, queryName, responseClass, null, null);
    }

    protected <R> R getResponse(String queryName, Class<R> responseClass, ObjectNode variables) throws SumarisTechnicalException {
        return getResponse(queryName, queryName, responseClass, null, variables);
    }

    protected <R, T> R getResponse(String queryName, Class<R> responseClass, Class<T> responseCollectionType, ObjectNode variables) throws SumarisTechnicalException {
        return getResponse(queryName, queryName, responseClass, responseCollectionType, variables);
    }

    protected <R, T> R getResponse(String queryName, Class<R> responseClass, Class<T> responseCollectionType, ObjectNode variables, String... fragmentNames) throws SumarisTechnicalException {
        return getResponse(queryName, queryName, responseClass, responseCollectionType, variables, fragmentNames);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <R, T> R getResponse(String resourceName, String queryName, Class<R> responseClass, Class<T> responseCollectionType, ObjectNode variables, String... fragmentNames)
        throws SumarisTechnicalException {
        assertTrue(StringUtils.isNotEmpty(resourceName));
        assertTrue(StringUtils.isNotEmpty(queryName));
        assertNotNull(responseClass);
        assertTrue(responseCollectionType == null || Collection.class.isAssignableFrom(responseClass));
        String queryResource = getResourcePath(resourceName);
        List<String> fragmentResources = fragmentNames != null ? Arrays.stream(fragmentNames)
            .filter(Objects::nonNull)
            .map(this::getResourcePath)
            .toList() : null;

        // add missing variables
        if (variables != null) {
            if (!variables.has("offset")) variables.put("offset", 0);
            if (!variables.has("size")) variables.put("size", 1000);
        }

        GraphQLResponse response = null;
        try {
            response = graphQLTestTemplate.perform(queryResource, variables, fragmentResources);
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                throw new SumarisTechnicalException("Error while performing request"); // this exception can be thrown when response body is empty with status 4xx
            }
            Assert.fail("Exception during graphql call: " + e.getMessage());
        }
        assertNotNull(response);
        assertTrue(response.isOk());
        assertNotNull(response.getRawResponse());
        assertNotNull(response.getRawResponse().getBody());

        // Parse body
        String body = response.getRawResponse().getBody();
        JsonNode nodes = assertDoesNotThrow(() -> objectMapper.readTree(body));
        if (nodes.hasNonNull(ERROR_HEADER)) {
            throw new SumarisTechnicalException(nodes.get(ERROR_HEADER).toPrettyString());
        }
        if (nodes.hasNonNull(DATA_HEADER) && nodes.get(DATA_HEADER).hasNonNull(queryName)) {
            R result = assertDoesNotThrow(() -> objectMapper.treeToValue(nodes.get(DATA_HEADER).get(queryName), responseClass));
            if (responseCollectionType == null) {
                return result;
            } else {
                R collectionResult = Beans.newInstance(responseClass);
                ((Collection) collectionResult).addAll(
                    ((Collection<?>) result).stream()
                        .map(o -> assertDoesNotThrow(() -> objectMapper.treeToValue(objectMapper.valueToTree(o), responseCollectionType)))
                        .toList()
                );
                return collectionResult;
            }
        } else {
            return null;
        }
    }

    private String getResourcePath(String resourceName) {
        return String.format(RESOURCE_PATTERN, resourceName);
    }

    protected ObjectNode asObjectNode(Map<String, Object> aMap) {
        ObjectNode variables = objectMapper.createObjectNode();

        try {
            for (Map.Entry<String, Object> entry : aMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Integer integer) {
                    variables.put(entry.getKey(), integer);
                } else if (value instanceof Double aDouble) {
                    variables.put(entry.getKey(), aDouble);
                } else if (value instanceof String string) {
                    variables.put(entry.getKey(), string);
                } else {
                    JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsBytes(entry.getValue()));
                    variables.putIfAbsent(entry.getKey(), jsonNode);
                }
            }
            return variables;
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }
    protected ConfigurationVO loadConfiguration() {
        return getResponse("configuration", ConfigurationVO.class);
    }
}
