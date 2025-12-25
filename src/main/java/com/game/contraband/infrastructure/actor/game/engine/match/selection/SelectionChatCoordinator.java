package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.SyncRoundChatId;
import org.apache.pekko.actor.typed.ActorRef;

public class SelectionChatCoordinator {

    private final ActorRef<ContrabandGameChatCommand> gameChat;

    public SelectionChatCoordinator(ActorRef<ContrabandGameChatCommand> gameChat) {
        this.gameChat = gameChat;
    }

    void syncRoundChatId(Long smugglerId, Long inspectorId) {
        gameChat.tell(new SyncRoundChatId(smugglerId, inspectorId));
    }
}
