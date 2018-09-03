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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.lambdaworks.codec.Base64;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.apache.commons.collections4.CollectionUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SubscriptionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionWebSocketHandler.class);


    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

    private final GraphQL graphQL;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public SubscriptionWebSocketHandler(GraphQLSchema graphQLSchema) {
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        if (subscriptionRef.get() != null) subscriptionRef.get().cancel();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        Map<String, Object> request;
        try {
            request = objectMapper.readValue(message.asBytes(), Map.class);
            log.debug("Getting WS request :" + request);
        }
        catch(IOException e) {
            log.error("Bad WS request: " + e.getMessage());
            return;
        }

        String type = Objects.toString(request.get("type"), "start");
        if ("connection_init".equals(type)) {
            // TODO : get auth data
            /*
            final Object opId = request.get("id");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                                ImmutableMap.of(
                                        "id", opId,
                                        "type", "connection_error",
                                        "payload", ...))
                        )));
             */
            return; // ignore
        }
        else if ("stop".equals(type)) {
            if (subscriptionRef.get() != null) subscriptionRef.get().cancel();
        }
        else if ("start".equals(type)) {
            Map<String, Object> payload = (Map<String, Object>)request.get("payload");
            final Object opId = request.get("id");

            String query = Objects.toString(payload.get("query"));
            ExecutionResult executionResult = graphQL.execute(ExecutionInput.newExecutionInput()
                    .query(query)
                    .operationName((String) payload.get("operationName"))
                    .variables(GraphQLHelper.getVariables(payload, objectMapper))
                    .build());

            //ExecutionResult executionResult = graphQL.execute(msq);

            // TODO: read variables, query, etc. ?

            // If error: send error then disconnect
            if (CollectionUtils.isNotEmpty(executionResult.getErrors())) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            ImmutableMap.of(
                                    "id", opId,
                                    "type", "error",
                                    "payload", GraphQLHelper.processResult(executionResult))
                    )));

                    //session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
                return;
            }

            Publisher<ExecutionResult> stream = executionResult.getData();

            stream.subscribe(new Subscriber<ExecutionResult>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriptionRef.set(subscription);
                    if (subscriptionRef.get() != null) subscriptionRef.get().request(1);
                }

                @Override
                public void onNext(ExecutionResult result) {
                    try {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                                ImmutableMap.of(
                                        "id", opId,
                                        "type", "data",
                                        "payload", GraphQLHelper.processResult(result))
                        )));
                        //session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result.toSpecification())));
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }

                    if (subscriptionRef.get() != null) subscriptionRef.get().request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    try {
                        session.close(CloseStatus.SERVER_ERROR);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }

                @Override
                public void onComplete() {
                    try {
                        session.close();
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
    }


}
