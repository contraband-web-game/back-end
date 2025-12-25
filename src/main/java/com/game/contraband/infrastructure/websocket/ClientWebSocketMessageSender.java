package com.game.contraband.infrastructure.websocket;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatKickedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatLeftPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatMessageMaskedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatMessagePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatWelcomePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ExceptionMessagePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.FixedInspectorIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.FixedSmugglerIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.InspectorApprovalStatePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RegisteredInspectorIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RegisteredSmugglerIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RoomDirectoryEntryPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RoomDirectoryUpdatedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.SelectionTimerPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.SmugglerApprovalStatePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.StartGamePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.WebSocketEmptyPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketOutboundMessage;
import com.game.contraband.infrastructure.websocket.message.WebSocketOutboundMessageType;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Sinks.Many;

public class ClientWebSocketMessageSender {

    private final AtomicReference<Many<WebSocketOutboundMessage>> sinkHolder;

    public ClientWebSocketMessageSender() {
        this(null);
    }

    public ClientWebSocketMessageSender(Many<WebSocketOutboundMessage> sink) {
        this.sinkHolder = new AtomicReference<>(sink);
    }

    public void attachSink(Many<WebSocketOutboundMessage> sink) {
        sinkHolder.set(sink);
    }

    public void detachSink(Many<WebSocketOutboundMessage> sink) {
        sinkHolder.compareAndSet(sink, null);
    }

    public void sendExceptionMessage(ExceptionCode code, String exceptionMessage) {
        ExceptionCode resolvedCode = code == null ? ExceptionCode.UNKNOWN_ERROR : code;
        ExceptionMessagePayload payload = new ExceptionMessagePayload(resolvedCode, exceptionMessage);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.EXCEPTION_MESSAGE,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendRoomDirectoryUpdated(List<RoomDirectorySnapshot> rooms, int totalCount) {
        List<RoomDirectoryEntryPayload> entries = rooms.stream()
                                                       .map(room -> new RoomDirectoryEntryPayload(
                                                               room.roomId(),
                                                               String.valueOf(room.roomId()),
                                                               room.lobbyName(),
                                                               room.maxPlayerCount(),
                                                               room.currentPlayerCount(),
                                                               room.entityId(),
                                                               room.gameStarted()
                                                       ))
                                                       .toList();
        RoomDirectoryUpdatedPayload payload = new RoomDirectoryUpdatedPayload(entries, totalCount);
        WebSocketOutboundMessage message = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.ROOM_DIRECTORY_UPDATED,
                payload
        );

        emit(message);
    }

    public void sendChatWelcome(String playerName) {
        WebSocketOutboundMessage payload = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.CHAT_WELCOME,
                new ChatWelcomePayload(playerName)
        );

        emit(payload);
    }

    public void sendChatMessage(ChatMessage chatMessage) {
        ChatMessagePayload payload = new ChatMessagePayload(
                chatMessage.id(),
                chatMessage.writerId(),
                chatMessage.writerName(),
                chatMessage.message(),
                chatMessage.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        emit(new WebSocketOutboundMessage(WebSocketOutboundMessageType.LOBBY_CHAT_MESSAGE, payload));
    }

    public void sendChatLeft(String playerName) {
        WebSocketOutboundMessage payload = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.CHAT_LEFT,
                new ChatLeftPayload(playerName)
        );

        emit(payload);
    }

    public void sendChatKicked(String playerName) {
        WebSocketOutboundMessage payload = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.CHAT_KICKED,
                new ChatKickedPayload(playerName)
        );

        emit(payload);
    }

    public void sendMaskedChatMessage(Long messageId, String chatEvent) {
        WebSocketOutboundMessage payload = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.CHAT_MESSAGE_MASKED,
                new ChatMessageMaskedPayload(messageId, chatEvent)
        );

        emit(payload);
    }

    public void sendSmugglerTeamChatMessage(ChatMessage chatMessage) {
        ChatMessagePayload payload = new ChatMessagePayload(
                chatMessage.id(),
                chatMessage.writerId(),
                chatMessage.writerName(),
                chatMessage.message(),
                chatMessage.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        emit(new WebSocketOutboundMessage(WebSocketOutboundMessageType.SMUGGLER_TEAM_CHAT_MESSAGE, payload));
    }

    public void sendInspectorTeamChatMessage(ChatMessage chatMessage) {
        ChatMessagePayload payload = new ChatMessagePayload(
                chatMessage.id(),
                chatMessage.writerId(),
                chatMessage.writerName(),
                chatMessage.message(),
                chatMessage.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        emit(new WebSocketOutboundMessage(WebSocketOutboundMessageType.INSPECTOR_TEAM_CHAT_MESSAGE, payload));
    }

    public void sendRoundChatMessage(ChatMessage chatMessage) {
        ChatMessagePayload payload = new ChatMessagePayload(
                chatMessage.id(),
                chatMessage.writerId(),
                chatMessage.writerName(),
                chatMessage.message(),
                chatMessage.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        emit(new WebSocketOutboundMessage(WebSocketOutboundMessageType.ROUND_CHAT_MESSAGE, payload));
    }

    public void sendStartGame(Long playerId, List<GameStartPlayer> allPlayers) {
        StartGamePayload payload = new StartGamePayload(playerId, allPlayers);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.START_GAME,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendSelectionTimer(int round, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) {
        SelectionTimerPayload payload = new SelectionTimerPayload(round, eventAtMillis, durationMillis, serverNowMillis, endAtMillis);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.ROUND_SELECTION_TIMER,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendRegisteredSmugglerId(Long playerId) {
        RegisteredSmugglerIdPayload payload = new RegisteredSmugglerIdPayload(playerId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.REGISTERED_SMUGGLER_ID,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendFixedSmugglerId(Long playerId) {
        FixedSmugglerIdPayload payload = new FixedSmugglerIdPayload(playerId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.FIXED_SMUGGLER_ID,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendFixedSmugglerIdForInspector() {
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.FIXED_SMUGGLER_ID_FOR_INSPECTOR,
                WebSocketEmptyPayload.INSTANCE
        );

        emit(webSocketOutboundMessage);
    }

    public void sendRegisteredInspectorId(Long playerId) {
        RegisteredInspectorIdPayload payload = new RegisteredInspectorIdPayload(playerId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.REGISTERED_INSPECTOR_ID,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendFixedInspectorId(Long playerId) {
        FixedInspectorIdPayload payload = new FixedInspectorIdPayload(playerId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.FIXED_INSPECTOR_ID,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendFixedInspectorIdForSmuggler() {
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.FIXED_INSPECTOR_ID_FOR_SMUGGLER,
                WebSocketEmptyPayload.INSTANCE
        );

        emit(webSocketOutboundMessage);
    }

    public void sendSmugglerApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) {
        SmugglerApprovalStatePayload payload = new SmugglerApprovalStatePayload(
                candidateId,
                approverIds.stream()
                           .toList(),
                fixed
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.SMUGGLER_APPROVAL_STATE,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendInspectorApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) {
        InspectorApprovalStatePayload payload = new InspectorApprovalStatePayload(
                candidateId,
                approverIds.stream().toList(),
                fixed
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.INSPECTOR_APPROVAL_STATE,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendWebSocketPing() {
        emit(WebSocketOutboundMessage.PING_MESSAGE);
    }

    public void requestSessionReconnect() {
        emit(WebSocketOutboundMessage.RECONNECT_MESSAGE);
    }

    private void emit(WebSocketOutboundMessage payload) {
        Many<WebSocketOutboundMessage> sink = sinkHolder.get();

        if (sink != null) {
            sink.tryEmitNext(payload);
        }
    }
}
