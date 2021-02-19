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
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.schema.GraphQLSchema;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.AuthUser;
import net.sumaris.server.util.security.AuthDataVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class SubscriptionWebSocketHandler extends TextWebSocketHandler {

    private final AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

    private final boolean debug;

    private List<WebSocketSession> sessions = new CopyOnWriteArrayList();

    private GraphQL graphQL;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    public SubscriptionWebSocketHandler(GraphQLSchema graphQLSchema) {
        this.debug = log.isDebugEnabled();
        this.graphQL = GraphQL.newGraphQL(graphQLSchema)
                .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // keep all sessions (for broadcast)
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        if (subscriptionRef.get() != null) subscriptionRef.get().cancel();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        Map<String, Object> request;
        try {
            request = objectMapper.readValue(message.asBytes(), Map.class);
            if (debug) log.debug(I18n.t("sumaris.server.subscription.getRequest", request));
        }
        catch(IOException e) {
            log.error(I18n.t("sumaris.server.error.subscription.badRequest", e.getMessage()));
            return;
        }

        String type = Objects.toString(request.get("type"), "start");
        if ("connection_init".equals(type)) {
            handleInitConnection(session, request);
        }
        else if ("stop".equals(type)) {
            if (subscriptionRef.get() != null) subscriptionRef.get().cancel();
        }
        else if ("start".equals(type)) {
            handleStartConnection(session, request);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
    }

    /* -- protected methods -- */

    protected void handleInitConnection(WebSocketSession session, Map<String, Object> request) {
        Map<String, Object> payload = (Map<String, Object>) request.get("payload");
        String authToken = MapUtils.getString(payload, "authToken");

        // Has token: try to authenticate
        if (StringUtils.isNotBlank(authToken)) {

            // try to authenticate
            try {
                Optional<AuthUser> authUser = authService.authenticate(authToken);
                // If success
                if (authUser.isPresent()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authUser.get().getUsername(), authToken, authUser.get().getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    return; // OK
                }
            }
            catch(AuthenticationException e) {
                log.warn("Unable to authenticate websocket session, using token: " + e.getMessage());
                // Continue
            }
        }

        // Not auth: send a new challenge
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    ImmutableMap.of(
                            "type", "error",
                            "payload", getUnauthorizedErrorWithChallenge()
                    ))));
        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }
    }


    protected void handleStartConnection(WebSocketSession session, Map<String, Object> request) {

        Map<String, Object> payload = (Map<String, Object>)request.get("payload");
        final Object opId = request.get("id");

        // Check authenticated
        if (!isAuthenticated()) {
            try {
                session.close(CloseStatus.SERVICE_RESTARTED);
            }
            catch(IOException e) {
                // continue
            }
            return;
        }

        String query = Objects.toString(payload.get("query"));
        ExecutionResult executionResult = graphQL.execute(ExecutionInput.newExecutionInput()
                .query(query)
                .operationName((String) payload.get("operationName"))
                .variables(GraphQLHelper.getVariables(payload, objectMapper))
                .build());

        // If error: send error then disconnect
        if (CollectionUtils.isNotEmpty(executionResult.getErrors())) {
            sendResponse(session,
                         ImmutableMap.of(
                                "id", opId,
                                "type", "error",
                                "payload", GraphQLHelper.processExecutionResult(executionResult))
                );
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
                sendResponse(session, ImmutableMap.of(
                                    "id", opId,
                                    "type", "data",
                                    "payload", GraphQLHelper.processExecutionResult(result))
                );

                if (subscriptionRef.get() != null) subscriptionRef.get().request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("GraphQL subscription error", throwable);
                sendResponse(session,
                             ImmutableMap.of(
                                    "id", opId,
                                    "type", "error",
                                    "payload", GraphQLHelper.processError(throwable))
                );
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

    protected boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated());
    }

    protected void sendResponse(WebSocketSession session, Object value)  {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(value)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected Map<String, Object> getUnauthorizedErrorWithChallenge() {
        AuthDataVO challenge = authService.createNewChallenge();
        return ImmutableMap.of("message", getUnauthorizedErrorString(),
                               "challenge", challenge);
    }

    protected String getUnauthorizedErrorString() {
        return GraphQLHelper.toJsonErrorString(ErrorCodes.UNAUTHORIZED, "Authentication required");
    }
}
