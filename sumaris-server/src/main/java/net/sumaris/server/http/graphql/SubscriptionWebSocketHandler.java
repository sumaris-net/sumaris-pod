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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationEventListener;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.Beans;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.util.security.AuthTokenVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class SubscriptionWebSocketHandler extends TextWebSocketHandler {

    private final boolean debug;

    private List<WebSocketSession> sessions = new CopyOnWriteArrayList();

    private Map<String, List<Subscription>> subscriptionsBySessionId = Maps.newConcurrentMap();

    private boolean ready = false;

    @Resource(name = "webSocketGraphQL")
    private GraphQL graphQL;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    public SubscriptionWebSocketHandler(ConfigurationService configuration) {
        this.debug = log.isDebugEnabled();

        // When configuration not ready yet (e.g. APp try to connect BEFORE pod is really started)
        if (!configuration.isReady()) {
            // listen config ready event
            configuration.addListener(new ConfigurationEventListener() {
                @Override
                public void onReady(ConfigurationReadyEvent event) {
                    configuration.removeListener(this);
                    SubscriptionWebSocketHandler.this.ready = true;
                }
            });
        }
        else {
            ready = true;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // keep all sessions (for broadcast)
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);

        // Closing session's subscriptions
        closeSubscriptions(session.getId());
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
            // Closing session's subscriptions
            closeSubscriptions(session.getId());
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
        // When not ready, force to stop the security chain
        if (!this.ready) {
            // Get user locale, from the session headers
            Locale locale = Beans.getStream(session.getHandshakeHeaders().getAcceptLanguageAsLocales())
                .findFirst()
                .orElse(I18n.getDefaultLocale());
            throw new AuthenticationServiceException(I18n.l(locale, "sumaris.error.starting"));
        }

        Map<String, Object> payload = (Map<String, Object>) request.get("payload");
        String token = MapUtils.getString(payload, "authToken");

        // Has token: try to authenticate
        if (StringUtils.isNotBlank(token)) {

            // try to authenticate
            try {
                UserDetails authUser = authService.authenticateByToken(token);
                // If success
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authUser.getUsername(), token, authUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return; // OK
            }
            catch(AuthenticationException e) {
                log.warn("Unable to authenticate websocket session, using token: " + e.getMessage());
                // Continue
            }
        }

        // Not auth: send a new challenge
        sendResponse(session,
            ImmutableMap.of(
                "type", "error",
                "payload", getUnauthorizedErrorWithChallenge()
            ));
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

        final String sessionId = session.getId();
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
                addSubscription(sessionId, subscription);
            }

            @Override
            public void onNext(ExecutionResult result) {
                sendResponse(session, ImmutableMap.of(
                                    "id", opId,
                                    "type", "data",
                                    "payload", GraphQLHelper.processExecutionResult(result))
                );
                requestSubscription(sessionId, 1);
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
        AuthTokenVO challenge = authService.createNewChallenge();
        return ImmutableMap.of("message", getUnauthorizedErrorString(),
                               "challenge", challenge);
    }

    protected String getUnauthorizedErrorString() {
        return GraphQLHelper.toJsonErrorString(ErrorCodes.UNAUTHORIZED, "Authentication required");
    }

    protected void closeSubscriptions(String sessionId) {
        // Closing session's subscriptions
        List<Subscription> subscriptions = subscriptionsBySessionId.get(sessionId);
        if (subscriptions != null) {
            subscriptions.forEach(Subscription::cancel);
        }
    }

    protected void addSubscription(String sessionId, Subscription subscription) {
        // Closing session's subscriptions
        List<Subscription> subscriptions = subscriptionsBySessionId.computeIfAbsent(sessionId, k -> Lists.newCopyOnWriteArrayList());
        subscriptions.add(subscription);
    }

    protected void requestSubscription(String sessionId, int l) {
        // Closing session's subscriptions
        List<Subscription> subscriptions = subscriptionsBySessionId.get(sessionId);
        if (subscriptions != null) {
            subscriptions.forEach(s -> s.request(l));
        }
    }
}
