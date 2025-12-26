package com.game.contraband.infrastructure.actor.game.engine.match.round;

import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearContrabandGameChat;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.SyncRoundChatId;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import org.apache.pekko.actor.typed.ActorRef;

public class RoundChatCoordinator {

    private final ActorRef<ContrabandGameChatCommand> gameChat;
    private final ClientSessionRegistry registry;

    public RoundChatCoordinator(
            ActorRef<ContrabandGameChatCommand> gameChat,
            ClientSessionRegistry registry
    ) {
        this.gameChat = gameChat;
        this.registry = registry;
    }

    public void syncRoundChatId(Long smugglerId, Long inspectorId) {
        gameChat.tell(new SyncRoundChatId(smugglerId, inspectorId));
    }

    public void clearRoundChatId() {
        gameChat.tell(new ContrabandGameChatActor.ClearRoundChatId());
    }

    public void clearGameChatForAll() {
        registry.tellAll(new ClearContrabandGameChat());
    }

    public ActorRef<ContrabandGameChatCommand> gameChat() {
        return gameChat;
    }
}
