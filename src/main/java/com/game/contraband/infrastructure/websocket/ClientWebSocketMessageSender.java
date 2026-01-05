package com.game.contraband.infrastructure.websocket;

import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatKickedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatLeftPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatMessageMaskedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatMessagePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ChatWelcomePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.CreateLobbyPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.DecidedInspectionPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.DecidedPassPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.DecidedSmugglerAmountForSmugglerTeamPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ExceptionMessagePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.FinishedGamePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.FinishedRoundPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.FixedInspectorIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.FixedSmugglerIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.InspectorApprovalStatePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.JoinedLobbyPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.OtherPlayerJoinedLobbyPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.OtherPlayerKickedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.OtherPlayerLeftLobbyPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RegisteredInspectorIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RegisteredSmugglerIdPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RoomDirectoryEntryPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.RoomDirectoryUpdatedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.SelectionTimerPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.SmugglerApprovalStatePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.StartGamePayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.StartNewRoundPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ToggledReadyPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.ToggledTeamPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.TransferFailedPayload;
import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.TransferPayload;
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

    public void sendExceptionMessage(ExceptionCode code) {
        ExceptionCode resolvedCode = code == null ? ExceptionCode.UNKNOWN_ERROR : code;
        ExceptionMessagePayload payload = new ExceptionMessagePayload(resolvedCode);
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

    public void sendStartNewRound(
            int currentRound,
            Long smugglerId,
            Long inspectorId,
            long eventAtMillis,
            long durationMillis,
            long serverNowMillis,
            long endAtMillis
    ) {
        StartNewRoundPayload payload = new StartNewRoundPayload(currentRound, smugglerId, inspectorId, eventAtMillis, durationMillis, serverNowMillis, endAtMillis);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.START_NEW_ROUND,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendFinishedRound(
            Long smugglerId,
            int smugglerAmount,
            Long inspectorId,
            int inspectorAmount,
            RoundOutcomeType outcomeType
    ) {
        FinishedRoundPayload payload = new FinishedRoundPayload(
                smugglerId,
                smugglerAmount,
                inspectorId,
                inspectorAmount,
                outcomeType
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.FINISHED_ROUND,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendFinishedGame(GameWinnerType gameWinnerType, int smugglerTotalBalance, int inspectorTotalBalance) {
        FinishedGamePayload payload = new FinishedGamePayload(
                gameWinnerType,
                smugglerTotalBalance,
                inspectorTotalBalance
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.FINISHED_GAME,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendTransferFailed(TransferFailureReason reason, String message) {
        TransferFailedPayload payload = new TransferFailedPayload(reason, message);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.TRANSFER_FAILED,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendDecideInspectorBehaviorForSmugglerTeam() {
        emit(WebSocketOutboundMessage.DECIDED_INSPECTOR_BEHAVIOR_FOR_SMUGGLER_TEAM);
    }

    public void sendDecidedPass(Long inspectorId) {
        DecidedPassPayload payload = new DecidedPassPayload(inspectorId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.DECIDED_PASS,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendDecidedInspection(Long inspectorId, int amount) {
        DecidedInspectionPayload payload = new DecidedInspectionPayload(inspectorId, amount);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.DECIDED_INSPECTION,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendDecideSmugglerAmountForSmugglerTeam(Long smugglerId, int amount) {
        DecidedSmugglerAmountForSmugglerTeamPayload payload = new DecidedSmugglerAmountForSmugglerTeamPayload(
                smugglerId,
                amount
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.DECIDED_SMUGGLER_AMOUNT_FOR_SMUGGLER_TEAM,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendDecideSmugglerAmountForInspectorTeam() {
        emit(WebSocketOutboundMessage.DECIDED_SMUGGLER_AMOUNT_FOR_INSPECTOR_TEAM);
    }

    public void sendTransfer(Long senderId, Long targetId, int senderBalance, int targetBalance, int amount) {
        TransferPayload payload = new TransferPayload(senderId, targetId, senderBalance, targetBalance, amount);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.TRANSFER,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendCreateLobby(int maxPlayerCount, String lobbyName, TeamRole teamRole) {
        CreateLobbyPayload payload = new CreateLobbyPayload(maxPlayerCount, lobbyName, teamRole);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.CREATE_LOBBY,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendCreatedLobby(
            Long roomId,
            Long hostId,
            int maxPlayerCount,
            int currentPlayerCount,
            String lobbyName,
            List<LobbyParticipant> lobbyParticipants
    ) {
        JoinedLobbyPayload payload = new JoinedLobbyPayload(
                roomId,
                hostId,
                maxPlayerCount,
                currentPlayerCount,
                lobbyName,
                lobbyParticipants
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.CREATED_LOBBY,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendOtherPlayerJoinedLobby(Long joinerId, String joinerName, TeamRole teamRole, int currentPlayerCount) {
        OtherPlayerJoinedLobbyPayload payload = new OtherPlayerJoinedLobbyPayload(
                joinerId,
                joinerName,
                teamRole,
                currentPlayerCount
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.OTHER_PLAYER_JOINED_LOBBY,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendJoinedLobby(Long roomId, Long hostId, int maxPlayerCount, int currentPlayerCount, String lobbyName, List<LobbyParticipant> lobbyParticipants) {
        JoinedLobbyPayload payload = new JoinedLobbyPayload(
                roomId,
                hostId,
                maxPlayerCount,
                currentPlayerCount,
                lobbyName,
                lobbyParticipants
        );
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.JOINED_LOBBY,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendToggledReady(Long playerId, boolean toggleReadyState) {
        ToggledReadyPayload payload = new ToggledReadyPayload(playerId, toggleReadyState);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.TOGGLED_READY,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendToggledTeam(Long playerId, String playerName, TeamRole teamRole) {
        ToggledTeamPayload payload = new ToggledTeamPayload(playerId, playerName, teamRole);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.TOGGLED_TEAM,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendLeftLobby() {
        emit(WebSocketOutboundMessage.LEFT_LOBBY_MESSAGE);
    }

    public void sendOtherPlayerLeftLobby(Long playerId) {
        OtherPlayerLeftLobbyPayload payload = new OtherPlayerLeftLobbyPayload(playerId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.OTHER_PLAYER_LEFT_LOBBY,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendKickedLobby() {
        emit(WebSocketOutboundMessage.KICKED_LOBBY_MESSAGE);
    }

    public void sendOtherPlayerKicked(Long playerId) {
        OtherPlayerKickedPayload payload = new OtherPlayerKickedPayload(playerId);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.OTHER_PLAYER_KICKED,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendHostDeletedLobby() {
        emit(WebSocketOutboundMessage.HOST_DELETED_LOBBY);
    }

    public void sendLobbyDeleted() {
        emit(WebSocketOutboundMessage.DELETED_LOBBY);
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
