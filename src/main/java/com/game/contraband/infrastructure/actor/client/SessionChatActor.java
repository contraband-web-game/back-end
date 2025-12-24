package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ChatCommand;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class SessionChatActor extends AbstractBehavior<ChatCommand> {

    public static Behavior<ChatCommand> create(
            Long playerId,
            ClientWebSocketMessageSender sender,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(context -> new SessionChatActor(context, playerId, sender, chatBlacklistRepository));
    }

    private SessionChatActor(
            ActorContext<ChatCommand> context,
            Long playerId,
            ClientWebSocketMessageSender sender,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        super(context);

        this.playerId = playerId;
        this.sender = sender;
        this.chatBlacklistRepository = chatBlacklistRepository;
    }

    private final Long playerId;
    private final ClientWebSocketMessageSender sender;
    private final ChatBlacklistRepository chatBlacklistRepository;

    @Override
    public Receive<ChatCommand> createReceive() {
        return newReceiveBuilder().build();
    }
}
