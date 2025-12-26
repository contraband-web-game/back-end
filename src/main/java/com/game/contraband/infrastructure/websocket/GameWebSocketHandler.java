package com.game.contraband.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.FetchRoomDirectoryPage;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ReSyncClientSession;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.RequestSendChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.RequestSendRoundChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.RequestSendTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestChangeMaxPlayerCount;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestDecideInspection;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestDecidePass;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestDecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestFixInspector;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestFixSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestKickPlayer;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestLeaveLobby;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestLobbyDeletion;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestRegisterInspector;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestRegisterSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestStartGame;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestToggleReady;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestToggleTeam;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestTransferMoney;
import com.game.contraband.infrastructure.actor.client.SessionPresenceActor.SessionPongReceived;
import com.game.contraband.infrastructure.actor.client.service.ClientSessionActorManageService;
import com.game.contraband.infrastructure.websocket.message.WebSocketInboundMessageType;
import com.game.contraband.infrastructure.websocket.message.WebSocketOutboundMessage;
import com.game.contraband.infrastructure.websocket.message.WebSocketOutboundMessageType;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.pekko.actor.typed.ActorRef;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@RequiredArgsConstructor
public class GameWebSocketHandler implements WebSocketHandler {

    private static final String MESSAGE_SCHEMA_TYPE = "type";

    private final ObjectMapper objectMapper;
    private final ClientSessionActorManageService manageService;
    private final Map<Long, ClientWebSocketMessageSender> clientMessageSenders = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        WebSocketConnectionContext context = extractConnectionContext(session);
        Sinks.Many<WebSocketOutboundMessage> messageSink = createMessageSink();
        ClientWebSocketMessageSender clientWebSocketMessageSender = getOrCreateMessageSender(context, messageSink);
        CompletionStage<ActorRef<ClientSessionCommand>> clientSession = getOrCreateClientSessionActor(
                context,
                clientWebSocketMessageSender
        );
        clientSession.thenAccept(actorRef -> actorRef.tell(new ReSyncClientSession(context.getPlayerId())));
        Mono<Void> inbound = handleInboundMessages(session, context, messageSink, clientSession);
        Flux<WebSocketMessage> outbound = handleOutboundMessages(session, messageSink);

