package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.InboundCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ChangeMaxPlayerCount;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.KickPlayer;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LeaveLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ReSyncPlayer;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.RequestDeleteLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.StartGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ToggleReady;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ToggleTeam;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideInspection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecidePass;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterInspector;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterSmuggler;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.TransferAmount;
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
                                  .onMessage(UpdateLobby.class, this::onUpdateLobby)
                                  .onMessage(ClearLobby.class, this::onClearLobby)
                                  .onMessage(RequestTransferMoney.class, this::onRequestTransferMoney)
                                  .onMessage(RequestDecideInspection.class, this::onRequestDecideInspection)
                                  .onMessage(RequestDecidePass.class, this::onRequestDecidePass)
                                  .onMessage(RequestDecideSmuggleAmount.class, this::onRequestDecideSmuggleAmount)
                                  .onMessage(RequestRegisterInspector.class, this::onRequestRegisterInspector)
                                  .onMessage(RequestRegisterSmuggler.class, this::onRequestRegisterSmuggler)
                                  .onMessage(RequestFixInspector.class, this::onRequestFixInspector)
                                  .onMessage(RequestFixSmuggler.class, this::onRequestFixSmuggler)
                                  .onMessage(RequestKickPlayer.class, this::onRequestKickPlayer)
                                  .onMessage(RequestLeaveLobby.class, this::onRequestLeaveLobby)
                                  .onMessage(RequestStartGame.class, this::onRequestStartGame)
                                  .onMessage(RequestChangeMaxPlayerCount.class, this::onRequestChangeMaxPlayerCount)
                                  .onMessage(RequestToggleTeam.class, this::onRequestToggleTeam)
                                  .onMessage(RequestToggleReady.class, this::onRequestToggleReady)
                                  .onMessage(RequestLobbyDeletion.class, this::onRequestLobbyDeletion)
                                  .build();
    }

    private Behavior<InboundCommand> onReSyncConnection(ReSyncConnection command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new SyncReconnectedPlayer(command.playerId()));
            return this;
        }
        if (lobby != null) {
            lobby.tell(new ReSyncPlayer(command.playerId(), gateway));
            return this;
        }
        return this;
    }

    private Behavior<InboundCommand> onUpdateContrabandGame(UpdateContrabandGame command) {
        this.contrabandGame = command.smugglingGame();
        return this;
    }

    private Behavior<InboundCommand> onUpdateLobby(UpdateLobby command) {
        this.lobby = command.lobby();
        return this;
    }

    private Behavior<InboundCommand> onClearLobby(ClearLobby command) {
        this.lobby = null;
        return this;
    }

    private Behavior<InboundCommand> onRequestTransferMoney(RequestTransferMoney command) {
        if (contrabandGame != null) {
            contrabandGame.tell(
                    new TransferAmount(command.playerId(), command.targetPlayerId(), Money.from(command.amount()))
            );
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestDecideInspection(RequestDecideInspection command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new DecideInspection(command.playerId(), command.amount()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestDecidePass(RequestDecidePass command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new DecidePass(command.playerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestDecideSmuggleAmount(RequestDecideSmuggleAmount command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new DecideSmuggleAmount(command.playerId(), command.amount()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestRegisterInspector(RequestRegisterInspector command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new RegisterInspector(command.inspectorId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestRegisterSmuggler(RequestRegisterSmuggler command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new RegisterSmuggler(command.smugglerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestFixInspector(RequestFixInspector command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new FixInspectorId(command.playerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestFixSmuggler(RequestFixSmuggler command) {
        if (contrabandGame != null) {
            contrabandGame.tell(new FixSmugglerId(command.playerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestKickPlayer(RequestKickPlayer command) {
        if (lobby != null) {
            lobby.tell(new KickPlayer(command.executorId(), command.targetPlayerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestLeaveLobby(RequestLeaveLobby command) {
        if (lobby != null) {
            lobby.tell(new LeaveLobby(command.playerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestStartGame(RequestStartGame command) {
        if (lobby != null) {
            lobby.tell(new StartGame(command.executorId(), command.totalRounds()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestChangeMaxPlayerCount(RequestChangeMaxPlayerCount command) {
        if (lobby != null) {
            lobby.tell(new ChangeMaxPlayerCount(command.maxPlayerCount(), command.executorId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestToggleTeam(RequestToggleTeam command) {
        if (lobby != null) {
            lobby.tell(new ToggleTeam(command.playerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestToggleReady(RequestToggleReady command) {
        if (lobby != null) {
            lobby.tell(new ToggleReady(command.playerId()));
        }
        return this;
    }

    private Behavior<InboundCommand> onRequestLobbyDeletion(RequestLobbyDeletion command) {
        if (lobby != null) {
            lobby.tell(new RequestDeleteLobby(command.executorId()));
        }
        return this;
    }

    public record ReSyncConnection(Long playerId) implements InboundCommand { }

    public record UpdateContrabandGame(ActorRef<ContrabandGameCommand> smugglingGame) implements InboundCommand { }

    public record UpdateLobby(ActorRef<LobbyCommand> lobby) implements InboundCommand { }

    public record ClearLobby() implements InboundCommand { }

    public record RequestTransferMoney(Long playerId, Long targetPlayerId, int amount) implements InboundCommand { }

    public record RequestDecideInspection(Long playerId, int amount) implements InboundCommand { }

    public record RequestDecidePass(Long playerId) implements InboundCommand { }

    public record RequestDecideSmuggleAmount(Long playerId, int amount) implements InboundCommand { }

    public record RequestRegisterInspector(Long inspectorId) implements InboundCommand { }

    public record RequestRegisterSmuggler(Long smugglerId) implements InboundCommand { }

    public record RequestFixInspector(Long playerId) implements InboundCommand { }

    public record RequestFixSmuggler(Long playerId) implements InboundCommand { }

    public record RequestKickPlayer(Long executorId, Long targetPlayerId) implements InboundCommand { }

    public record RequestLeaveLobby(Long playerId) implements InboundCommand { }

    public record RequestStartGame(Long executorId, int totalRounds) implements InboundCommand { }

    public record RequestChangeMaxPlayerCount(int maxPlayerCount, Long executorId) implements InboundCommand { }

    public record RequestToggleTeam(Long playerId) implements InboundCommand { }

    public record RequestToggleReady(Long playerId) implements InboundCommand { }

    public record RequestLobbyDeletion(Long executorId) implements InboundCommand { }
}
