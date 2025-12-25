package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.LocalDirectoryCommand;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class ClientSessionActor extends AbstractBehavior<ClientSessionCommand> {

    public static Behavior<ClientSessionCommand> create(
            Long playerId,
            ClientWebSocketMessageSender clientWebSocketMessageSender,
            ActorRef<LocalDirectoryCommand> roomDirectoryCache,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(
                context -> {
                    ActorRef<OutboundCommand> outbound = context.spawn(
                            SessionOutboundActor.create(playerId, clientWebSocketMessageSender, context.getSelf()),
                            "session-outbound-" + playerId
                    );
                    ActorRef<InboundCommand> inbound = context.spawn(
                            SessionInboundActor.create(context.getSelf()),
                            "session-inbound-" + playerId
                    );
                    ActorRef<PresenceCommand> presence = context.spawn(
                            SessionPresenceActor.create(playerId, outbound, context.getSelf(), roomDirectoryCache),
                            "session-presence-" + playerId
                    );
                    ActorRef<ChatCommand> chat = context.spawn(
                            SessionChatActor.create(playerId, clientWebSocketMessageSender, chatBlacklistRepository),
                            "session-chat-" + playerId
                    );

                    return new ClientSessionActor(context, outbound, inbound, presence, chat);
                }
        );
    }

    private ClientSessionActor(
            ActorContext<ClientSessionCommand> context,
            ActorRef<OutboundCommand> outbound,
            ActorRef<InboundCommand> inbound,
            ActorRef<PresenceCommand> presence,
            ActorRef<ChatCommand> chat
    ) {
        super(context);

        this.outbound = outbound;
        this.inbound = inbound;
        this.presence = presence;
        this.chat = chat;
    }

    private final ActorRef<OutboundCommand> outbound;
    private final ActorRef<InboundCommand> inbound;
    private final ActorRef<PresenceCommand> presence;
    private final ActorRef<ChatCommand> chat;
    private ActiveGame activeGame;

    @Override
    public Receive<ClientSessionCommand> createReceive() {
        return newReceiveBuilder().onMessage(OutboundCommand.class, this::forwardToOutbound)
                                  .onMessage(InboundCommand.class, this::forwardToInbound)
                                  .onMessage(PresenceCommand.class, this::forwardToPresence)
                                  .onMessage(ChatCommand.class, this::forwardToChat)
                                  .onMessage(UpdateActiveGame.class, this::onUpdateActiveGame)
                                  .onMessage(ReSyncConnection.class, this::onReSyncConnection)
                                  .onSignal(PostStop.class, this::onPostStop)
                                  .build();
    }

    private Behavior<ClientSessionCommand> forwardToOutbound(OutboundCommand command) {
        outbound.tell(command);
        return this;
    }

    private Behavior<ClientSessionCommand> forwardToInbound(InboundCommand command) {
        inbound.tell(command);
        return this;
    }

    private Behavior<ClientSessionCommand> forwardToPresence(PresenceCommand command) {
        presence.tell(command);
        return this;
    }

    private Behavior<ClientSessionCommand> forwardToChat(ChatCommand command) {
        chat.tell(command);
        return this;
    }

    private Behavior<ClientSessionCommand> onUpdateActiveGame(UpdateActiveGame command) {
        this.activeGame = new ActiveGame(command.roomId(), command.entityId());
        return this;
    }

    private Behavior<ClientSessionCommand> onReSyncConnection(ReSyncConnection command) {
        inbound.tell(new SessionInboundActor.ReSyncConnection(command.playerId()));
        presence.tell(new SessionPresenceActor.ResubscribeRoomDirectory());
        return this;
    }

    private Behavior<ClientSessionCommand> onPostStop(PostStop signal) {
        presence.tell(new SessionPresenceActor.UnregisterSessionCommand());
        return this;
    }

    public interface ClientSessionCommand extends CborSerializable { }

    public interface ChatCommand extends ClientSessionCommand { }

    public interface InboundCommand extends ClientSessionCommand { }

    public interface OutboundCommand extends ClientSessionCommand { }

    public interface PresenceCommand extends ClientSessionCommand { }

    public record UpdateActiveGame(Long roomId, String entityId) implements ClientSessionCommand { }

    public record ReSyncConnection(Long playerId) implements ClientSessionCommand { }

    private record ActiveGame(Long roomId, String entityId) { }
}
