package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorIdForSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerIdForInspector;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateInspectorApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSelectionTimer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSmugglerApprovalState;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.PrepareNextSelection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterInspector;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterSmuggler;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundReady;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundSelectionTimeout;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartNewRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.RoundReadySelection;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class ContrabandSelectionActor extends AbstractBehavior<ContrabandGameCommand> {

    private static final Duration SELECTION_DURATION = Duration.ofSeconds(33L);

    public static Behavior<ContrabandGameCommand> create(
            SelectionClientMessenger clientMessenger,
            SelectionChatCoordinator chatCoordinator,
            SelectionParticipants participants,
            ActorRef<ContrabandGameCommand> facade
    ) {
        return Behaviors.setup(
                context -> new ContrabandSelectionActor(
                        context,
                        clientMessenger,
                        chatCoordinator,
                        facade,
                        participants
                ).initialize()
        );
    }

    private ContrabandSelectionActor(
            ActorContext<ContrabandGameCommand> context,
            SelectionClientMessenger clientMessenger,
            SelectionChatCoordinator chatCoordinator,
            ActorRef<ContrabandGameCommand> facade,
            SelectionParticipants participants
    ) {
        super(context);
        this.clientMessenger = clientMessenger;
        this.chatCoordinator = chatCoordinator;
        this.facade = facade;
        this.selectionState = new SelectionState();
        this.participants = participants;
    }

    private final SelectionClientMessenger clientMessenger;
    private final SelectionChatCoordinator chatCoordinator;
    private final ActorRef<ContrabandGameCommand> facade;
    private final SelectionState selectionState;
    private final SelectionParticipants participants;

    private ContrabandSelectionActor initialize() {
        seedInitialRoundIfSinglePerTeam();
        startSelectionPhaseIfNeeded();
        return this;
    }

    @Override
    public Receive<ContrabandGameCommand> createReceive() {
        return newReceiveBuilder().onMessage(RegisterSmugglerId.class, this::onRegisterSmugglerId)
                                  .onMessage(RegisterSmuggler.class, this::onRegisterSmuggler)
                                  .onMessage(FixSmugglerId.class, this::onFixSmugglerId)
                                  .onMessage(RegisterInspectorId.class, this::onRegisterInspectorId)
                                  .onMessage(RegisterInspector.class, this::onRegisterInspector)
                                  .onMessage(FixInspectorId.class, this::onFixInspectorId)
                                  .onMessage(RoundSelectionTimeout.class, this::onRoundSelectionTimeout)
                                  .onMessage(PrepareNextSelection.class, this::onPrepareNextSelection)
                                  .onMessage(SyncReconnectedPlayer.class, this::onSyncReconnectedPlayer)
                                  .onSignal(PostStop.class, this::onPostStop)
                                  .build();
    }

    private Behavior<ContrabandGameCommand> onRegisterSmugglerId(RegisterSmugglerId command) {
        if (needsRoundAlignment(command.currentRound())) {
            return this;
        }

        ActorRef<ClientSessionCommand> smugglerClientSession = clientMessenger.teamSession(
                TeamRole.SMUGGLER,
                command.smugglerId()
        );

        if (smugglerClientSession == null) {
            return this;
        }

        try {
            selectionState.registerSmugglerId(command.currentRound(), command.smugglerId());
            clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateRegisterSmugglerId(command.smugglerId()));
            propagateSmugglerApprovalState();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            smugglerClientSession.tell(new HandleExceptionMessage(ExceptionCode.GAME_INVALID_STATE));
        }
        return this;
    }

    private Behavior<ContrabandGameCommand> onRegisterSmuggler(RegisterSmuggler command) {
        return onRegisterSmugglerId(new RegisterSmugglerId(command.smugglerId(), selectionState.currentRound()));
    }

    private Behavior<ContrabandGameCommand> onFixSmugglerId(FixSmugglerId command) {
        if (requiresSmugglerConsensus()) {
            selectionState.toggleSmugglerApproval(command.requesterId());
            if (!selectionState.hasEnoughSmugglerApprovals(requiredSmugglerApprovals())) {
                propagateSmugglerApprovalState();
                return this;
            }
        }

        selectionState.fixSmugglerId();
        clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateFixedSmugglerId(selectionState.smugglerId()));
        clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateFixedSmugglerIdForInspector());
        propagateSmugglerApprovalState();

        syncRoundChatIdIfReady();
        startRoundIfReady();
        return this;
    }

    private Behavior<ContrabandGameCommand> onRegisterInspector(RegisterInspector command) {
        return onRegisterInspectorId(new RegisterInspectorId(command.inspectorId(), selectionState.currentRound()));
    }

    private Behavior<ContrabandGameCommand> onRegisterInspectorId(RegisterInspectorId command) {
        if (needsRoundAlignment(command.currentRound())) {
            return this;
        }
        ActorRef<ClientSessionCommand> inspectorClientSession = clientMessenger.teamSession(
                TeamRole.INSPECTOR,
                command.inspectorId()
        );

        if (inspectorClientSession == null) {
            return this;
        }

        try {
            selectionState.registerInspectorId(command.currentRound(), command.inspectorId());
            clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateRegisterInspectorId(command.inspectorId()));
            propagateInspectorApprovalState();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            inspectorClientSession.tell(new HandleExceptionMessage(ExceptionCode.GAME_INVALID_STATE));
        }
        return this;
    }

    private Behavior<ContrabandGameCommand> onFixInspectorId(FixInspectorId command) {
        if (requiresInspectorConsensus()) {
            selectionState.toggleInspectorApproval(command.requesterId());
            if (!selectionState.hasEnoughInspectorApprovals(requiredInspectorApprovals())) {
                propagateInspectorApprovalState();
                return this;
            }
        }

        selectionState.fixInspectorId();
        clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateFixedInspectorId(selectionState.inspectorId()));
        clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateFixedInspectorIdForSmuggler());
        propagateInspectorApprovalState();

        syncRoundChatIdIfReady();
        startRoundIfReady();
        return this;
    }

    private Behavior<ContrabandGameCommand> onRoundSelectionTimeout(RoundSelectionTimeout command) {
        if (selectionState.isDifferentRound(command.round()) || selectionState.isReady()) {
            return this;
        }

        selectRandomPlayerIfNeeded(TeamRole.SMUGGLER);
        selectRandomPlayerIfNeeded(TeamRole.INSPECTOR);
        syncRoundChatIdIfReady();
        startRoundIfReady();
        return this;
    }

    private Behavior<ContrabandGameCommand> onPrepareNextSelection(PrepareNextSelection command) {
        prepareRoundIfAhead(command.nextRound());
        return this;
    }

    private Behavior<ContrabandGameCommand> onSyncReconnectedPlayer(SyncReconnectedPlayer command) {
        TeamRole teamRole = resolveTeamRole(command.playerId());
        if (teamRole == null) {
            return this;
        }

        ActorRef<ClientSessionCommand> targetSession = clientMessenger.totalSession(command.playerId());
        if (targetSession == null) {
            return this;
        }

        syncSelectionStateFor(targetSession, teamRole);
        return this;
    }

    private Behavior<ContrabandGameCommand> onPostStop(PostStop signal) {
        selectionState.cancelSelectionTimeout();
        return Behaviors.same();
    }

    private void startRoundIfReady() {
        if (selectionState.isReady()) {
            cancelSelectionTimeout();
            facade.tell(
                    new RoundReady(
                            new RoundReadySelection(
                                    selectionState.smugglerId(),
                                    selectionState.inspectorId(),
                                    selectionState.currentRound()
                            )
                    )
            );
            facade.tell(new StartNewRound());
        }
    }

    private void seedInitialRoundIfSinglePerTeam() {
        if (!participants.isTwoPlayerGame()) {
            return;
        }

        autoSeedAndStartRound();
    }

    private void autoSeedAndStartRound() {
        if (!participants.isTwoPlayerGame()) {
            return;
        }
        if (!participants.hasBothCandidates()) {
            return;
        }

        Long smuggler = participants.firstSmugglerId().orElse(null);
        Long inspector = participants.firstInspectorId().orElse(null);

        selectionState.seed(smuggler, inspector);
        clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateRegisterSmugglerId(smuggler));
        clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateFixedSmugglerId(smuggler));
        clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateRegisterInspectorId(inspector));
        clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateFixedInspectorId(inspector));
        chatCoordinator.syncRoundChatId(smuggler, inspector);
        facade.tell(new RoundReady(new RoundReadySelection(smuggler, inspector, selectionState.currentRound())));
        facade.tell(new StartNewRound());
    }

    private void startSelectionPhaseIfNeeded() {
        if (participants.isTwoPlayerGame()) {
            return;
        }
        if (selectionState.isReady()) {
            return;
        }

        scheduleSelectionTimeout();
    }

    private void scheduleSelectionTimeout() {
        cancelSelectionTimeout();
        Instant startedAt = Instant.now();
        selectionState.initSelectionTimeout(
                getContext().scheduleOnce(
                        SELECTION_DURATION,
                        facade,
                        new RoundSelectionTimeout(selectionState.currentRound())
                ),
                startedAt,
                SELECTION_DURATION
        );
        long endAt = startedAt.plus(SELECTION_DURATION).toEpochMilli();
        clientMessenger.broadcastSelectionTimer(
                selectionState.currentRound(),
                startedAt.toEpochMilli(),
                SELECTION_DURATION.toMillis(),
                startedAt.toEpochMilli(),
                endAt
        );
    }

    private void cancelSelectionTimeout() {
        selectionState.cancelSelectionTimeout();
    }

    private void selectRandomPlayerIfNeeded(TeamRole teamRole) {
        if (isPlayerAlreadySelected(teamRole)) {
            return;
        }

        if (teamRole.isSmuggler() && selectionState.isSmugglerReady()) {
            clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateRegisterSmugglerId(selectionState.smugglerId()));
            selectionState.fixSmugglerId();
            clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateFixedSmugglerId(selectionState.smugglerId()));
            clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateFixedSmugglerIdForInspector());
            return;
        }
        if (teamRole.isInspector() && selectionState.isInspectorReady()) {
            clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateRegisterInspectorId(selectionState.inspectorId()));
            selectionState.fixInspectorId();
            clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateFixedInspectorId(selectionState.inspectorId()));
            clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateFixedInspectorIdForSmuggler());
            return;
        }

        participants.pickRandomPlayerId(teamRole, ThreadLocalRandom.current())
                    .ifPresent(playerId -> registerAndFixPlayer(teamRole, playerId));
    }

    private boolean isPlayerAlreadySelected(TeamRole teamRole) {
        if (teamRole.isSmuggler()) {
            return selectionState.isSmugglerFixed();
        }

        return selectionState.isInspectorFixed();
    }

    private void registerAndFixPlayer(TeamRole teamRole, Long playerId) {
        fixRoundParticipantSmuggler(teamRole, playerId);
        fixRoundParticipantInspector(teamRole, playerId);
        clientMessenger.tellTeam(teamRole, createRegisterMessage(teamRole, playerId));
        clientMessenger.tellTeam(teamRole, createFixedMessage(teamRole, playerId));
        if (teamRole.isSmuggler()) {
            clientMessenger.tellTeam(TeamRole.INSPECTOR, new PropagateFixedSmugglerIdForInspector());
        }
        if (teamRole.isInspector()) {
            clientMessenger.tellTeam(TeamRole.SMUGGLER, new PropagateFixedInspectorIdForSmuggler());
        }
    }

    private void fixRoundParticipantSmuggler(TeamRole teamRole, Long playerId) {
        if (teamRole.isInspector()) {
            return;
        }

        selectionState.registerSmugglerId(playerId);
        selectionState.fixSmugglerId();
    }

    private void fixRoundParticipantInspector(TeamRole teamRole, Long playerId) {
        if (teamRole.isSmuggler()) {
            return;
        }

        selectionState.registerInspectorId(playerId);
        selectionState.fixInspectorId();
    }

    private ClientSessionCommand createRegisterMessage(TeamRole teamRole, Long playerId) {
        if (teamRole.isInspector()) {
            return new PropagateRegisterInspectorId(playerId);
        }

        return new PropagateRegisterSmugglerId(playerId);
    }

    private ClientSessionCommand createFixedMessage(TeamRole teamRole, Long playerId) {
        if (teamRole.isInspector()) {
            return new PropagateFixedInspectorId(playerId);
        }

        return new PropagateFixedSmugglerId(playerId);
    }

    private void syncRoundChatIdIfReady() {
        if (selectionState.isReady()) {
            chatCoordinator.syncRoundChatId(selectionState.smugglerId(), selectionState.inspectorId());
        }
    }

    private void syncSelectionStateFor(ActorRef<ClientSessionCommand> targetSession, TeamRole teamRole) {
        if (teamRole.isSmuggler() && selectionState.isSmugglerReady()) {
            targetSession.tell(new PropagateRegisterSmugglerId(selectionState.smugglerId()));
            if (selectionState.isSmugglerFixed()) {
                targetSession.tell(new PropagateFixedSmugglerId(selectionState.smugglerId()));
            }
        }
        if (teamRole.isInspector() && selectionState.isInspectorReady()) {
            targetSession.tell(new PropagateRegisterInspectorId(selectionState.inspectorId()));
            if (selectionState.isInspectorFixed()) {
                targetSession.tell(new PropagateFixedInspectorId(selectionState.inspectorId()));
            }
        }
        if (teamRole.isInspector() && selectionState.isSmugglerFixed()) {
            targetSession.tell(new PropagateFixedSmugglerIdForInspector());
        }
        if (teamRole.isSmuggler() && selectionState.isInspectorFixed()) {
            targetSession.tell(new PropagateFixedInspectorIdForSmuggler());
        }

        if (teamRole.isSmuggler()) {
            targetSession.tell(
                    new PropagateSmugglerApprovalState(
                            selectionState.smugglerCandidateId(),
                            selectionState.smugglerApprovalsSnapshot(),
                            selectionState.isSmugglerFixed()
                    )
            );
        }
        if (teamRole.isInspector()) {
            targetSession.tell(
                    new PropagateInspectorApprovalState(
                            selectionState.inspectorCandidateId(),
                            selectionState.inspectorApprovalsSnapshot(),
                            selectionState.isInspectorFixed()
                    )
            );
        }

        if (selectionState.isRoundNotReady()) {
            selectionState.currentSelectionTimer()
                        .ifPresent(snapshot -> targetSession.tell(
                                new PropagateSelectionTimer(
                                        selectionState.currentRound(),
                                        snapshot.startedAt().toEpochMilli(),
                                        snapshot.duration().toMillis(),
                                        Instant.now().toEpochMilli(),
                                        snapshot.startedAt().plus(snapshot.duration()).toEpochMilli()
                                )
                        ));
        }
    }

    private TeamRole resolveTeamRole(Long playerId) {
        if (clientMessenger.hasTeamSession(TeamRole.SMUGGLER, playerId)) {
            return TeamRole.SMUGGLER;
        }
        if (clientMessenger.hasTeamSession(TeamRole.INSPECTOR, playerId)) {
            return TeamRole.INSPECTOR;
        }
        return null;
    }

    private boolean requiresSmugglerConsensus() {
        return participants.requiresSmugglerConsensus();
    }

    private boolean requiresInspectorConsensus() {
        return participants.requiresInspectorConsensus();
    }

    private int requiredSmugglerApprovals() {
        return participants.requiredSmugglerApprovals();
    }

    private int requiredInspectorApprovals() {
        return participants.requiredInspectorApprovals();
    }

    private void propagateSmugglerApprovalState() {
        clientMessenger.tellTeam(
                TeamRole.SMUGGLER,
                new PropagateSmugglerApprovalState(
                        selectionState.smugglerCandidateId(),
                            selectionState.smugglerApprovalsSnapshot(),
                        selectionState.isSmugglerFixed()
                )
        );
    }

    private void propagateInspectorApprovalState() {
        clientMessenger.tellTeam(
                TeamRole.INSPECTOR,
                new PropagateInspectorApprovalState(
                        selectionState.inspectorCandidateId(),
                        selectionState.inspectorApprovalsSnapshot(),
                        selectionState.isInspectorFixed()
                )
        );
    }

    private boolean needsRoundAlignment(int requestedRound) {
        return !this.alignRoundIfNeeded(requestedRound);
    }

    private boolean alignRoundIfNeeded(int requestedRound) {
        int currentRound = selectionState.currentRound();
        if (requestedRound == currentRound) {
            return true;
        }
        if (requestedRound > currentRound) {
            prepareRoundIfAhead(requestedRound);
            return selectionState.currentRound() == requestedRound;
        }
        return false;
    }

    private void prepareRoundIfAhead(int targetRound) {
        if (targetRound <= selectionState.currentRound()) {
            return;
        }

        cancelSelectionTimeout();

        while (selectionState.currentRound() < targetRound) {
            selectionState.prepareNextRound();
        }

        if (participants.isTwoPlayerGame()) {
            autoSeedAndStartRound();
            return;
        }

        startSelectionPhaseIfNeeded();
    }
}
