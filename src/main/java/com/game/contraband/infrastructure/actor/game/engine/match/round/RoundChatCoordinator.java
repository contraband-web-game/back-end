package com.game.contraband.infrastructure.actor.game.engine.match.round;

import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearContrabandGameChat;
import com.game.contraband.infrastructure.actor.game.chat.GameChatProtocol.GameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import org.apache.pekko.actor.typed.ActorRef;

public class RoundChatCoordinator {

    private final ActorRef<GameChatCommand> gameChat;
    private final ClientSessionRegistry registry;

    public RoundChatCoordinator(ActorRef<GameChatCommand> gameChat, ClientSessionRegistry registry) {
        this.gameChat = gameChat;
        this.registry = registry;
    }

    public void clearRoundChatId() {
        gameChat.tell(new ContrabandGameChatActor.ClearRoundChatId());
    }

    public void clearGameChatForAll() {
        registry.tellAll(new ClearContrabandGameChat());
    }

    public ActorRef<GameChatCommand> gameChat() {
        return gameChat;
    }
}
