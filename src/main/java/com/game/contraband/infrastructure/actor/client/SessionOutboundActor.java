package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
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

    private SessionOutboundActor(ActorContext<OutboundCommand> context, Long playerId, ClientWebSocketMessageSender sender, ActorRef<ClientSessionCommand> gateway) {
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

    public record HandleExceptionMessage(ExceptionCode code, String exceptionMessage) implements OutboundCommand { }

    public record SendWebSocketPing() implements OutboundCommand { }

    public record RequestSessionReconnect() implements OutboundCommand { }
}
