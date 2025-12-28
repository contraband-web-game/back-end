package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpyClientWebSocketMessageSender extends ClientWebSocketMessageSender {

    public ExceptionCode exceptionCode;
    public String exceptionMessage;
    public boolean pingSent;
    public boolean reconnectRequested;
    public List<RoomDirectorySnapshot> roomSnapshots = List.of();
    public Integer roomTotalCount;
    public List<GameStartPlayer> startGamePlayers = List.of();
    public boolean startGameSent;
    public SelectionTimer selectionTimer;
    public Long registeredSmugglerId;
    public Long fixedSmugglerId;
    public boolean fixedSmugglerIdForInspectorSent;
    public Long registeredInspectorId;
    public Long fixedInspectorId;
    public boolean fixedInspectorIdForSmugglerSent;
    public ApprovalState smugglerApprovalState;
    public ApprovalState inspectorApprovalState;
    public StartRound startRound;
    public FinishedRound finishedRound;
    public FinishedGame finishedGame;
    public TransferFailure transferFailure;
    public DecidedPass decidedPass;
    public DecidedInspection decidedInspection;
    public DecidedSmuggle decidedSmuggle;
    public Transfer transfer;
    public CreateLobby createLobby;
    public CreatedLobby createdLobby;
    public OtherJoined otherJoined;
    public JoinedLobby joinedLobby;
    public ToggleReady toggleReady;
    public ToggleTeam toggleTeam;
    public boolean leftLobby;
    public Long otherLeftLobby;
    public boolean kickedLobby;
    public Long otherKicked;
    public boolean hostDeletedLobby;
    public boolean lobbyDeleted;

    public static class SelectionTimer {
        public final int round;
        public final long eventAtMillis;
        public final long durationMillis;
        public final long serverNowMillis;
        public final long endAtMillis;

        public SelectionTimer(int round, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) {
            this.round = round;
            this.eventAtMillis = eventAtMillis;
            this.durationMillis = durationMillis;
            this.serverNowMillis = serverNowMillis;
            this.endAtMillis = endAtMillis;
        }
    }

    public static class ApprovalState {
        public final Long candidateId;
        public final Set<Long> approverIds;
        public final boolean fixed;

        public ApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) {
            this.candidateId = candidateId;
            this.approverIds = approverIds;
            this.fixed = fixed;
        }
    }

    public static class StartRound {
        public final int currentRound;
        public final Long smugglerId;
        public final Long inspectorId;
        public final long eventAtMillis;
        public final long durationMillis;
        public final long serverNowMillis;
        public final long endAtMillis;

        public StartRound(int currentRound, Long smugglerId, Long inspectorId, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) {
            this.currentRound = currentRound;
            this.smugglerId = smugglerId;
            this.inspectorId = inspectorId;
            this.eventAtMillis = eventAtMillis;
            this.durationMillis = durationMillis;
            this.serverNowMillis = serverNowMillis;
            this.endAtMillis = endAtMillis;
        }
    }

    public static class FinishedRound {
        public final Long smugglerId;
        public final int smugglerAmount;
        public final Long inspectorId;
        public final int inspectorAmount;
        public final RoundOutcomeType outcomeType;

        public FinishedRound(Long smugglerId, int smugglerAmount, Long inspectorId, int inspectorAmount, RoundOutcomeType outcomeType) {
            this.smugglerId = smugglerId;
            this.smugglerAmount = smugglerAmount;
            this.inspectorId = inspectorId;
            this.inspectorAmount = inspectorAmount;
            this.outcomeType = outcomeType;
        }
    }

    public static class FinishedGame {
        public final GameWinnerType winnerType;
        public final int smugglerTotalBalance;
        public final int inspectorTotalBalance;

        public FinishedGame(GameWinnerType winnerType, int smugglerTotalBalance, int inspectorTotalBalance) {
            this.winnerType = winnerType;
            this.smugglerTotalBalance = smugglerTotalBalance;
            this.inspectorTotalBalance = inspectorTotalBalance;
        }
    }

    public static class TransferFailure {
        public final TransferFailureReason reason;
        public final String message;

        public TransferFailure(TransferFailureReason reason, String message) {
            this.reason = reason;
            this.message = message;
        }
    }

    public static class DecidedPass {
        public final Long inspectorId;
        public final boolean smugglerView;

        public DecidedPass(Long inspectorId, boolean smugglerView) {
            this.inspectorId = inspectorId;
            this.smugglerView = smugglerView;
        }
    }

    public static class DecidedInspection {
        public final Long inspectorId;
        public final int amount;
        public final boolean smugglerView;

        public DecidedInspection(Long inspectorId, int amount, boolean smugglerView) {
            this.inspectorId = inspectorId;
            this.amount = amount;
            this.smugglerView = smugglerView;
        }
    }

    public static class DecidedSmuggle {
        public final Long smugglerId;
        public final int amount;
        public final boolean smugglerView;

        public DecidedSmuggle(Long smugglerId, int amount, boolean smugglerView) {
            this.smugglerId = smugglerId;
            this.amount = amount;
            this.smugglerView = smugglerView;
        }
    }

    public static class Transfer {
        public final Long senderId;
        public final Long targetId;
        public final int senderBalance;
        public final int targetBalance;
        public final int amount;

        public Transfer(Long senderId, Long targetId, int senderBalance, int targetBalance, int amount) {
            this.senderId = senderId;
            this.targetId = targetId;
            this.senderBalance = senderBalance;
            this.targetBalance = targetBalance;
            this.amount = amount;
        }
    }

    public static class CreateLobby {
        public final int maxPlayerCount;
        public final String lobbyName;
        public final TeamRole teamRole;

        public CreateLobby(int maxPlayerCount, String lobbyName, TeamRole teamRole) {
            this.maxPlayerCount = maxPlayerCount;
            this.lobbyName = lobbyName;
            this.teamRole = teamRole;
        }
    }

    public static class CreatedLobby {
        public final Long roomId;
        public final Long hostId;
        public final int maxPlayerCount;
        public final int currentPlayerCount;
        public final String lobbyName;
        public final List<LobbyParticipant> lobbyParticipants;

        public CreatedLobby(Long roomId, Long hostId, int maxPlayerCount, int currentPlayerCount, String lobbyName, List<LobbyParticipant> lobbyParticipants) {
            this.roomId = roomId;
            this.hostId = hostId;
            this.maxPlayerCount = maxPlayerCount;
            this.currentPlayerCount = currentPlayerCount;
            this.lobbyName = lobbyName;
            this.lobbyParticipants = lobbyParticipants;
        }
    }

    public static class OtherJoined {
        public final Long joinerId;
        public final String joinerName;
        public final TeamRole teamRole;
        public final int currentPlayerCount;

        public OtherJoined(Long joinerId, String joinerName, TeamRole teamRole, int currentPlayerCount) {
            this.joinerId = joinerId;
            this.joinerName = joinerName;
            this.teamRole = teamRole;
            this.currentPlayerCount = currentPlayerCount;
        }
    }

    public static class JoinedLobby {
        public final Long roomId;
        public final Long hostId;
        public final int maxPlayerCount;
        public final int currentPlayerCount;
        public final String lobbyName;
        public final List<LobbyParticipant> lobbyParticipants;

        public JoinedLobby(Long roomId, Long hostId, int maxPlayerCount, int currentPlayerCount, String lobbyName, List<LobbyParticipant> lobbyParticipants) {
            this.roomId = roomId;
            this.hostId = hostId;
            this.maxPlayerCount = maxPlayerCount;
            this.currentPlayerCount = currentPlayerCount;
            this.lobbyName = lobbyName;
            this.lobbyParticipants = lobbyParticipants;
        }
    }

    public static class ToggleReady {
        public final Long playerId;
        public final boolean toggleReadyState;

        public ToggleReady(Long playerId, boolean toggleReadyState) {
            this.playerId = playerId;
            this.toggleReadyState = toggleReadyState;
        }
    }

    public static class ToggleTeam {
        public final Long playerId;
        public final String playerName;
        public final TeamRole teamRole;

        public ToggleTeam(Long playerId, String playerName, TeamRole teamRole) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.teamRole = teamRole;
        }
    }

    @Override
    public void sendExceptionMessage(ExceptionCode code, String exceptionMessage) {
        this.exceptionCode = code;
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    public void sendWebSocketPing() {
        this.pingSent = true;
    }

    @Override
    public void requestSessionReconnect() {
        this.reconnectRequested = true;
    }

    @Override
    public void sendRoomDirectoryUpdated(List<RoomDirectorySnapshot> rooms, int totalCount) {
        this.roomSnapshots = rooms;
        this.roomTotalCount = totalCount;
    }

    @Override
    public void sendStartGame(Long playerId, List<GameStartPlayer> allPlayers) {
        this.startGameSent = true;
        this.startGamePlayers = new ArrayList<>(allPlayers);
    }

    @Override
    public void sendSelectionTimer(int round, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) {
        this.selectionTimer = new SelectionTimer(round, eventAtMillis, durationMillis, serverNowMillis, endAtMillis);
    }

    @Override
    public void sendRegisteredSmugglerId(Long playerId) {
        this.registeredSmugglerId = playerId;
    }

    @Override
    public void sendFixedSmugglerId(Long playerId) {
        this.fixedSmugglerId = playerId;
    }

    @Override
    public void sendFixedSmugglerIdForInspector() {
        this.fixedSmugglerIdForInspectorSent = true;
    }

    @Override
    public void sendRegisteredInspectorId(Long playerId) {
        this.registeredInspectorId = playerId;
    }

    @Override
    public void sendFixedInspectorId(Long playerId) {
        this.fixedInspectorId = playerId;
    }

    @Override
    public void sendFixedInspectorIdForSmuggler() {
        this.fixedInspectorIdForSmugglerSent = true;
    }

    @Override
    public void sendSmugglerApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) {
        this.smugglerApprovalState = new ApprovalState(candidateId, approverIds, fixed);
    }

    @Override
    public void sendInspectorApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) {
        this.inspectorApprovalState = new ApprovalState(candidateId, approverIds, fixed);
    }

    @Override
    public void sendStartNewRound(int currentRound, Long smugglerId, Long inspectorId, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) {
        this.startRound = new StartRound(currentRound, smugglerId, inspectorId, eventAtMillis, durationMillis, serverNowMillis, endAtMillis);
    }

    @Override
    public void sendFinishedRound(Long smugglerId, int smugglerAmount, Long inspectorId, int inspectorAmount, RoundOutcomeType outcomeType) {
        this.finishedRound = new FinishedRound(smugglerId, smugglerAmount, inspectorId, inspectorAmount, outcomeType);
    }

    @Override
    public void sendFinishedGame(GameWinnerType gameWinnerType, int smugglerTotalBalance, int inspectorTotalBalance) {
        this.finishedGame = new FinishedGame(gameWinnerType, smugglerTotalBalance, inspectorTotalBalance);
    }

    @Override
    public void sendTransferFailed(TransferFailureReason reason, String message) {
        this.transferFailure = new TransferFailure(reason, message);
    }

    @Override
    public void sendDecidedPass(Long inspectorId) {
        this.decidedPass = new DecidedPass(inspectorId, false);
    }

    @Override
    public void sendDecideInspectorBehaviorForSmugglerTeam() {
        this.decidedPass = new DecidedPass(null, true);
    }

    @Override
    public void sendDecidedInspection(Long inspectorId, int amount) {
        this.decidedInspection = new DecidedInspection(inspectorId, amount, false);
    }

    @Override
    public void sendDecideSmugglerAmountForSmugglerTeam(Long smugglerId, int amount) {
        this.decidedSmuggle = new DecidedSmuggle(smugglerId, amount, true);
    }

    @Override
    public void sendDecideSmugglerAmountForInspectorTeam() {
        this.decidedSmuggle = new DecidedSmuggle(null, 0, false);
    }

    @Override
    public void sendTransfer(Long senderId, Long targetId, int senderBalance, int targetBalance, int amount) {
        this.transfer = new Transfer(senderId, targetId, senderBalance, targetBalance, amount);
    }

    @Override
    public void sendCreateLobby(int maxPlayerCount, String lobbyName, TeamRole teamRole) {
        this.createLobby = new CreateLobby(maxPlayerCount, lobbyName, teamRole);
    }

    @Override
    public void sendCreatedLobby(Long roomId, Long hostId, int maxPlayerCount, int currentPlayerCount, String lobbyName, List<LobbyParticipant> lobbyParticipants) {
        this.createdLobby = new CreatedLobby(roomId, hostId, maxPlayerCount, currentPlayerCount, lobbyName, lobbyParticipants);
    }

    @Override
    public void sendOtherPlayerJoinedLobby(Long joinerId, String joinerName, TeamRole teamRole, int currentPlayerCount) {
        this.otherJoined = new OtherJoined(joinerId, joinerName, teamRole, currentPlayerCount);
    }

    @Override
    public void sendJoinedLobby(Long roomId, Long hostId, int maxPlayerCount, int currentPlayerCount, String lobbyName, List<LobbyParticipant> lobbyParticipants) {
        this.joinedLobby = new JoinedLobby(roomId, hostId, maxPlayerCount, currentPlayerCount, lobbyName, lobbyParticipants);
    }

    @Override
    public void sendToggledReady(Long playerId, boolean toggleReadyState) {
        this.toggleReady = new ToggleReady(playerId, toggleReadyState);
    }

    @Override
    public void sendToggledTeam(Long playerId, String playerName, TeamRole teamRole) {
        this.toggleTeam = new ToggleTeam(playerId, playerName, teamRole);
    }

    @Override
    public void sendLeftLobby() {
        this.leftLobby = true;
    }

    @Override
    public void sendOtherPlayerLeftLobby(Long playerId) {
        this.otherLeftLobby = playerId;
    }

    @Override
    public void sendKickedLobby() {
        this.kickedLobby = true;
    }

    @Override
    public void sendOtherPlayerKicked(Long playerId) {
        this.otherKicked = playerId;
    }

    @Override
    public void sendHostDeletedLobby() {
        this.hostDeletedLobby = true;
    }

    @Override
    public void sendLobbyDeleted() {
        this.lobbyDeleted = true;
    }
}
