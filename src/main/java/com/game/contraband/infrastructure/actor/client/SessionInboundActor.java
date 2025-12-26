package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.InboundCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class SessionInboundActor extends AbstractBehavior<InboundCommand> {

    static Behavior<InboundCommand> create(ActorRef<ClientSessionCommand> gateway) {
        return Behaviors.setup(context -> new SessionInboundActor(context, gateway));
    }

    private SessionInboundActor(ActorContext<InboundCommand> context, ActorRef<ClientSessionCommand> gateway) {
        super(context);
        this.gateway = gateway;
    }

    private final ActorRef<ClientSessionCommand> gateway;
    private ActorRef<LobbyCommand> lobby;
    private ActorRef<ContrabandGameCommand> contrabandGame;

    @Override
    public Receive<InboundCommand> createReceive() {
        return newReceiveBuilder().onMessage(ReSyncConnection.class, this::onReSyncConnection)
                                  .onMessage(UpdateContrabandGame.class, this::onUpdateContrabandGame)
                                  .build();
    }

    private Behavior<InboundCommand> onUpdateContrabandGame(UpdateContrabandGame command) {
        this.contrabandGame = command.smugglingGame();
        return this;
    }

    private Behavior<InboundCommand> onUpdateLobby(UpdateLobby command) {
        this.lobby = command.lobby();
        return this;
    }

    private Behavior<InboundCommand> onReSyncConnection(ReSyncConnection command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new SyncReconnectedPlayer(command.playerId()));
            return this;
        }

        return this;
    }

    public record UpdateContrabandGame(ActorRef<ContrabandGameCommand> smugglingGame) implements InboundCommand { }

    public record UpdateLobby(ActorRef<LobbyCommand> lobby) implements InboundCommand { }

    public record ReSyncConnection(Long playerId) implements InboundCommand { }
}
