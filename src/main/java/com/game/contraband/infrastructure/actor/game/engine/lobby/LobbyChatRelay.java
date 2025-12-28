package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncLobbyChat;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.Shutdown;
import org.apache.pekko.actor.typed.ActorRef;

public class LobbyChatRelay {

    private final LobbyExternalGateway externalGateway;

    public LobbyChatRelay(LobbyExternalGateway externalGateway) {
        this.externalGateway = externalGateway;
    }

    public void syncLobbyChat(ActorRef<ClientSessionCommand> session) {
        session.tell(new SyncLobbyChat(externalGateway.lobbyChat()));
    }

    public void sendToChat(LobbyChatCommand command) {
        externalGateway.sendToLobbyChat(command);
    }

    public void stopChat() {
        externalGateway.sendToLobbyChat(new Shutdown());
    }
}
