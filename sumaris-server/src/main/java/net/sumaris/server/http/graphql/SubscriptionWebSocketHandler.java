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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class SubscriptionWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private static final List<String> GRAPHQL_WS = Collections.singletonList("graphql-ws");

    public interface GqlTypes {
        String GQL_CONNECTION_INIT = "connection_init";
        String GQL_CONNECTION_ACK = "connection_ack";
        String GQL_CONNECTION_ERROR = "connection_error";
        String GQL_CONNECTION_KEEP_ALIVE = "ka";

        String GQL_CONNECTION_PING = "ping";

        String GQL_CONNECTION_PONG = "pong";
        String GQL_CONNECTION_TERMINATE = "connection_terminate";
        String GQL_START = "start";
        String GQL_STOP = "stop";
        String GQL_ERROR = "error";
        String GQL_DATA = "data";
        String GQL_COMPLETE = "complete";
    }

    private final boolean debug;

    private Map<String, Disposable> subscriptions = Maps.newConcurrentMap();
    private final AtomicReference<ScheduledFuture<?>> keepAlive = new AtomicReference<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final GraphQL graphQL;
    private final ObjectMapper objectMapper;
    private final AuthService authService;
    private final TaskScheduler taskScheduler;

    @Value("${graphql.spqr.ws.keepAlive.intervalMillis:10000}")
    private int keepAliveInterval = 10000;

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
            this.keepAlive.compareAndSet(null,
                taskScheduler.scheduleWithFixedDelay(keepAliveTask(session),
                Math.max(keepAliveInterval, 5000))); // Should be >= 5 sec
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        // Closing session's subscriptions
        cancelAll();

        // Close keep alive task
        cancelKeepAliveTask();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

       Map<String, Object> request;
        try {
            request = objectMapper.readValue(message.asBytes(), Map.class);
            if (debug) log.debug(I18n.t("sumaris.server.subscription.getRequest", request));

            String type = Objects.toString(request.get("type"), "start");
            switch (type) {
                case GqlTypes.GQL_CONNECTION_INIT:
                    handleInitConnection(session, request);
                    break;
                case GqlTypes.GQL_START:
                    handleStartRequest(session, request);
                    break;
                case GqlTypes.GQL_STOP:
                    handleStopRequest(session, request);
                    break;
                case GqlTypes.GQL_CONNECTION_KEEP_ALIVE:
                case GqlTypes.GQL_CONNECTION_PING:
                    log.debug(I18n.t("sumaris.server.info.subscription.cancelKeepAliveTask", type));
                    cancelKeepAliveTask();
                    break;
                case GqlTypes.GQL_CONNECTION_PONG:
                    log.debug(I18n.t("sumaris.server.info.subscription.received", type));
                    break;
                case GqlTypes.GQL_CONNECTION_TERMINATE:
                    session.close();
                    cancelAll();
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
        return GRAPHQL_WS;
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


    protected void handleStartRequest(WebSocketSession session, Map<String, Object> request) {

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

        if (result.getData() instanceof Publisher) {
            handleSubscription(session, id, result);
        } else {
            handleQueryOrMutation(session, id, result);
        }

    }

    protected void handleStopRequest(WebSocketSession session, Map<String, Object> request) {
        final String opeId = request.get("id").toString();

        // Closing the subscription
        cancel(opeId);
    }

    protected void handleSubscription(WebSocketSession session, String id, ExecutionResult executionResult) {
        Publisher<ExecutionResult> events = executionResult.getData();

        Disposable subscription = Flux.from(events).subscribe(
            result -> onNext(session, id, result),
            error -> onError(session, id, error),
            () -> onComplete(session, id)
        );
        registerSubscription(id, subscription);
    }

    private void handleQueryOrMutation(WebSocketSession session, String id, ExecutionResult result) {
        onNext(session, id, result);
        onComplete(session, id);
    }

    protected void onNext(WebSocketSession session,  String id, ExecutionResult result) {
        sendResponse(session, ImmutableMap.of(
            "id", id,
            "type", GqlTypes.GQL_DATA,
            "payload", GraphQLHelper.processExecutionResult(result))
        );
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

    protected boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated());
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
        return ImmutableMap.of("message", getUnauthorizedErrorString(),
                               "challenge", challenge);
    }

    protected String getUnauthorizedErrorString() {
        return GraphQLHelper.toJsonErrorString(ErrorCodes.UNAUTHORIZED, "Authentication required");
    }

    protected void cancelAll() {
        // Closing session's subscriptions
        synchronized (subscriptions) {
            subscriptions.values().forEach(Disposable::dispose);
            subscriptions.clear();
        }
    }

    protected void cancel(String id) {

        // Stop subscription
        synchronized (subscriptions) {
            Disposable subscription = subscriptions.remove(id);
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }

    protected void registerSubscription(String id, Disposable subscription) {

        // Add to subscriptions
        synchronized (subscriptions) {
            subscriptions.put(id, subscription);
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
        cancelAll();
    }

    private Runnable keepAliveTask(WebSocketSession session) {
        return () -> {
            if (session != null && session.isOpen()) {
                sendResponse(session,
                    ImmutableMap.of(
                        "type", "ping" //GqlTypes.GQL_CONNECTION_KEEP_ALIVE
                    ),
                    // Error handler
                    (e) -> {
                        if (e instanceof IllegalStateException) return; // Silent (continue without close the session)
                        fatalError(session, e);
                    });
            }
        };
    }

    protected void cancelKeepAliveTask() {
        if (taskScheduler != null) {
            this.keepAlive.getAndUpdate(task -> {
                if (task != null) {
                    task.cancel(false);
                }
                return null;
            });
        }
    }
}
