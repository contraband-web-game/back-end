package com.game.contraband.infrastructure.actor.game.chat.lobby;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateKickedMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateLeftMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateNewMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateWelcomeMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher.ChatMessageEvent;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.List;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class LobbyChatActor extends AbstractBehavior<LobbyChatActor.LobbyChatCommand> {

    public static Behavior<LobbyChatCommand> create(
            Long roomId,
            String entityId,
            ActorRef<ClientSessionCommand> hostClientSession,
            Long hostId,
            ChatMessageEventPublisher chatMessageEventPublisher,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(
                context -> {
                    LobbyChatMetadata chatMetadata = new LobbyChatMetadata(roomId, entityId);
                    LobbyChatParticipants participants = new LobbyChatParticipants(hostId, hostClientSession);
                    LobbyChatTimeline chatTimeline = new LobbyChatTimeline(roomId);
                    LobbyChatBlacklistListener blacklistListener = new LobbyChatBlacklistListener(
                            chatBlacklistRepository,
                            blocked -> context.getSelf().tell(new MaskBlockedPlayerMessages(blocked))
                    );
                    return new LobbyChatActor(
                            context,
                            chatMetadata,
                            participants,
                            chatTimeline,
                            chatMessageEventPublisher,
                            blacklistListener
                    );
                }
        );
    }

    private LobbyChatActor(
            ActorContext<LobbyChatCommand> context,
            LobbyChatMetadata chatMetadata,
            LobbyChatParticipants participants,
            LobbyChatTimeline chatTimeline,
            ChatMessageEventPublisher chatMessageEventPublisher,
            LobbyChatBlacklistListener blacklistListener
    ) {
        super(context);
        this.chatMetadata = chatMetadata;
        this.participants = participants;
        this.chatTimeline = chatTimeline;
        this.chatMessageEventPublisher = chatMessageEventPublisher;
        this.blacklistListener = blacklistListener;
    }

    private final LobbyChatMetadata chatMetadata;
    private final LobbyChatParticipants participants;
    private final LobbyChatTimeline chatTimeline;
    private final ChatMessageEventPublisher chatMessageEventPublisher;
    private final LobbyChatBlacklistListener blacklistListener;

    @Override
    public Receive<LobbyChatCommand> createReceive() {
        return newReceiveBuilder().onMessage(SendMessage.class, this::onSendMessage)
                                  .onMessage(JoinMessage.class, this::onJoinMessage)
                                  .onMessage(LeftMessage.class, this::onLeftMessage)
                                  .onMessage(KickedMessage.class, this::onKickedMessage)
                                  .onMessage(Shutdown.class, this::onShutdown)
                                  .onMessage(MaskBlockedPlayerMessages.class, this::onMaskBlockedPlayerMessages)
                                  .onSignal(PostStop.class, this::onPostStop)
                                  .build();
    }

    private Behavior<LobbyChatCommand> onSendMessage(SendMessage command) {
        if (command.message() == null || command.message().isBlank()) {
            sendTo(
                    command.writerId(),
                    new HandleExceptionMessage(
                            ExceptionCode.CHAT_MESSAGE_EMPTY,
                            "메시지는 비어 있을 수 없습니다.")
            );
            return this;
        }
        if (blacklistListener.isBlocked(command.writerId())) {
            sendTo(command.writerId(), new HandleExceptionMessage(ExceptionCode.CHAT_USER_BLOCKED, "차단된 사용자입니다. 채팅을 보낼 수 없습니다."));
            return this;
        }

        ChatMessage chatMessage = chatTimeline.append(command.writerId(), command.writerName(), command.message());
        participants.forEach(target -> target.tell(new PropagateNewMessage(chatMessage)));
        chatMessageEventPublisher.publish(new ChatMessageEvent(chatMetadata.entityId(), chatMetadata.roomId(), ChatEventType.LOBBY_CHAT, null, chatMessage));
        return this;
    }

    private Behavior<LobbyChatCommand> onJoinMessage(JoinMessage command) {
        participants.add(command.playerId(), command.clientSession());
        participants.forEach(
                targetClientSession -> targetClientSession.tell(new PropagateWelcomeMessage(command.playerName()))
        );
        return this;
    }

    private Behavior<LobbyChatCommand> onLeftMessage(LeftMessage command) {
        participants.remove(command.playerId());
        participants.forEach(
                targetClientSession -> targetClientSession.tell(new PropagateLeftMessage(command.playerName()))
        );
        return this;
    }

    private Behavior<LobbyChatCommand> onKickedMessage(KickedMessage command) {
        participants.remove(command.playerId());
        participants.forEach(
                targetClientSession -> targetClientSession.tell(new PropagateKickedMessage(command.playerName()))
        );
        return this;
    }

    private Behavior<LobbyChatCommand> onMaskBlockedPlayerMessages(MaskBlockedPlayerMessages command) {
        List<ChatMessage> masked = chatTimeline.maskMessagesByWriter(command.playerId());

        if (masked.isEmpty()) {
            return this;
        }

        List<Long> maskedIds = masked.stream()
                                      .map(ChatMessage::id)
                                      .toList();

        participants.broadcastMasked(maskedIds, ChatEventType.LOBBY_CHAT);
        return this;
    }

    private Behavior<LobbyChatCommand> onShutdown(Shutdown command) {
        return Behaviors.stopped();
    }

    private Behavior<LobbyChatCommand> onPostStop(PostStop signal) {
        blacklistListener.close();
        return this;
    }

    private void sendTo(Long playerId, HandleExceptionMessage message) {
        ActorRef<ClientSessionCommand> clientSession = participants.get(playerId);
        if (clientSession != null) {
            clientSession.tell(message);
        }
    }

    public interface LobbyChatCommand extends CborSerializable { }

    public record SendMessage(Long writerId, String writerName, String message) implements LobbyChatCommand { }

    public record JoinMessage(ActorRef<ClientSessionCommand> clientSession, Long playerId, String playerName) implements LobbyChatCommand { }

    public record LeftMessage(Long playerId, String playerName) implements LobbyChatCommand { }

    public record KickedMessage(Long playerId, String playerName) implements LobbyChatCommand { }

    public record MaskBlockedPlayerMessages(Long playerId) implements LobbyChatCommand { }

    public record Shutdown() implements LobbyChatCommand { }
}
