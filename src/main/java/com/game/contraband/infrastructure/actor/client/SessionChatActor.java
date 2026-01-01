package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.SendMessage;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ChatInRound;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ChatInspectorTeam;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ChatSmugglerTeam;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.List;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.Terminated;
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
    private final LobbyChatHolder lobbyChatHolder = new LobbyChatHolder();
    private final ContrabandGameChatHolder contrabandGameChatHolder = new ContrabandGameChatHolder();

    @Override
    public Receive<ChatCommand> createReceive() {
        return newReceiveBuilder().onMessage(PropagateWelcomeMessage.class, this::onPropagateWelcomeMessage)
                                  .onMessage(PropagateNewMessage.class, this::onPropagateNewMessage)
                                  .onMessage(PropagateLeftMessage.class, this::onPropagateLeftMessage)
                                  .onMessage(PropagateKickedMessage.class, this::onPropagateKickedMessage)
                                  .onMessage(PropagateMaskedChatMessage.class, this::onPropagateMaskedChatMessage)
                                  .onMessage(PropagateMaskedChatBatch.class, this::onPropagateMaskedChatBatch)
                                  .onMessage(PropagateSmugglerTeamChat.class, this::onPropagateSmugglerTeamChat)
                                  .onMessage(PropagateInspectorTeamChat.class, this::onPropagateInspectorTeamChat)
                                  .onMessage(PropagateRoundChat.class, this::onPropagateRoundChat)
                                  .onMessage(SyncContrabandGameChat.class, this::onSyncContrabandGameChat)
                                  .onMessage(ClearContrabandGameChat.class, this::onClearContrabandGameChat)
                                  .onMessage(SyncLobbyChat.class, this::onSyncLobbyChat)
                                  .onMessage(ClearLobbyChat.class, this::onClearLobbyChat)
                                  .onMessage(RequestSendTeamChat.class, this::onRequestSendTeamChat)
                                  .onMessage(RequestSendRoundChat.class, this::onRequestSendRoundChat)
                                  .onMessage(RequestSendChat.class, this::onRequestSendChat)
                                  .onSignal(Terminated.class, this::onTerminated)
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

    private Behavior<ChatCommand> onPropagateSmugglerTeamChat(PropagateSmugglerTeamChat command) {
        sender.sendSmugglerTeamChatMessage(command.chatMessage());
        return this;
    }

    private Behavior<ChatCommand> onPropagateInspectorTeamChat(PropagateInspectorTeamChat command) {
        sender.sendInspectorTeamChatMessage(command.chatMessage());
        return this;
    }

    private Behavior<ChatCommand> onPropagateRoundChat(PropagateRoundChat command) {
        sender.sendRoundChatMessage(command.chatMessage());
        return this;
    }

    private Behavior<ChatCommand> onSyncContrabandGameChat(SyncContrabandGameChat command) {
        contrabandGameChatHolder.set(command.chat(), command.teamRole(), getContext());
        return this;
    }

    private Behavior<ChatCommand> onClearContrabandGameChat(ClearContrabandGameChat command) {
        contrabandGameChatHolder.clear();
        return this;
    }

    private Behavior<ChatCommand> onSyncLobbyChat(SyncLobbyChat command) {
        lobbyChatHolder.set(command.lobbyChat(), getContext());
        return this;
    }

    private Behavior<ChatCommand> onClearLobbyChat(ClearLobbyChat command) {
        lobbyChatHolder.clear();
        return this;
    }

    private Behavior<ChatCommand> onRequestSendTeamChat(RequestSendTeamChat command) {
        if (chatBlacklistRepository.isBlocked(command.playerId())) {
            sender.sendExceptionMessage(
                    ExceptionCode.CHAT_USER_BLOCKED,
                    "차단된 사용자입니다. 채팅을 보낼 수 없습니다."
            );
            return this;
        }
        ContrabandGameChatHolder.GameChatRef gameChatRef = contrabandGameChatHolder.get();
        if (gameChatRef == null) {
            return this;
        }
        if (gameChatRef.teamRole.isSmuggler()) {
            gameChatRef.chat.tell(new ChatSmugglerTeam(command.playerId(), command.playerName(), command.message()));
            return this;
        }
        if (gameChatRef.teamRole.isInspector()) {
            gameChatRef.chat.tell(new ChatInspectorTeam(command.playerId(), command.playerName(), command.message()));
        }
        return this;
    }

    private Behavior<ChatCommand> onRequestSendRoundChat(RequestSendRoundChat command) {
        if (chatBlacklistRepository.isBlocked(command.playerId())) {
            sender.sendExceptionMessage(ExceptionCode.CHAT_USER_BLOCKED, "차단된 사용자입니다. 채팅을 보낼 수 없습니다.");
            return this;
        }
        ContrabandGameChatHolder.GameChatRef gameChatRef = contrabandGameChatHolder.get();

        if (gameChatRef != null) {
            gameChatRef.chat.tell(new ChatInRound(command.playerId(), command.playerName(), command.message(), command.currentRound()));
        }

        return this;
    }

    private Behavior<ChatCommand> onRequestSendChat(RequestSendChat command) {
        if (chatBlacklistRepository.isBlocked(command.playerId())) {
            sender.sendExceptionMessage(
                    ExceptionCode.CHAT_USER_BLOCKED,
                    "차단된 사용자입니다. 채팅을 보낼 수 없습니다."
            );
            return this;
        }
        ActorRef<LobbyChatCommand> lobbyChat = lobbyChatHolder.get();
        if (lobbyChat != null) {
            lobbyChat.tell(new SendMessage(command.playerId(), command.playerName(), command.message()));
            return this;
        }

        ContrabandGameChatHolder.GameChatRef gameChatRef = contrabandGameChatHolder.get();
        if (gameChatRef == null) {
            return this;
        }

        if (gameChatRef.teamRole.isSmuggler()) {
            gameChatRef.chat.tell(new ChatSmugglerTeam(command.playerId(), command.playerName(), command.message()));
            return this;
        }
        if (gameChatRef.teamRole.isInspector()) {
            gameChatRef.chat.tell(new ChatInspectorTeam(command.playerId(), command.playerName(), command.message()));
        }
        return this;
    }

    private Behavior<ChatCommand> onPropagateMaskedChatBatch(PropagateMaskedChatBatch command) {
        command.messageIds()
               .forEach(messageId -> sender.sendMaskedChatMessage(messageId, command.chatEvent().name()));
        return this;
    }

    private Behavior<ChatCommand> onTerminated(Terminated signal) {
        if (lobbyChatHolder.isTerminated(signal.getRef())) {
            lobbyChatHolder.clear();
        }
        if (contrabandGameChatHolder.isTerminated(signal.getRef())) {
            contrabandGameChatHolder.clear();
        }
        return this;
    }

    private static class LobbyChatHolder {

        private ActorRef<LobbyChatCommand> ref;

        void set(ActorRef<LobbyChatCommand> ref, ActorContext<ChatCommand> context) {
            this.ref = ref;
            context.watch(ref);
        }

        void clear() {
            this.ref = null;
        }

        ActorRef<LobbyChatCommand> get() {
            return ref;
        }

        boolean isTerminated(ActorRef<?> terminated) {
            return ref != null && ref.equals(terminated);
        }
    }

    private static class ContrabandGameChatHolder {

        private GameChatRef ref;

        void set(ActorRef<ContrabandGameChatCommand> ref, TeamRole teamRole, ActorContext<ChatCommand> context) {
            this.ref = new GameChatRef(ref, teamRole);
            context.watch(ref);
        }

        void clear() {
            this.ref = null;
        }

        GameChatRef get() {
            return ref;
        }

        boolean isTerminated(ActorRef<?> terminated) {
            return ref != null && ref.chat.equals(terminated);
        }

        private record GameChatRef(ActorRef<ContrabandGameChatCommand> chat, TeamRole teamRole) { }
    }

    public record PropagateWelcomeMessage(String playerName) implements ChatCommand { }

    public record PropagateNewMessage(ChatMessage chatMessage) implements ChatCommand { }

    public record PropagateLeftMessage(String playerName) implements ChatCommand { }

    public record PropagateKickedMessage(String playerName) implements ChatCommand { }

    // 단일 채팅 메시지가 차단되었음을 알리기 위한 Actor 메시지
    public record PropagateMaskedChatMessage(Long messageId, ChatEventType chatEvent) implements ChatCommand { }

    // 여러 채팅 메시지가 차단되었음을 알리기 위한 Actor 메시지
    public record PropagateMaskedChatBatch(List<Long> messageIds, ChatEventType chatEvent) implements ChatCommand { }

    public record PropagateSmugglerTeamChat(ChatMessage chatMessage) implements ChatCommand { }

    public record PropagateInspectorTeamChat(ChatMessage chatMessage) implements ChatCommand { }

    public record PropagateRoundChat(ChatMessage chatMessage) implements ChatCommand { }

    public record SyncContrabandGameChat(ActorRef<ContrabandGameChatCommand> chat, TeamRole teamRole) implements ChatCommand { }

    public record ClearContrabandGameChat() implements ChatCommand { }

    public record SyncLobbyChat(ActorRef<LobbyChatCommand> lobbyChat) implements ChatCommand { }

    public record ClearLobbyChat() implements ChatCommand { }

    public record RequestSendTeamChat(Long playerId, String playerName, String message) implements ChatCommand { }

    public record RequestSendRoundChat(Long playerId, String playerName, String message, int currentRound) implements ChatCommand { }

    public record RequestSendChat(Long playerId, String playerName, String message) implements ChatCommand { }
}
