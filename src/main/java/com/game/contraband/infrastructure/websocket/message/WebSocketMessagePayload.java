package com.game.contraband.infrastructure.websocket.message;

import com.fasterxml.jackson.annotation.JsonValue;
import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
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

    record RegisteredSmugglerIdPayload(Long playerId) implements WebSocketMessagePayload { }

    record FixedSmugglerIdPayload(Long playerId) implements WebSocketMessagePayload { }

    record RegisteredInspectorIdPayload(Long playerId) implements WebSocketMessagePayload { }

    record FixedInspectorIdPayload(Long playerId) implements WebSocketMessagePayload { }

    record SmugglerApprovalStatePayload(Long candidateId, List<Long> approverIds, boolean fixed) implements WebSocketMessagePayload { }

    record InspectorApprovalStatePayload(Long candidateId, List<Long> approverIds, boolean fixed) implements WebSocketMessagePayload { }

    record StartNewRoundPayload(int currentRound, Long smugglerId, Long inspectorId, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) implements WebSocketMessagePayload { }

    record FinishedRoundPayload(Long smugglerId, int smugglerAmount, Long inspectorId, int inspectorAmount, RoundOutcomeType outcomeType) implements WebSocketMessagePayload { }

    record FinishedGamePayload(GameWinnerType gameWinnerType, int smugglerTotalBalance, int inspectorTotalBalance) implements WebSocketMessagePayload { }

    record TransferFailedPayload(TransferFailureReason reason, String message) implements WebSocketMessagePayload { }

    record DecidedPassPayload(Long inspectorId) implements WebSocketMessagePayload { }

    record DecidedInspectionPayload(Long inspectorId, int amount) implements WebSocketMessagePayload { }

    record DecidedSmugglerAmountForSmugglerTeamPayload(Long smugglerId, int amount) implements WebSocketMessagePayload { }

    record TransferPayload(Long senderId, Long targetId, int senderBalance, int targetBalance, int amount) implements WebSocketMessagePayload { }
}