        return session.send(outbound)
                      .and(inbound)
                      .doFinally(signal -> cleanupConnection(clientWebSocketMessageSender, messageSink));
    }

    private WebSocketConnectionContext extractConnectionContext(WebSocketSession session) {
        return WebSocketConnectionContext.create(session);
    }

    private Sinks.Many<WebSocketOutboundMessage> createMessageSink() {
        return Sinks.many()
                    .unicast()
                    .onBackpressureBuffer();
    }

    private ClientWebSocketMessageSender getOrCreateMessageSender(
            WebSocketConnectionContext context,
            Sinks.Many<WebSocketOutboundMessage> sink
    ) {
        return clientMessageSenders.compute(
                context.getPlayerId(),
                (userId, sender) -> {
                    ClientWebSocketMessageSender actualSender =
                            sender != null ? sender : new ClientWebSocketMessageSender();

                    actualSender.attachSink(sink);
                    return actualSender;
                }
        );
    }

    private CompletionStage<ActorRef<ClientSessionCommand>> getOrCreateClientSessionActor(
            WebSocketConnectionContext context,
            ClientWebSocketMessageSender clientWebSocketMessageSender
    ) {
        return manageService.findClientSession(context.getPlayerId())
                            .map(ref -> (CompletionStage<ActorRef<ClientSessionCommand>>) CompletableFuture.completedFuture(ref))
                            .orElseGet(() -> manageService.createClientSessionActor(context.getPlayerId(), clientWebSocketMessageSender));
    }

    private Mono<Void> handleInboundMessages(
            WebSocketSession session,
            WebSocketConnectionContext context,
            Sinks.Many<WebSocketOutboundMessage> sink,
            CompletionStage<ActorRef<ClientSessionCommand>> clientSession
    ) {
        return session.receive()
                      .flatMap(message -> processInboundMessage(context, sink, clientSession, message))
                      .then();
    }

    private Mono<Void> processInboundMessage(
            WebSocketConnectionContext context,
            Sinks.Many<WebSocketOutboundMessage> sink,
            CompletionStage<ActorRef<ClientSessionCommand>> clientSession,
            WebSocketMessage message
    ) {
        String payload = message.getPayloadAsText();

        if (handleClientHealthPong(payload, clientSession)) {
            return Mono.empty();
        }
        if (handlePingMessage(payload, sink)) {
            return Mono.empty();
        }
        if (handleDeleteLobby(payload, clientSession, context)) {
            return Mono.empty();
        }
        if (handleLobbyInteractions(payload, clientSession, context)) {
            return Mono.empty();
        }
        if (handleContrabandGameInteractions(payload, clientSession, context)) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    private boolean handlePingMessage(
            String payload,
            Sinks.Many<WebSocketOutboundMessage> sink
    ) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path(MESSAGE_SCHEMA_TYPE).asText();

            if (WebSocketInboundMessageType.HEARTBEAT_PING.isSameType(type)) {
                sink.tryEmitNext(WebSocketOutboundMessage.withoutPayload(WebSocketOutboundMessageType.HEARTBEAT_PONG));
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean handleDeleteLobby(
            String payload,
            CompletionStage<ActorRef<ClientSessionCommand>> clientSession,
            WebSocketConnectionContext context
    ) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path(MESSAGE_SCHEMA_TYPE).asText();

            if (WebSocketInboundMessageType.DELETE_LOBBY.isSameType(type)) {
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestLobbyDeletion(context.getPlayerId())));
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean handleLobbyInteractions(
            String payload,
            CompletionStage<ActorRef<ClientSessionCommand>> clientSession,
            WebSocketConnectionContext context
    ) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path(MESSAGE_SCHEMA_TYPE).asText();

            if (WebSocketInboundMessageType.QUERY_ROOM_DIRECTORY.isSameType(type)) {
                int page = node.path("page").asInt(0);
                int size = node.path("size").asInt(20);
                clientSession.thenAccept(actorRef -> actorRef.tell(new FetchRoomDirectoryPage(page, size)));
                return true;
            }
            if (WebSocketInboundMessageType.TOGGLE_READY.isSameType(type)) {
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestToggleReady(context.getPlayerId())));
                return true;
            }
            if (WebSocketInboundMessageType.TOGGLE_TEAM.isSameType(type)) {
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestToggleTeam(context.getPlayerId())));
                return true;
            }
            if (WebSocketInboundMessageType.CHANGE_MAX_PLAYER_COUNT.isSameType(type)) {
                int maxPlayerCount = node.path("maxPlayerCount").asInt();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(
                                new RequestChangeMaxPlayerCount(maxPlayerCount, context.getPlayerId())
                        )
                );
                return true;
            }
            if (WebSocketInboundMessageType.START_GAME.isSameType(type)) {
                int totalRounds = node.path("totalRounds").asInt();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(new RequestStartGame(context.getPlayerId(), totalRounds))
                );
                return true;
            }
            if (WebSocketInboundMessageType.LEAVE_LOBBY.isSameType(type)) {
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestLeaveLobby(context.getPlayerId())));
                return true;
            }
            if (WebSocketInboundMessageType.KICK_PLAYER.isSameType(type)) {
                long targetPlayerId = node.path("targetPlayerId").asLong();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(new RequestKickPlayer(context.getPlayerId(), targetPlayerId))
                );
                return true;
            }
            if (WebSocketInboundMessageType.SEND_CHAT.isSameType(type)) {
                String message = node.path("message").asText();
                String playerName = node.path("playerName").asText();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(new RequestSendChat(context.getPlayerId(), playerName, message))
                );
                return true;
            }
            if (WebSocketInboundMessageType.SEND_ROUND_CHAT.isSameType(type)) {
                String message = node.path("message").asText();
                String playerName = node.path("playerName").asText();
                int currentRound = node.path("currentRound").asInt();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(
                                new RequestSendRoundChat(
                                        context.getPlayerId(),
                                        playerName,
                                        message,
                                        currentRound
                                )
                        )
                );
                return true;
            }
            if (WebSocketInboundMessageType.SEND_TEAM_CHAT.isSameType(type)) {
                String message = node.path("message").asText();
                String playerName = node.path("playerName").asText();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(new RequestSendTeamChat(context.getPlayerId(), playerName, message))
                );
                return true;
            }
            if (WebSocketInboundMessageType.FIX_SMUGGLER_ID.isSameType(type)) {
                long playerId = context.getPlayerId();
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestFixSmuggler(playerId)));
                return true;
            }
            if (WebSocketInboundMessageType.FIX_INSPECTOR_ID.isSameType(type)) {
                long playerId = context.getPlayerId();
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestFixInspector(playerId)));
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean handleContrabandGameInteractions(
            String payload,
            CompletionStage<ActorRef<ClientSessionCommand>> clientSession,
            WebSocketConnectionContext context
    ) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path(MESSAGE_SCHEMA_TYPE).asText();

            if (WebSocketInboundMessageType.REGISTER_SMUGGLER.isSameType(type)) {
                long smugglerId = node.path("smugglerId").asLong();
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestRegisterSmuggler(smugglerId)));
                return true;
            }
            if (WebSocketInboundMessageType.REGISTER_INSPECTOR.isSameType(type)) {
                long inspectorId = node.path("inspectorId").asLong();
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestRegisterInspector(inspectorId)));
                return true;
            }
            if (WebSocketInboundMessageType.DECIDE_SMUGGLE_AMOUNT.isSameType(type)) {
                int amount = node.path("amount").asInt();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(new RequestDecideSmuggleAmount(context.getPlayerId(), amount))
                );
                return true;
            }
            if (WebSocketInboundMessageType.DECIDE_PASS.isSameType(type)) {
                clientSession.thenAccept(actorRef -> actorRef.tell(new RequestDecidePass(context.getPlayerId())));
                return true;
            }
            if (WebSocketInboundMessageType.DECIDE_INSPECTION.isSameType(type)) {
                int amount = node.path("amount").asInt();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(new RequestDecideInspection(context.getPlayerId(), amount))
                );
                return true;
            }
            if (WebSocketInboundMessageType.TRANSFER_MONEY.isSameType(type)) {
                long targetPlayerId = node.path("targetPlayerId").asLong();
                int amount = node.path("amount").asInt();
                clientSession.thenAccept(
                        actorRef -> actorRef.tell(
                                new RequestTransferMoney(
                                        context.getPlayerId(),
                                        targetPlayerId,
                                        amount
                                )
                        )
                );
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean handleClientHealthPong(
            String payload,
            CompletionStage<ActorRef<ClientSessionCommand>> clientSession
    ) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path(MESSAGE_SCHEMA_TYPE).asText();

            if (WebSocketInboundMessageType.SESSION_HEALTH_PONG.isSameType(type)) {
                clientSession.thenAccept(actorRef -> actorRef.tell(new SessionPongReceived()));
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private Flux<WebSocketMessage> handleOutboundMessages(
            WebSocketSession session,
            Sinks.Many<WebSocketOutboundMessage> sink
    ) {
        return sink.asFlux()
                   .flatMap(payload -> serializeMessage(session, payload));
    }

    private Mono<WebSocketMessage> serializeMessage(
            WebSocketSession session,
            WebSocketOutboundMessage payload
    ) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            return Mono.just(session.textMessage(json));
        } catch (JsonProcessingException ignored) {
            return Mono.empty();
        }
    }

    private void cleanupConnection(
            ClientWebSocketMessageSender clientWebSocketMessageSender,
            Sinks.Many<WebSocketOutboundMessage> sink
    ) {
        clientWebSocketMessageSender.detachSink(sink);
        sink.tryEmitComplete();
    }

    @Getter
    @RequiredArgsConstructor
    private static class WebSocketConnectionContext {

        private final Long playerId;

        static WebSocketConnectionContext create(WebSocketSession session) {
            URI uri = session.getHandshakeInfo().getUri();
            MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri)
                                                                       .build()
                                                                       .getQueryParams();

            Long playerId = extractRequiredParam(params, "playerId");

            return new WebSocketConnectionContext(playerId);
        }

        private static Long extractRequiredParam(MultiValueMap<String, String> params, String key) {
            try {
                return Long.valueOf(Objects.requireNonNull(params.getFirst(key)));
            } catch (NullPointerException | NumberFormatException e) {
                throw new IllegalArgumentException(key + "에 해당하는 ID가 없거나 유효하지 않습니다.");
            }
        }
    }
}
