package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import java.util.List;
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
        return newReceiveBuilder().onMessage(PropagateWelcomeMessage.class, this::onPropagateWelcomeMessage)
                                  .onMessage(PropagateNewMessage.class, this::onPropagateNewMessage)
                                  .onMessage(PropagateLeftMessage.class, this::onPropagateLeftMessage)
                                  .onMessage(PropagateKickedMessage.class, this::onPropagateKickedMessage)
                                  .onMessage(PropagateMaskedChatMessage.class, this::onPropagateMaskedChatMessage)
                                  .build();
    }

    private Behavior<ChatCommand> onPropagateWelcomeMessage(PropagateWelcomeMessage command) {
        sender.sendChatWelcome(command.playerName());
        return this;
    }

    private Behavior<ChatCommand> onPropagateNewMessage(PropagateNewMessage command) {
        sender.sendChatMessage(command.chatMessage());
        return this;
    }

    private Behavior<ChatCommand> onPropagateLeftMessage(PropagateLeftMessage command) {
        sender.sendChatLeft(command.playerName());
        return this;
    }

    private Behavior<ChatCommand> onPropagateKickedMessage(PropagateKickedMessage command) {
        sender.sendChatKicked(command.playerName());
        return this;
    }

    private Behavior<ChatCommand> onPropagateMaskedChatMessage(PropagateMaskedChatMessage command) {
        sender.sendMaskedChatMessage(command.messageId(), command.chatEvent().name());
        return this;
    }

    public record PropagateWelcomeMessage(String playerName) implements ChatCommand { }

    public record PropagateNewMessage(ChatMessage chatMessage) implements ChatCommand { }

    public record PropagateLeftMessage(String playerName) implements ChatCommand { }

    public record PropagateKickedMessage(String playerName) implements ChatCommand { }

    // 단일 채팅 메시지가 차단되었음을 알리기 위한 Actor 메시지
    public record PropagateMaskedChatMessage(Long messageId, ChatEventType chatEvent) implements ChatCommand { }

    // 여러 채팅 메시지가 차단되었음을 알리기 위한 Actor 메시지
    public record PropagateMaskedChatBatch(List<Long> messageIds, ChatEventType chatEvent) implements ChatCommand { }
}
