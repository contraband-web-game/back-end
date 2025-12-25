package com.game.contraband.infrastructure.websocket.message;

import com.fasterxml.jackson.annotation.JsonValue;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
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

    record ChatWelcomePayload(String playerName) implements WebSocketMessagePayload { }

    record ChatMessagePayload(Long messageId, Long writerId, String writerName, String message, String createdAt) implements WebSocketMessagePayload { }

    record ChatLeftPayload(String playerName) implements WebSocketMessagePayload { }

    record ChatKickedPayload(String playerName) implements WebSocketMessagePayload { }

    record ChatMessageMaskedPayload(Long messageId, String chatEvent) implements WebSocketMessagePayload { }

    record StartGamePayload(Long playerId, List<GameStartPlayer> allPlayers) implements WebSocketMessagePayload { }

    record SelectionTimerPayload(int round, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) implements WebSocketMessagePayload { }
}
