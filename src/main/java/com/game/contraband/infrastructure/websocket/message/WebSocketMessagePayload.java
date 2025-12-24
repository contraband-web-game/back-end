package com.game.contraband.infrastructure.websocket.message;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public interface WebSocketMessagePayload {

    enum WebSocketEmptyPayload implements WebSocketMessagePayload {
        INSTANCE;

        @JsonValue
        public Object value() {
            return null;
        }
    }

    record ExceptionMessagePayload(ExceptionCode code, String exceptionMessage) implements WebSocketMessagePayload { }

    record RoomDirectoryEntryPayload(Long roomId, String roomIdString, String lobbyName, int maxPlayerCount, int currentPlayerCount, String entityId, boolean gameStarted) implements WebSocketMessagePayload { }

    record RoomDirectoryUpdatedPayload(List<RoomDirectoryEntryPayload> rooms, int totalCount) implements WebSocketMessagePayload { }
}
