package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.PresenceCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RequestSessionReconnect;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.SendWebSocketPing;
import java.time.Duration;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class SessionPresenceActor extends AbstractBehavior<PresenceCommand> {

    static Behavior<PresenceCommand> create(
            Long playerId,
            ActorRef<OutboundCommand> outbound,
            ActorRef<ClientSessionCommand> gateway
    ) {
        return Behaviors.setup(
                context ->
                        Behaviors.withTimers(
                                timers -> {
                                    timers.startTimerAtFixedRate(
                                            new SessionHealthCheck(),
                                            Duration.ofSeconds(30L)
                                    );
                                    return new SessionPresenceActor(
                                            context,
                                            playerId,
                                            outbound,
                                            gateway
                                    );
                                }
                        )
        );
    }

    private SessionPresenceActor(
            ActorContext<PresenceCommand> context,
            Long playerId,
            ActorRef<OutboundCommand> outbound,
            ActorRef<ClientSessionCommand> gateway
    ) {
        super(context);

        this.playerId = playerId;
        this.outbound = outbound;
        this.gateway = gateway;
    }

    private static final int MAX_MISSED_SESSION_PONGS = 2;

    private final Long playerId;
    private final ActorRef<OutboundCommand> outbound;
    private final ActorRef<ClientSessionCommand> gateway;
    private int missedSessionPongs;

    @Override
    public Receive<PresenceCommand> createReceive() {
        return newReceiveBuilder().onMessage(SessionHealthCheck.class, this::onSessionHealthCheck)
                                  .onMessage(SessionPongReceived.class, this::onSessionPongReceived)
                                  .build();
    }

    private Behavior<PresenceCommand> onSessionHealthCheck(SessionHealthCheck command) {
        outbound.tell(new SendWebSocketPing());
        missedSessionPongs++;

        if (missedSessionPongs >= MAX_MISSED_SESSION_PONGS) {
            outbound.tell(new RequestSessionReconnect());
            missedSessionPongs = 0;
        }
        return this;
    }

    private Behavior<PresenceCommand> onSessionPongReceived(SessionPongReceived command) {
        missedSessionPongs = 0;
        return this;
    }

    public record SessionPongReceived() implements PresenceCommand { }

    public record SessionHealthCheck() implements PresenceCommand { }

    public record ResubscribeRoomDirectory() implements PresenceCommand { }

    public record UnregisterSessionCommand() implements PresenceCommand { }
}
