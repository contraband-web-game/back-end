package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.application.game.dto.ActiveGameView;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClearActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.QueryActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.RegisterRuntimeSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.TerminateSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UnregisterRuntimeSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UpdateActiveGame;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.ClearLobby;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateContrabandGame;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.chat.GameChatProtocol.GameChatCommand;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.Terminated;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import org.apache.pekko.pattern.StatusReply;

public class ClientSessionGatewayActor extends AbstractBehavior<ClientSessionCommand> {

    public static final EntityTypeKey<ClientSessionCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(ClientSessionCommand.class, "client-sessions");

    public static Behavior<ClientSessionCommand> create() {
        return Behaviors.setup(ClientSessionGatewayActor::new);
    }

    private ClientSessionGatewayActor(ActorContext<ClientSessionCommand> context) {
        super(context);
    }

    private ActorRef<ClientSessionCommand> runtimeSession;
    private ActiveGame activeGame;
    private ActorRef<LobbyCommand> lobby;
    private ActorRef<ContrabandGameCommand> contrabandGame;
    private GameChatSync gameChatSync;

    @Override
    public Receive<ClientSessionCommand> createReceive() {
        return newReceiveBuilder().onMessage(RegisterRuntimeSession.class, this::onRegisterRuntimeSession)
                                  .onMessage(UnregisterRuntimeSession.class, this::onUnregisterRuntimeSession)
                                  .onMessage(UpdateLobby.class, this::onUpdateLobby)
                                  .onMessage(ClearLobby.class, this::onClearLobby)
                                  .onMessage(UpdateContrabandGame.class, this::onUpdateContrabandGame)
                                  .onMessage(SyncContrabandGameChat.class, this::onSyncContrabandGameChat)
                                  .onMessage(ClearContrabandGameChat.class, this::onClearContrabandGameChat)
                                  .onMessage(UpdateActiveGame.class, this::onUpdateActiveGame)
                                  .onMessage(ClearActiveGame.class, this::onClearActiveGame)
                                  .onMessage(QueryActiveGame.class, this::onQueryActiveGame)
                                  .onSignal(Terminated.class, this::onTerminated)
                                  .onAnyMessage(this::forwardToRuntimeSession)
                                  .build();
    }

    private Behavior<ClientSessionCommand> onRegisterRuntimeSession(RegisterRuntimeSession command) {
        ActorRef<ClientSessionCommand> nextSession = command.runtimeSession();

        if (runtimeSession != null && !runtimeSession.equals(nextSession)) {
            getContext().unwatch(runtimeSession);
            runtimeSession.tell(new TerminateSession());
        }

        runtimeSession = nextSession;
        getContext().watch(runtimeSession);

        if (activeGame != null) {
            runtimeSession.tell(new UpdateActiveGame(activeGame.roomId(), activeGame.entityId()));
        }
        if (lobby != null) {
            runtimeSession.tell(new UpdateLobby(lobby));
        }
        if (contrabandGame != null) {
            runtimeSession.tell(new UpdateContrabandGame(contrabandGame));
        }
        if (gameChatSync != null) {
            runtimeSession.tell(new SyncContrabandGameChat(gameChatSync.chat(), gameChatSync.teamRole()));
        }

        return this;
    }

    private Behavior<ClientSessionCommand> onUnregisterRuntimeSession(UnregisterRuntimeSession command) {
        if (runtimeSession == null) {
            return this;
        }
        if (!runtimeSession.equals(command.runtimeSession())) {
            return this;
        }

        getContext().unwatch(runtimeSession);
        runtimeSession = null;
        return this;
    }

    private Behavior<ClientSessionCommand> onUpdateLobby(UpdateLobby command) {
        lobby = command.lobby();
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onClearLobby(ClearLobby command) {
        lobby = null;
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onUpdateContrabandGame(UpdateContrabandGame command) {
        contrabandGame = command.smugglingGame();
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onSyncContrabandGameChat(SyncContrabandGameChat command) {
        gameChatSync = new GameChatSync(command.chat(), command.teamRole());
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onClearContrabandGameChat(ClearContrabandGameChat command) {
        gameChatSync = null;
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onUpdateActiveGame(UpdateActiveGame command) {
        activeGame = new ActiveGame(command.roomId(), command.entityId());
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onClearActiveGame(ClearActiveGame command) {
        activeGame = null;
        contrabandGame = null;
        forward(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onQueryActiveGame(QueryActiveGame command) {
        command.replyTo().tell(StatusReply.success(toActiveGameView()));
        return this;
    }

    private Behavior<ClientSessionCommand> onTerminated(Terminated signal) {
        if (runtimeSession != null && runtimeSession.equals(signal.getRef())) {
            runtimeSession = null;
        }

        return this;
    }

    private Behavior<ClientSessionCommand> forwardToRuntimeSession(ClientSessionCommand command) {
        forward(command);
        return this;
    }

    private void forward(ClientSessionCommand command) {
        if (runtimeSession == null) {
            return;
        }
        runtimeSession.tell(command);
    }

    private ActiveGameView toActiveGameView() {
        if (activeGame == null) {
            return null;
        }

        return new ActiveGameView(activeGame.roomId(), activeGame.entityId());
    }

    private record GameChatSync(
            ActorRef<GameChatCommand> chat,
            TeamRole teamRole
    ) { }

    private record ActiveGame(Long roomId, String entityId) implements CborSerializable { }
}
