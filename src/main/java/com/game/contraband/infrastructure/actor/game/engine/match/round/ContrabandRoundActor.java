package com.game.contraband.infrastructure.actor.game.engine.match.round;

import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.dto.RoundDto;
import com.game.contraband.domain.game.round.settle.RoundSettlement;
import com.game.contraband.domain.game.transfer.TransferFailureException;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedInspection;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedPass;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedSmuggleAmount;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartNewRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransfer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransferFailed;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideInspection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecidePass;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FinishCurrentRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FinishedGame;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.GameCleanup;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.PrepareNextSelection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundTimeout;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartSelectedRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.TransferAmount;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.RoundReadySelection;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class ContrabandRoundActor extends AbstractBehavior<ContrabandGameCommand> {

    private static final Duration ROUND_DURATION = Duration.ofSeconds(33L);

    public static Behavior<ContrabandGameCommand> create(
            Long roomId,
            String entityId,
            RoundGameContext gameContext,
            RoundClientMessenger clientMessenger,
            RoundChatCoordinator chatCoordinator
    ) {
        return Behaviors.setup(
                context -> new ContrabandRoundActor(
                        context,
                        roomId,
                        entityId,
                        gameContext,
                        clientMessenger,
                        chatCoordinator
                )
        );
    }

    private final Long roomId;
    private final String entityId;
    private final RoundGameContext gameContext;
    private final RoundClientMessenger clientMessenger;
    private final RoundChatCoordinator chatCoordinator;
    private final RoundFlowState roundState;

    private ContrabandRoundActor(
            ActorContext<ContrabandGameCommand> context,
            Long roomId,
            String entityId,
            RoundGameContext gameContext,
            RoundClientMessenger clientMessenger,
            RoundChatCoordinator chatCoordinator
    ) {
        super(context);
        this.roomId = roomId;
        this.entityId = entityId;
        this.gameContext = gameContext;
        this.clientMessenger = clientMessenger;
        this.chatCoordinator = chatCoordinator;
        this.roundState = new RoundFlowState();
    }

    @Override
    public Receive<ContrabandGameCommand> createReceive() {
        return newReceiveBuilder().onMessage(StartSelectedRound.class, this::onStartSelectedRound)
                                  .onMessage(TransferAmount.class, this::onTransferAmount)
                                  .onMessage(DecideSmuggleAmount.class, this::onDecideSmuggleAmount)
                                  .onMessage(DecidePass.class, this::onDecidePass)
                                  .onMessage(DecideInspection.class, this::onDecideInspection)
                                  .onMessage(FinishCurrentRound.class, this::onFinishCurrentRound)
                                  .onMessage(FinishedGame.class, this::onFinishedGame)
                                  .onMessage(RoundTimeout.class, this::onRoundTimeout)
                                  .onMessage(SyncReconnectedPlayer.class, this::onSyncReconnectedPlayer)
                                  .onSignal(PostStop.class, this::onPostStop)
                                  .build();
    }

    private Behavior<ContrabandGameCommand> onStartSelectedRound(StartSelectedRound command) {
        RoundReadySelection selection = command.selection();
        roundState.assignRound(selection);

        executeTotalCommand(
                () -> gameContext.startNewRound(selection.smugglerId(), selection.inspectorId()),
                () -> {
                    Instant startedAt = Instant.now();
                    long serverNow = startedAt.toEpochMilli();
                    long endAt = startedAt.plus(ROUND_DURATION).toEpochMilli();
                    clientMessenger.broadcastStartRound(
                            roundState,
                            startedAt,
                            ROUND_DURATION.toMillis(),
                            serverNow,
                            endAt
                    );
                    scheduleRoundTimeout(startedAt);
                },
                NoopRunnable.INSTANCE
        );
        return this;
    }

    private Behavior<ContrabandGameCommand> onTransferAmount(TransferAmount command) {
        ActorRef<ClientSessionCommand> fromClientSession = clientMessenger.totalSession(command.fromPlayerId());

        if (fromClientSession == null) {
            return this;
        }

        executeTransferCommand(
                fromClientSession,
                () -> gameContext.transfer(command.fromPlayerId(), command.toPlayerId(), command.amount()),
                () -> {
                    ActorRef<ClientSessionCommand> toClientSession = clientMessenger.totalSession(command.toPlayerId());
                    if (toClientSession == null) {
                        fromClientSession.tell(new PropagateTransferFailed(TransferFailureReason.UNKNOWN, "수신자 세션을 찾을 수 없습니다."));
                        return;
                    }

                    Money fromPlayerBalance = gameContext.balanceOf(command.fromPlayerId());
                    Money toPlayerBalance = gameContext.balanceOf(command.toPlayerId());

                    int amount = command.amount().getAmount();
                    PropagateTransfer payload = new PropagateTransfer(
                            command.fromPlayerId(),
                            command.toPlayerId(),
                            fromPlayerBalance.getAmount(),
                            toPlayerBalance.getAmount(),
                            amount
                    );
                    fromClientSession.tell(payload);
                    toClientSession.tell(payload);
                },
                ex -> handleTransferFailure(fromClientSession, ex)
        );
        return this;
    }

    private Behavior<ContrabandGameCommand> onDecideSmuggleAmount(DecideSmuggleAmount command) {
        ActorRef<ClientSessionCommand> smugglerClientSession = clientMessenger.teamSession(
                TeamRole.SMUGGLER,
                command.smugglerId()
        );

        if (smugglerClientSession == null) {
            return this;
        }
        if (roundState.isNotCurrentSmuggler(command.smugglerId())) {
            smugglerClientSession.tell(
                    new HandleExceptionMessage(
                            ExceptionCode.NOT_CURRENT_ROUND_SMUGGLER,
                            "현재 라운드를 진행하는 밀수꾼이 아닙니다."
                    )
            );
            return this;
        }

        executeClientCommand(
                () -> gameContext.decideSmuggleAmount(command.amount()),
                () -> {
                    clientMessenger.tellAll(new PropagateDecidedSmuggleAmount(command.smugglerId(), command.amount()));
                    roundState.markSmugglerActionDone();
                    tryFinishCurrentRound();
                },
                forwardExceptionTo(smugglerClientSession)
        );
        return this;
    }

    private Behavior<ContrabandGameCommand> onDecidePass(DecidePass command) {
        ActorRef<ClientSessionCommand> inspectorClientSession = clientMessenger.teamSession(
                TeamRole.INSPECTOR,
                command.inspectorId()
        );

        if (inspectorClientSession == null) {
            return this;
        }
        if (roundState.isNotCurrentInspector(command.inspectorId())) {
            inspectorClientSession.tell(
                    new HandleExceptionMessage(
                            ExceptionCode.NOT_CURRENT_ROUND_INSPECTOR,
                            "현재 라운드를 진행하는 검사관이 아닙니다."
                    )
            );
            return this;
        }

        executeClientCommand(
                gameContext::decidePass,
                () -> {
                    clientMessenger.tellAll(new PropagateDecidedPass(command.inspectorId()));
                    roundState.markInspectorActionDone();
                    tryFinishCurrentRound();
                },
                forwardExceptionTo(inspectorClientSession)
        );
        return this;
    }

    private Behavior<ContrabandGameCommand> onDecideInspection(DecideInspection command) {
        ActorRef<ClientSessionCommand> inspectorClientSession = clientMessenger.teamSession(
                TeamRole.INSPECTOR,
                command.inspectorId()
        );

        if (inspectorClientSession == null) {
            return this;
        }
        if (roundState.isNotCurrentInspector(command.inspectorId())) {
            inspectorClientSession.tell(
                    new HandleExceptionMessage(
                            ExceptionCode.NOT_CURRENT_ROUND_INSPECTOR,
                            "현재 라운드를 진행하는 검사관이 아닙니다."
                    )
            );
            return this;
        }

        executeClientCommand(
                () -> gameContext.decideInspection(command.amount()),
                () -> {
                    clientMessenger.tellAll(new PropagateDecidedInspection(command.inspectorId(), command.amount()));
                    roundState.markInspectorActionDone();
                    tryFinishCurrentRound();
                },
                forwardExceptionTo(inspectorClientSession)
        );
        return this;
    }

    private Behavior<ContrabandGameCommand> onFinishCurrentRound(FinishCurrentRound command) {
        if (gameContext.cannotFinishCurrentRound()) {
            return this;
        }

        executeTotalCommand(
                () -> {
                    RoundDto roundRecord = gameContext.finishCurrentRound();
                    RoundSettlement settlement = roundRecord.settlement();
                    Player smuggler = settlement.smuggler();
                    Player inspector = settlement.inspector();

                    clientMessenger.broadcastFinishedRound(
                            new PropagateFinishedRound(
                                    smuggler.getId(),
                                    smuggler.getBalance().getAmount(),
                                    inspector.getId(),
                                    inspector.getBalance().getAmount(),
                                    settlement.outcomeType()
                            )
                    );
                    chatCoordinator.clearRoundChatId();
                },
                () -> {
                    if (gameContext.isFinished()) {
                        clientMessenger.facade().tell(new FinishedGame());
                    } else {
                        clientMessenger.facade().tell(new PrepareNextSelection(roundState.nextRound()));
                    }
                    roundState.resetAfterFinish();
                },
                roundState::cancelRoundTimeout
        );

        return this;
    }

    private Behavior<ContrabandGameCommand> onFinishedGame(FinishedGame command) {
        if (gameContext.isNotFinished()) {
            return this;
        }

        roundState.resetAfterFinish();
        GameWinnerType gameWinnerType = gameContext.winner();
        Money smugglerTotalBalance = gameContext.smugglerTotalBalance();
        Money inspectorTotalBalance = gameContext.inspectorTotalBalance();

        clientMessenger.broadcastFinishedGame(
                new PropagateFinishedGame(
                        gameWinnerType,
                        smugglerTotalBalance.getAmount(),
                        inspectorTotalBalance.getAmount()
                )
        );
        clientMessenger.publishGameEnded();
        clearGameChatForAll();
        clientMessenger.notifyParentEndGame();
        clientMessenger.facade().tell(new GameCleanup());
        return Behaviors.stopped();
    }

    private Behavior<ContrabandGameCommand> onRoundTimeout(RoundTimeout command) {
        if (roundState.isDifferentRound(command.round())) {
            return this;
        }
        if (roundState.isSmugglerActionNotDone()) {
            clientMessenger.facade().tell(new DecideSmuggleAmount(roundState.smugglerId(), 0));
        }
        if (roundState.isInspectorActionNotDone()) {
            clientMessenger.facade().tell(new DecidePass(roundState.inspectorId()));
        }
        return this;
    }

    private Behavior<ContrabandGameCommand> onSyncReconnectedPlayer(SyncReconnectedPlayer command) {
        ActorRef<ClientSessionCommand> targetSession = clientMessenger.totalSession(command.playerId());
        if (targetSession == null) {
            return this;
        }

        TeamRole teamRole = resolveTeamRole(command.playerId());
        if (teamRole == null) {
            return this;
        }

        clientMessenger.sendStartGameToSession(targetSession, buildStartPlayerEntries());
        syncActiveRoundStateFor(targetSession);

        if (gameContext.isFinished()) {
            syncFinishedGame(targetSession);
        }
        return this;
    }

    private Behavior<ContrabandGameCommand> onPostStop(PostStop signal) {
        roundState.cancelRoundTimeout();
        return Behaviors.same();
    }

    private void tryFinishCurrentRound() {
        if (gameContext.canFinishCurrentRound()) {
            clientMessenger.facade().tell(new FinishCurrentRound());
        }
    }

    private void scheduleRoundTimeout(Instant startedAt) {
        roundState.cancelRoundTimeout();

        Cancellable cancellable = getContext().scheduleOnce(
                ROUND_DURATION,
                clientMessenger.facade(),
                new RoundTimeout(roundState.currentRound())
        );

        roundState.initRoundTimeout(cancellable, startedAt, ROUND_DURATION);
    }

    private void syncActiveRoundStateFor(ActorRef<ClientSessionCommand> targetSession) {
        gameContext.currentRound().ifPresent(round -> {
            Instant startInstant = roundState.currentRoundTimer()
                                             .map(RoundRuntimeState.TimerSnapshot::startedAt)
                                             .orElse(Instant.now());
            Duration durationObj = roundState.currentRoundTimer()
                                             .map(RoundRuntimeState.TimerSnapshot::duration)
                                             .orElse(ROUND_DURATION);
            long duration = durationObj.toMillis();
            long eventAt = startInstant.toEpochMilli();
            long serverNow = Instant.now().toEpochMilli();
            long endAt = startInstant.plus(durationObj).toEpochMilli();
            targetSession.tell(
                    new PropagateStartNewRound(
                            round.getRoundNumber(),
                            round.getSmugglerId(),
                            round.getInspectorId(),
                            eventAt,
                            duration,
                            serverNow,
                            endAt
                    )
            );
            if (round.isSmuggleAmountDeclared()) {
                targetSession.tell(
                        new PropagateDecidedSmuggleAmount(
                                round.getSmugglerId(),
                                round.getSmuggleAmount().getAmount()
                        )
                );
            }
            if (round.isInspectionDecisionProvided()) {
                syncInspectionDecision(targetSession, round);
            }
        });
    }

    private void syncInspectionDecision(ActorRef<ClientSessionCommand> targetSession, Round round) {
        if (round.isPass()) {
            targetSession.tell(new PropagateDecidedPass(round.getInspectorId()));
            return;
        }

        if (round.isInspection()) {
            targetSession.tell(
                    new PropagateDecidedInspection(
                            round.getInspectorId(),
                            round.getInspectionThreshold().getAmount()
                    )
            );
        }
    }

    private void syncFinishedGame(ActorRef<ClientSessionCommand> targetSession) {
        Money smugglerTotalBalance = gameContext.smugglerTotalBalance();
        Money inspectorTotalBalance = gameContext.inspectorTotalBalance();

        targetSession.tell(
                new PropagateFinishedGame(
                        gameContext.winner(),
                        smugglerTotalBalance.getAmount(),
                        inspectorTotalBalance.getAmount()
                )
        );
    }

    private TeamRole resolveTeamRole(Long playerId) {
        return gameContext.resolveTeamRole(playerId);
    }

    private void clearGameChatForAll() {
        chatCoordinator.clearGameChatForAll();
    }

    private void executeClientCommand(
            ClientCommand command,
            Runnable onSuccess,
            Consumer<Exception> onError
    ) {
        try {
            command.execute();
            onSuccess.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            onError.accept(ex);
        }
    }

    private void executeTransferCommand(
            ActorRef<ClientSessionCommand> clientSession,
            ClientCommand command,
            Runnable onSuccess,
            Consumer<TransferFailureException> onTransferError
    ) {
        try {
            command.execute();
            onSuccess.run();
        } catch (TransferFailureException ex) {
            onTransferError.accept(ex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            forwardExceptionTo(clientSession).accept(ex);
        }
    }

    private void executeTotalCommand(
            TotalCommand command,
            Runnable onSuccess,
            Runnable onError
    ) {
        try {
            command.execute();
            onSuccess.run();
        } catch (Exception ex) {
            clientMessenger.tellAll(new HandleExceptionMessage(ExceptionCode.GAME_INVALID_STATE, ex.getMessage()));
            onError.run();
        }
    }

    private Consumer<Exception> forwardExceptionTo(ActorRef<ClientSessionCommand> clientSession) {
        return ex -> clientSession.tell(new HandleExceptionMessage(ExceptionCode.GAME_INVALID_STATE, ex.getMessage()));
    }

    private void handleTransferFailure(
            ActorRef<ClientSessionCommand> clientSession,
            TransferFailureException ex
    ) {
        TransferFailureReason reason = ex.getReason() != null ? ex.getReason() : TransferFailureReason.UNKNOWN;
        clientSession.tell(new PropagateTransferFailed(reason, ex.getMessage()));
    }

    private List<GameStartPlayer> buildStartPlayerEntries() {
        return gameContext.buildStartPlayerEntries();
    }

    private interface ClientCommand {
        void execute();
    }

    private interface TotalCommand {
        void execute();
    }
}
