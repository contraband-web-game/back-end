package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import com.game.contraband.infrastructure.actor.game.chat.GameChatProtocol.GameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.SyncRoundChatId;
import org.apache.pekko.actor.typed.ActorRef;

public class SelectionChatCoordinator {

    private final ActorRef<GameChatCommand> gameChat;

    public SelectionChatCoordinator(ActorRef<GameChatCommand> gameChat) {
        this.gameChat = gameChat;
    }

    void syncRoundChatId(Long smugglerId, Long inspectorId) {
        gameChat.tell(new SyncRoundChatId(smugglerId, inspectorId));
    }
}
