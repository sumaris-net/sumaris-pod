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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class SubscriptionWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private static final List<String> GRAPHQL_WS_PROTOCOLS = ImmutableList.of(
        "graphql-transport-ws", // The new protocols
        "graphql-ws" // The deprecated protocol
    );

    public interface GqlTypes {
        String GQL_CONNECTION_INIT = "connection_init";
        String GQL_CONNECTION_ACK = "connection_ack";
        String GQL_CONNECTION_ERROR = "connection_error";
        String GQL_CONNECTION_PING = "ping";
        String GQL_CONNECTION_PONG = "pong";
        String GQL_CONNECTION_TERMINATE = "connection_terminate";
        String GQL_ERROR = "error";
        String GQL_SUBSCRIBE = "subscribe";
        String GQL_COMPLETE = "complete";
        String GQL_NEXT = "next";

        @Deprecated
        String GQL_CONNECTION_KEEP_ALIVE = "ka";
        @Deprecated
        String GQL_START = "start";
        @Deprecated
        String GQL_STOP = "stop";
        @Deprecated
        String GQL_DATA = "data";
    }

    private final boolean debug;

    private final Map<String, Disposable> subscriptions = Maps.newConcurrentMap();
    private final AtomicReference<ScheduledFuture<?>> pingTask = new AtomicReference<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final GraphQL graphQL;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final TaskScheduler taskScheduler;

    private final AtomicReference<UsernamePasswordAuthenticationToken> authentication = new AtomicReference<>();

    @Value("${graphql.ws.ping.intervalMillis:10000}")
    private int pingInterval = 10000;

    @Autowired
    public SubscriptionWebSocketHandler(ConfigurationService configuration,
                                        GraphQL webSocketGraphQL,
                                        AuthService authService,
                                        ObjectMapper objectMapper,
                                        Optional<TaskScheduler> taskScheduler) {
        this.graphQL = webSocketGraphQL;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.taskScheduler = taskScheduler.get();
        this.debug = log.isDebugEnabled();

        // When configuration not ready yet (e.g. APp try to connect BEFORE pod is really started)
        if (!configuration.isReady()) {
            // listen config ready event
            configuration.addListener(new ConfigurationEventListener() {
                @Override
                public void onReady(ConfigurationReadyEvent event) {
                    configuration.removeListener(this);
                    SubscriptionWebSocketHandler.this.ready.set(true);
                }
            });
        }
        else {
            this.ready.set(true);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (taskScheduler != null) {
            this.pingTask.compareAndSet(null,
                taskScheduler.scheduleWithFixedDelay(
                    () -> this.sendPing(session),
                    Math.max(pingInterval, 5000))); // Should be >= 5 sec
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        // Closing session's subscriptions
        disposeAllSubscriptions();

        // Close keep alive task
        cancelPingTask();

        // logout
        logout();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

       Map<String, Object> request;
        try {
            request = objectMapper.readValue(message.asBytes(), Map.class);
            if (debug) log.debug(I18n.t("sumaris.server.subscription.getRequest", request));

            String type = Objects.toString(request.get("type"), GqlTypes.GQL_START);
            switch (type) {
                // graphql-transport-ws
                case GqlTypes.GQL_CONNECTION_INIT:
                    handleInitConnection(session, request);
                    break;
                case GqlTypes.GQL_SUBSCRIBE:
                    handleSubscribeRequest(session, request);
                    break;
                case GqlTypes.GQL_COMPLETE:
                    handleCompleteRequest(session, request);
                    break;
                case GqlTypes.GQL_CONNECTION_PING:
                    log.debug(I18n.t("sumaris.server.info.subscription.cancelPingTask", type));
                    cancelPingTask();
                    break;
                case GqlTypes.GQL_CONNECTION_PONG:
                    break;
                case GqlTypes.GQL_CONNECTION_TERMINATE:
                    session.close();
                    disposeAllSubscriptions();
                    break;

                // Deprecated subscriptions-transport-ws (for Pod v1)
                case GqlTypes.GQL_CONNECTION_KEEP_ALIVE:
                    log.debug(I18n.t("sumaris.server.info.subscription.cancelPingTask", type));
                    cancelPingTask();
                    break;
                case GqlTypes.GQL_START:
                    handleSubscribeRequest(session, request, GqlTypes.GQL_DATA);
                    break;
                case GqlTypes.GQL_STOP:
                    handleStopRequest(session, request);
                    break;

                default:
                    log.error(I18n.t("sumaris.server.error.subscription.badRequest", "Unknown message type :" + type));
            }
        } catch (Exception e) {
            log.error(I18n.t("sumaris.server.error.subscription.badRequest", e.getMessage()));
            fatalError(session, e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        fatalError(session, exception);
    }

    @Override
    public List<String> getSubProtocols() {
        return GRAPHQL_WS_PROTOCOLS;
    }

    /* -- protected methods -- */

    protected void handleInitConnection(WebSocketSession session, Map<String, Object> request) {
        // When not ready, force to stop the security chain
        if (!this.ready.get()) {
            // Get user locale, from the session headers
            Locale locale = Beans.getStream(session.getHandshakeHeaders().getAcceptLanguageAsLocales())
                .findFirst()
                .orElse(I18n.getDefaultLocale());
            sendResponse(session,
                ImmutableMap.of(
                    "type", GqlTypes.GQL_CONNECTION_ERROR,
                    "payload", Collections.singletonMap("message", I18n.l(locale, "sumaris.error.starting"))
                ));
            throw new AuthenticationServiceException(I18n.l(locale, "sumaris.error.starting"));
        }

        Map<String, Object> payload = (Map<String, Object>) request.get("payload");
        String token = MapUtils.getString(payload, "authToken");

        // Has token: try to authenticate
        if (StringUtils.isNotBlank(token)) {

            // try to authenticate
            try {
                UserDetails authUser = authService.authenticateByToken(token);

                // If success: store authentication in the security context
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authUser.getUsername(), token, authUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                this.authentication.set(authentication);

                // Send an ack
                sendResponse(session,
                    ImmutableMap.of(
                        "type", GqlTypes.GQL_CONNECTION_ACK
                    ));
                return;
            }
            catch(AuthenticationException e) {
                log.warn("WebSocket session {} ({}) cannot authenticate with token '{}'",
                    session.getId(),
                    session.getRemoteAddress(),
                    e.getMessage());
                // Continue
            }
        }

        // Not auth: send a new challenge
        sendResponse(session,
            ImmutableMap.of(
                "type", GqlTypes.GQL_ERROR,
                "payload", getUnauthorizedErrorWithChallenge()
            ));
    }

    protected void handleSubscribeRequest(WebSocketSession session, Map<String, Object> request) {
        handleSubscribeRequest(session, request, GqlTypes.GQL_NEXT);
    }
    protected void handleSubscribeRequest(WebSocketSession session, Map<String, Object> request, String nextType) {

        Map<String, Object> payload = (Map<String, Object>)request.get("payload");
        final String id = request.get("id").toString();

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
        ExecutionResult result = graphQL.execute(ExecutionInput.newExecutionInput()
            .query(query)
            .operationName((String) payload.get("operationName"))
            .variables(GraphQLHelper.getVariables(payload, objectMapper))
            .build());

        // If error: send error then disconnect
        if (CollectionUtils.isNotEmpty(result.getErrors())) {
            sendResponse(session,
                ImmutableMap.of(
                    "id", id,
                    "type", GqlTypes.GQL_ERROR,
                    "payload", GraphQLHelper.processExecutionResult(result))
            );
            return;
        }

        // Subscription
        if (result.getData() instanceof Publisher) {
            handleSubscription(session, id, result, nextType);
        }
        // Query or mutation
        else {
            onNext(session, id, result, nextType);
            onComplete(session, id);
        }

    }

    protected void handleCompleteRequest(WebSocketSession session, Map<String, Object> request) {
        final String opeId = request.get("id").toString();

        // Closing the subscription
        disposeSubscription(opeId);
    }

    protected void handleSubscription(WebSocketSession session, String id, ExecutionResult executionResult) {
        handleSubscription(session, id, executionResult, GqlTypes.GQL_NEXT);
    }
    protected void handleSubscription(WebSocketSession session, String id, ExecutionResult executionResult, String nextType) {
        Publisher<ExecutionResult> events = executionResult.getData();

        Disposable subscription = Flux.from(events).subscribe(
            result -> onNext(session, id, result, nextType),
            error -> onError(session, id, error),
            () -> onComplete(session, id)
        );
        registerSubscription(id, subscription);
    }

    @Deprecated
    protected void handleDataSubscription(WebSocketSession session, String id, ExecutionResult executionResult) {
        Publisher<ExecutionResult> events = executionResult.getData();

        Disposable subscription = Flux.from(events).subscribe(
            result -> onData(session, id, result),
            error -> onError(session, id, error),
            () -> onComplete(session, id)
        );
        registerSubscription(id, subscription);
    }

    @Deprecated
    protected void handleStopRequest(WebSocketSession session, Map<String, Object> request) {
        handleCompleteRequest(session, request);
    }

    @Deprecated
    protected void handleStartRequest(WebSocketSession session, Map<String, Object> request) {
        handleSubscribeRequest(session, request, GqlTypes.GQL_DATA);
    }

    protected void onNext(WebSocketSession session,  String id, ExecutionResult result) {
        onNext(session, id, result, GqlTypes.GQL_NEXT);
    }

    protected void onNext(WebSocketSession session,  String id, ExecutionResult result, String type) {

        Object response = ImmutableMap.of(
            "id", id,
            "type", type,
            "payload", GraphQLHelper.processExecutionResult(result)
        );

        sendResponse(session, response);

        if (debug) log.debug(I18n.t("sumaris.server.subscription.sentRequest", response));
    }

    protected void onError(WebSocketSession session, String id, Throwable throwable) {
        log.warn("GraphQL subscription error", throwable);
        sendResponse(session,
            ImmutableMap.of(
                "id", id,
                "type", GqlTypes.GQL_ERROR,
                "payload", GraphQLHelper.processError(throwable))
        );
    }

    protected void onComplete(WebSocketSession session, String id) {
        sendResponse(session,
            ImmutableMap.of(
                "id", id,
                "type", GqlTypes.GQL_COMPLETE
        ));
    }

    @Deprecated
    protected void onData(WebSocketSession session,  String id, ExecutionResult result) {
        sendResponse(session, ImmutableMap.of(
            "id", id,
            "type", GqlTypes.GQL_DATA,
            "payload", GraphQLHelper.processExecutionResult(result))
        );
    }

    protected boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Thread is auth: OK
        if (auth != null && auth.isAuthenticated()) {
            return true;
        }

        // Check if session hold authentication
        auth = this.authentication.get();
        if (auth != null && auth.isAuthenticated()) {
            // Store in security context
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        }

        // No authentication (nor in the thread, not in the session)
        return false;
    }

    protected void sendResponse(WebSocketSession session, Object value)  {
        sendResponse(session, value, (e) -> fatalError(session, e));
    }

    protected void sendResponse(WebSocketSession session, Object value, Consumer<Exception> errorHandler)  {
        // /!\ Many threads can use the same session, so need to use 'synchronized' on session
        // This avoid to have the exception "The remote endpoint was in state [TEXT_PARTIAL_WRITING] which is an invalid state for called method"
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(value)));
            } catch (IllegalStateException | IOException e) {
                errorHandler.accept(e);
            }

        }
    }

    protected Map<String, Object> getUnauthorizedErrorWithChallenge() {
        AuthTokenVO challenge = authService.createNewChallenge();
        String errorMessage = getUnauthorizedErrorString();
        return ImmutableMap.of("message", errorMessage,
            "challenge", challenge);
    }

    protected String getUnauthorizedErrorString() {
        return GraphQLHelper.toJsonErrorString(ErrorCodes.UNAUTHORIZED, "Authentication required");
    }

    /**
     * Closing all session's subscriptions
     */
    protected void disposeAllSubscriptions() {
        synchronized (subscriptions) {
            subscriptions.values().stream()
                .filter(d -> !d.isDisposed())
                .forEach(Disposable::dispose);
            subscriptions.clear();
        }
    }

    protected void disposeSubscription(String opeId) {

        // Stop subscription
        synchronized (subscriptions) {
            Disposable subscription = subscriptions.remove(opeId);
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }

    protected void registerSubscription(String opeId, Disposable subscription) {

        // Add to subscriptions
        synchronized (subscriptions) {
            subscriptions.put(opeId, subscription);
        }
    }

    private void fatalError(WebSocketSession session, Throwable exception) {
        try {
            if (!ready.get()) {
                session.close(CloseStatus.SERVICE_RESTARTED);
                log.warn("WebSocket session {} ({}) closed: handler not started", session.getId(), session.getRemoteAddress());
            }
            else {
                session.close(exception instanceof IOException
                    ? CloseStatus.SESSION_NOT_RELIABLE
                    : CloseStatus.SERVER_ERROR);
                log.warn("WebSocket session {} ({}) closed due to an exception", session.getId(), session.getRemoteAddress(), exception);
            }
        } catch (Exception suppressed) {
            exception.addSuppressed(suppressed);
            log.warn("WebSocket session {} ({}) closed due to an exception", session.getId(), session.getRemoteAddress(), exception);
        }
        disposeAllSubscriptions();
    }

    private void sendPing(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            sendResponse(session,
                ImmutableMap.of(
                    "type", GqlTypes.GQL_CONNECTION_PING
                ),
                // Error handler
                (e) -> {
                    if (e instanceof IllegalStateException) return; // Silent (continue without close the session)
                    fatalError(session, e);
                });
        }
    }

    protected void cancelPingTask() {
        if (taskScheduler != null) {
            this.pingTask.getAndUpdate(task -> {
                if (task != null) {
                    task.cancel(false);
                }
                return null;
            });
        }
    }

    protected void logout() {
        this.authentication.set(null);
        SecurityContextHolder.clearContext();
    }
}
