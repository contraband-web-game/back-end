package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UpdateActiveGame;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.List;
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

    public record HandleExceptionMessage(ExceptionCode code, String exceptionMessage) implements OutboundCommand { }

    public record SendWebSocketPing() implements OutboundCommand { }

    public record RequestSessionReconnect() implements OutboundCommand { }

    public record RoomDirectoryUpdated(List<RoomDirectorySnapshot> rooms, int totalCount) implements OutboundCommand { }

    public record PropagateStartGame(ActorRef<ContrabandGameCommand> smugglingGame, Long roomId, String entityId, List<GameStartPlayer> allPlayers) implements OutboundCommand { }
}
