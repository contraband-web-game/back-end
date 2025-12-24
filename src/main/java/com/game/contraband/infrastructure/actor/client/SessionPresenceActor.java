package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.PresenceCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RequestSessionReconnect;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.SendWebSocketPing;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.LocalDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.RegisterSession;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.RequestRoomDirectoryPage;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.UnregisterSession;
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
            ActorRef<ClientSessionCommand> gateway,
            ActorRef<LocalDirectoryCommand> roomDirectoryCache
    ) {
        return Behaviors.setup(
                context ->
                        Behaviors.withTimers(
                                timers -> {
                                    roomDirectoryCache.tell(new RegisterSession(playerId, gateway));

                                    timers.startTimerAtFixedRate(
                                            new SessionHealthCheck(),
                                            Duration.ofSeconds(30L)
                                    );
                                    return new SessionPresenceActor(
                                            context,
                                            playerId,
                                            outbound,
                                            gateway,
                                            roomDirectoryCache
                                    );
                                }
                        )
        );
    }

    private SessionPresenceActor(
            ActorContext<PresenceCommand> context,
            Long playerId,
            ActorRef<OutboundCommand> outbound,
            ActorRef<ClientSessionCommand> gateway,
            ActorRef<LocalDirectoryCommand> roomDirectoryCache
    ) {
        super(context);

        this.playerId = playerId;
        this.outbound = outbound;
        this.gateway = gateway;
        this.roomDirectoryCache = roomDirectoryCache;
    }

    private static final int MAX_MISSED_SESSION_PONGS = 2;

    private final Long playerId;
    private final ActorRef<OutboundCommand> outbound;
    private final ActorRef<ClientSessionCommand> gateway;
    private final ActorRef<LocalDirectoryCommand> roomDirectoryCache;
    private int missedSessionPongs;

    @Override
    public Receive<PresenceCommand> createReceive() {
        return newReceiveBuilder().onMessage(SessionHealthCheck.class, this::onSessionHealthCheck)
                                  .onMessage(SessionPongReceived.class, this::onSessionPongReceived)
                                  .onMessage(ResubscribeRoomDirectory.class, this::onResubscribeRoomDirectory)
                                  .onMessage(RequestRoomDirectoryPageCommand.class, this::onRequestRoomDirectoryPage)
                                  .onMessage(UnregisterSessionCommand.class, this::onUnregister)
                                  .onSignal(PostStop.class, this::onPostStop)
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

    private Behavior<PresenceCommand> onResubscribeRoomDirectory(ResubscribeRoomDirectory command) {
        roomDirectoryCache.tell(new RegisterSession(playerId, gateway));
        return this;
    }

    private Behavior<PresenceCommand> onRequestRoomDirectoryPage(RequestRoomDirectoryPageCommand command) {
        roomDirectoryCache.tell(new RequestRoomDirectoryPage(command.page(), command.size()));
        return this;
    }

    private Behavior<PresenceCommand> onUnregister(UnregisterSessionCommand command) {
        roomDirectoryCache.tell(new UnregisterSession(playerId));
        return this;
    }

    private Behavior<PresenceCommand> onPostStop(PostStop signal) {
        roomDirectoryCache.tell(new UnregisterSession(playerId));
        return this;
    }

    public record SessionPongReceived() implements PresenceCommand { }

    public record SessionHealthCheck() implements PresenceCommand { }

    public record ResubscribeRoomDirectory() implements PresenceCommand { }

    public record UnregisterSessionCommand() implements PresenceCommand { }

    public record RequestRoomDirectoryPageCommand(int page, int size) implements PresenceCommand { }
}
