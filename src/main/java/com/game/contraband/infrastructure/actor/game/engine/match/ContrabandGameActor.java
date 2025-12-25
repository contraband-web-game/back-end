package com.game.contraband.infrastructure.actor.game.engine.match;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.GameCleanup;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundReady;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SelectionCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartNewRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartSelectedRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.RoundReadySelection;
import com.game.contraband.infrastructure.actor.game.engine.match.round.ContrabandRoundActor;
import com.game.contraband.infrastructure.actor.game.engine.match.round.RoundChatCoordinator;
import com.game.contraband.infrastructure.actor.game.engine.match.round.RoundClientMessenger;
import com.game.contraband.infrastructure.actor.game.engine.match.round.RoundGameContext;
import com.game.contraband.infrastructure.actor.game.engine.match.selection.ContrabandSelectionActor;
import com.game.contraband.infrastructure.actor.game.engine.match.selection.SelectionChatCoordinator;
import com.game.contraband.infrastructure.actor.game.engine.match.selection.SelectionClientMessenger;
import com.game.contraband.infrastructure.actor.game.engine.match.selection.SelectionParticipants;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class ContrabandGameActor extends AbstractBehavior<ContrabandGameCommand> {

    private final ActorRef<ContrabandGameCommand> selectionActor;
    private final ActorRef<ContrabandGameCommand> roundActor;
    private RoundReadySelection pendingRound;

    public static Behavior<ContrabandGameCommand> create(
            Long roomId,
            String entityId,
            ContrabandGame contrabandGamee,
            ActorRef<LobbyCommand> parent,
            Map<Long, ActorRef<ClientSessionCommand>> clientSessions,
            ChatMessageEventPublisher chatMessageEventPublisher,
            GameLifecycleEventPublisher gameLifecycleEventPublisher,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(
                context -> {
                    ClientSessionRegistry clientSessionRegistry = ClientSessionRegistry.create(
                            contrabandGamee,
                            clientSessions,
                            context,
                            roomId,
                            entityId
                    );
                    ActorRef<ContrabandGameChatCommand> gameChat = context.spawn(
                            ContrabandGameChatActor.create(
                                    roomId,
                                    entityId,
                                    chatMessageEventPublisher,
                                    clientSessionRegistry.getSessionsByTeam(),
                                    chatBlacklistRepository
                            ),
                            "game-chat-" + roomId
                    );

                    clientSessionRegistry.syncGameChatForAll(gameChat);

                    RoundGameContext roundGameContext = new RoundGameContext(contrabandGamee);
                    RoundClientMessenger roundClientMessenger = new RoundClientMessenger(
                            clientSessionRegistry,
                            parent,
                            context.getSelf(),
                            gameLifecycleEventPublisher,
                            roomId,
                            entityId
                    );
                    RoundChatCoordinator roundChatCoordinator = new RoundChatCoordinator(gameChat, clientSessionRegistry);
                    SelectionParticipants participants = new SelectionParticipants(
                            contrabandGamee.smugglerPlayers(),
                            contrabandGamee.inspectorPlayers(),
                            contrabandGamee.smugglerTeamSize(),
                            contrabandGamee.inspectorTeamSize()
                    );
                    ActorRef<ContrabandGameCommand> roundActor = context.spawn(
                            ContrabandRoundActor.create(
                                    roomId,
                                    entityId,
                                    roundGameContext,
                                    roundClientMessenger,
                                    roundChatCoordinator
                            ),
                            "contraband-round-" + roomId
                    );

                    ActorRef<ContrabandGameCommand> selectionActor = context.spawn(
                            ContrabandSelectionActor.create(
                                    new SelectionClientMessenger(clientSessionRegistry),
                                    new SelectionChatCoordinator(gameChat),
                                    participants,
                                    context.getSelf()
                            ),
                            "contraband-selection-" + roomId
                    );

                    return new ContrabandGameActor(context, roundActor, selectionActor);
                }
        );
    }

    private ContrabandGameActor(
            ActorContext<ContrabandGameCommand> context,
            ActorRef<ContrabandGameCommand> roundActor,
            ActorRef<ContrabandGameCommand> selectionActor
    ) {
        super(context);
        this.roundActor = roundActor;
        this.selectionActor = selectionActor;
    }

    @Override
    public Receive<ContrabandGameCommand> createReceive() {
        return newReceiveBuilder().onMessage(RoundReady.class, this::onRoundReady)
                                  .onMessage(StartNewRound.class, this::onStartNewRound)
                                  .onMessage(SyncReconnectedPlayer.class, this::onSyncReconnectedPlayer)
                                  .onMessage(GameCleanup.class, this::onGameCleanup)
                                  .onMessage(SelectionCommand.class, this::forwardToSelection)
                                  .onMessage(RoundCommand.class, this::forwardToRound)
                                  .build();
    }

    private Behavior<ContrabandGameCommand> forwardToSelection(ContrabandGameCommand command) {
        selectionActor.tell(command);
        return this;
    }

    private Behavior<ContrabandGameCommand> forwardToRound(ContrabandGameCommand command) {
        roundActor.tell(command);
        return this;
    }

    private Behavior<ContrabandGameCommand> onRoundReady(RoundReady command) {
        this.pendingRound = command.selection();
        return this;
    }

    private Behavior<ContrabandGameCommand> onStartNewRound(StartNewRound command) {
        if (pendingRound != null) {
            roundActor.tell(new StartSelectedRound(pendingRound));
            pendingRound = null;
        }
        return this;
    }

    private Behavior<ContrabandGameCommand> onSyncReconnectedPlayer(SyncReconnectedPlayer command) {
        selectionActor.tell(command);
        roundActor.tell(command);
        return this;
    }

    private Behavior<ContrabandGameCommand> onGameCleanup(GameCleanup command) {
        return Behaviors.stopped();
    }
}
