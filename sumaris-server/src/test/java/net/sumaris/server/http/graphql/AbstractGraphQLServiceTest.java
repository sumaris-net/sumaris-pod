
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import com.graphql.spring.boot.test.GraphQLTestTemplate;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.server.AbstractServiceTest;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    protected <R> R getResponse(String queryName, Class<R> responseClass) throws SumarisTechnicalException {
        return getResponse(queryName, queryName, responseClass, null, null);
    }

    protected <R> R getResponse(String queryName, Class<R> responseClass, ObjectNode variables) throws SumarisTechnicalException {
        return getResponse(queryName, queryName, responseClass, null, variables);
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
        List<String> fragmentResources = fragmentNames != null ? Arrays.stream(fragmentNames).map(this::getResourcePath).collect(Collectors.toList()) : null;

        // add missing variables
        if (variables != null) {
            if (!variables.has("offset")) variables.put("offset", 0);
            if (!variables.has("size")) variables.put("size", 1000);
        }

        GraphQLResponse response = assertDoesNotThrow(() ->
            graphQLTestTemplate.perform(queryResource, variables, fragmentResources));
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
                        .collect(Collectors.toList())
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
}
