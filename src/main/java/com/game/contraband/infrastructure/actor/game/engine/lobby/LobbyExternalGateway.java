package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.GameChatProtocol.GameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.GameChatProtocol.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import java.util.Objects;
import org.apache.pekko.actor.typed.ActorRef;

public class LobbyExternalGateway {

    private final ActorRef<GameChatCommand> lobbyChat;
    private final ActorRef<GameManagerCommand> parent;
    private final ChatMessageEventPublisher chatMessageEventPublisher;
    private final GameLifecycleEventPublisher gameLifecycleEventPublisher;
    private final ChatBlacklistRepository chatBlacklistRepository;

    public LobbyExternalGateway(
            ActorRef<GameChatCommand> lobbyChat,
            ActorRef<GameManagerCommand> parent,
            ChatMessageEventPublisher chatMessageEventPublisher,
            GameLifecycleEventPublisher gameLifecycleEventPublisher,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        this.lobbyChat = Objects.requireNonNull(lobbyChat, "lobbyChat");
        this.parent = Objects.requireNonNull(parent, "parent");
        this.chatMessageEventPublisher = Objects.requireNonNull(chatMessageEventPublisher, "chatMessageEventPublisher");
        this.gameLifecycleEventPublisher = gameLifecycleEventPublisher;
        this.chatBlacklistRepository = Objects.requireNonNull(chatBlacklistRepository, "chatBlacklistRepository");
    }

    public void sendToLobbyChat(LobbyChatCommand command) {
        lobbyChat.tell(command);
    }

    public void notifyParent(GameManagerCommand command) {
        parent.tell(command);
    }

    public ActorRef<GameChatCommand> lobbyChat() {
        return lobbyChat;
    }

    public ChatMessageEventPublisher chatMessageEventPublisher() {
        return chatMessageEventPublisher;
    }

    public GameLifecycleEventPublisher gameLifecycleEventPublisher() {
        return gameLifecycleEventPublisher;
    }

    public ChatBlacklistRepository chatBlacklistRepository() {
        return chatBlacklistRepository;
    }

    public void publishGameStarted(String entityId, Long roomId) {
        if (gameLifecycleEventPublisher == null) {
            return;
        }

        gameLifecycleEventPublisher.publishGameStarted(entityId, roomId);
    }
}
