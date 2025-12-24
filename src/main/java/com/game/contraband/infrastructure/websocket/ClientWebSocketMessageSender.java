package com.game.contraband.infrastructure.websocket;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ExceptionMessagePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RoomDirectoryEntryPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RoomDirectoryUpdatedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketOutboundMessage;
import com.game.contraband.infrastructure.websocket.message.WebSocketOutboundMessageType;
import java.util.List;
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
