package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClearActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UpdateActiveGame;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.List;
import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class SessionOutboundActor extends AbstractBehavior<OutboundCommand> {

    private final Long playerId;
    private final ClientWebSocketMessageSender sender;
    private final ActorRef<ClientSessionCommand> gateway;
    private TeamRole teamRole;

    static Behavior<OutboundCommand> create(Long playerId, ClientWebSocketMessageSender sender, ActorRef<ClientSessionCommand> gateway) {
        return Behaviors.setup(context -> new SessionOutboundActor(context, playerId, sender, gateway));
    }

    private SessionOutboundActor(
            ActorContext<OutboundCommand> context,
            Long playerId,
            ClientWebSocketMessageSender sender,
            ActorRef<ClientSessionCommand> gateway
    ) {
        super(context);

        this.playerId = playerId;
        this.sender = sender;
        this.gateway = gateway;
    }

    @Override
    public Receive<OutboundCommand> createReceive() {
        return newReceiveBuilder().onMessage(HandleExceptionMessage.class, this::onHandleExceptionMessage)
                                  .onMessage(SendWebSocketPing.class, this::onSendWebSocketPing)
                                  .onMessage(RequestSessionReconnect.class, this::onRequestSessionReconnect)
                                  .onMessage(RoomDirectoryUpdated.class, this::onRoomDirectoryUpdated)
                                  .onMessage(PropagateStartGame.class, this::onPropagateStartGame)
                                  .onMessage(PropagateSelectionTimer.class, this::onPropagateSelectionTimer)
                                  .onMessage(PropagateRegisterSmugglerId.class, this::onPropagateRegisterSmugglerId)
                                  .onMessage(PropagateFixedSmugglerId.class, this::onPropagateFixedSmugglerId)
                                  .onMessage(PropagateFixedSmugglerIdForInspector.class, this::onPropagateFixedSmugglerIdForInspector)
                                  .onMessage(PropagateRegisterInspectorId.class, this::onPropagateRegisterInspectorId)
                                  .onMessage(PropagateFixedInspectorId.class, this::onPropagateFixedInspectorId)
                                  .onMessage(PropagateFixedInspectorIdForSmuggler.class, this::onPropagateFixedInspectorIdForSmuggler)
                                  .onMessage(PropagateSmugglerApprovalState.class, this::onPropagateSmugglerApprovalState)
                                  .onMessage(PropagateInspectorApprovalState.class, this::onPropagateInspectorApprovalState)
                                  .onMessage(PropagateStartNewRound.class, this::onPropagateStartNewRound)
                                  .onMessage(PropagateFinishedRound.class, this::onPropagateFinishedRound)
                                  .onMessage(PropagateFinishedGame.class, this::onPropagateFinishedGame)
                                  .onMessage(PropagateTransferFailed.class, this::onPropagateTransferFailed)
                                  .onMessage(PropagateDecidedPass.class, this::onPropagateDecidedPass)
                                  .onMessage(PropagateDecidedInspection.class, this::onPropagateDecidedInspection)
                                  .onMessage(PropagateDecidedSmuggleAmount.class, this::onPropagateDecidedSmuggleAmount)
                                  .onMessage(PropagateTransfer.class, this::onPropagateTransfer)
                                  .onMessage(PropagateCreateLobby.class, this::onPropagateCreateLobby)
                                  .build();
    }

    private Behavior<OutboundCommand> onHandleExceptionMessage(HandleExceptionMessage command) {
        sender.sendExceptionMessage(command.code(), command.exceptionMessage());
        return this;
    }

    private Behavior<OutboundCommand> onSendWebSocketPing(SendWebSocketPing command) {
        sender.sendWebSocketPing();
        return this;
    }

    private Behavior<OutboundCommand> onRequestSessionReconnect(RequestSessionReconnect command) {
        sender.requestSessionReconnect();
        return this;
    }

    private Behavior<OutboundCommand> onRoomDirectoryUpdated(RoomDirectoryUpdated command) {
        sender.sendRoomDirectoryUpdated(command.rooms(), command.totalCount());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateStartGame(PropagateStartGame command) {
        GameStartPlayer targetLobbyParticipant = command.allPlayers().stream()
                                                        .filter(player -> player.playerId().equals(playerId))
                                                        .findAny()
                                                        .orElse(null);

        if (targetLobbyParticipant == null) {
            return this;
        }

        this.teamRole = targetLobbyParticipant.teamRole();
        sender.sendStartGame(playerId, command.allPlayers());
        gateway.tell(new UpdateActiveGame(command.roomId(), command.entityId()));
        return this;
    }

    private Behavior<OutboundCommand> onPropagateSelectionTimer(PropagateSelectionTimer command) {
        sender.sendSelectionTimer(
                command.round(),
                command.eventAtMillis(),
                command.durationMillis(),
                command.serverNowMillis(),
                command.endAtMillis()
        );
        return this;
    }

    private Behavior<OutboundCommand> onPropagateRegisterSmugglerId(PropagateRegisterSmugglerId command) {
        if (this.teamRole == null || this.teamRole.isInspector()) {
            return this;
        }

        sender.sendRegisteredSmugglerId(command.smugglerId());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateFixedSmugglerId(PropagateFixedSmugglerId command) {
        if (this.teamRole == null || this.teamRole.isInspector()) {
            return this;
        }

        sender.sendFixedSmugglerId(command.smugglerId());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateFixedSmugglerIdForInspector(PropagateFixedSmugglerIdForInspector command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            return this;
        }

        sender.sendFixedSmugglerIdForInspector();
        return this;
    }

    private Behavior<OutboundCommand> onPropagateRegisterInspectorId(PropagateRegisterInspectorId command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            return this;
        }

        sender.sendRegisteredInspectorId(command.inspectorId());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateFixedInspectorId(PropagateFixedInspectorId command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            return this;
        }

        sender.sendFixedInspectorId(command.inspectorId());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateFixedInspectorIdForSmuggler(PropagateFixedInspectorIdForSmuggler command) {
        if (this.teamRole != null && this.teamRole.isInspector()) {
            return this;
        }

        sender.sendFixedInspectorIdForSmuggler();
        return this;
    }

    private Behavior<OutboundCommand> onPropagateSmugglerApprovalState(PropagateSmugglerApprovalState command) {
        if (this.teamRole != null && this.teamRole.isInspector()) {
            return this;
        }

        sender.sendSmugglerApprovalState(command.candidateId(), command.approverIds(), command.fixed());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateInspectorApprovalState(PropagateInspectorApprovalState command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            return this;
        }

        sender.sendInspectorApprovalState(command.candidateId(), command.approverIds(), command.fixed());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateStartNewRound(PropagateStartNewRound command) {
        sender.sendStartNewRound(
                command.currentRound(),
                command.smugglerId(),
                command.inspectorId(),
                command.eventAtMillis(),
                command.durationMillis(),
                command.serverNowMillis(),
                command.endAtMillis()
        );
        return this;
    }

    private Behavior<OutboundCommand> onPropagateFinishedRound(PropagateFinishedRound command) {
        sender.sendFinishedRound(
                command.smugglerId(),
                command.smugglerAmount(),
                command.inspectorId(),
                command.inspectorAmount(),
                command.outcomeType()
        );
        return this;
    }

    private Behavior<OutboundCommand> onPropagateFinishedGame(PropagateFinishedGame command) {
        sender.sendFinishedGame(
                command.gameWinnerType(),
                command.smugglerTotalBalance(),
                command.inspectorTotalBalance()
        );
        gateway.tell(new ClearActiveGame());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateTransferFailed(PropagateTransferFailed command) {
        sender.sendTransferFailed(command.reason(), command.message());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateDecidedPass(PropagateDecidedPass command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            sender.sendDecideInspectorBehaviorForSmugglerTeam();
            return this;
        }

        sender.sendDecidedPass(command.inspectorId());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateDecidedInspection(PropagateDecidedInspection command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            sender.sendDecideInspectorBehaviorForSmugglerTeam();
            return this;
        }

        sender.sendDecidedInspection(command.inspectorId(), command.amount());
        return this;
    }

    private Behavior<OutboundCommand> onPropagateDecidedSmuggleAmount(PropagateDecidedSmuggleAmount command) {
        if (this.teamRole != null && this.teamRole.isSmuggler()) {
            sender.sendDecideSmugglerAmountForSmugglerTeam(command.smugglerId(), command.amount());
            return this;
        }

        sender.sendDecideSmugglerAmountForInspectorTeam();
        return this;
    }

    private Behavior<OutboundCommand> onPropagateTransfer(PropagateTransfer command) {
        sender.sendTransfer(
                command.senderId(),
                command.targetId(),
                command.senderBalance(),
                command.targetBalance(),
                command.amount()
        );
        return this;
    }

    private Behavior<OutboundCommand> onPropagateCreateLobby(PropagateCreateLobby command) {
        sender.sendCreateLobby(command.maxPlayerCount(), command.lobbyName(), command.teamRole());
        return this;
    }

    public record HandleExceptionMessage(ExceptionCode code, String exceptionMessage) implements OutboundCommand { }

    public record SendWebSocketPing() implements OutboundCommand { }

    public record RequestSessionReconnect() implements OutboundCommand { }

    public record RoomDirectoryUpdated(List<RoomDirectorySnapshot> rooms, int totalCount) implements OutboundCommand { }

    public record PropagateStartGame(ActorRef<ContrabandGameCommand> smugglingGame, Long roomId, String entityId, List<GameStartPlayer> allPlayers) implements OutboundCommand { }

    public record PropagateSelectionTimer(int round, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) implements OutboundCommand { }

    public record PropagateRegisterSmugglerId(Long smugglerId) implements OutboundCommand { }

    public record PropagateFixedSmugglerId(Long smugglerId) implements OutboundCommand { }

    public record PropagateFixedSmugglerIdForInspector() implements OutboundCommand { }

    public record PropagateRegisterInspectorId(Long inspectorId) implements OutboundCommand { }

    public record PropagateFixedInspectorId(Long inspectorId) implements OutboundCommand { }

    public record PropagateFixedInspectorIdForSmuggler() implements OutboundCommand { }

    public record PropagateSmugglerApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) implements OutboundCommand { }

    public record PropagateInspectorApprovalState(Long candidateId, Set<Long> approverIds, boolean fixed) implements OutboundCommand { }

    public record PropagateStartNewRound(int currentRound, Long smugglerId, Long inspectorId, long eventAtMillis, long durationMillis, long serverNowMillis, long endAtMillis) implements OutboundCommand { }

    public record PropagateFinishedRound(Long smugglerId, int smugglerAmount, Long inspectorId, int inspectorAmount, RoundOutcomeType outcomeType) implements OutboundCommand { }

    public record PropagateFinishedGame(GameWinnerType gameWinnerType, int smugglerTotalBalance, int inspectorTotalBalance) implements OutboundCommand { }

    public record PropagateTransferFailed(TransferFailureReason reason, String message) implements OutboundCommand { }

    public record PropagateDecidedPass(Long inspectorId) implements OutboundCommand { }

    public record PropagateDecidedInspection(Long inspectorId, int amount) implements OutboundCommand { }

    public record PropagateDecidedSmuggleAmount(Long smugglerId, int amount) implements OutboundCommand { }

    public record PropagateTransfer(Long senderId, Long targetId, int senderBalance, int targetBalance, int amount) implements OutboundCommand { }

    public record PropagateCreateLobby(ActorRef<LobbyCommand> lobby, int maxPlayerCount, String lobbyName, TeamRole teamRole) implements OutboundCommand { }
}
