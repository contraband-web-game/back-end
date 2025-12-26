package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.global.actor.CborSerializable;
import org.apache.pekko.actor.typed.Behavior;

public class LobbyActor {

    public static Behavior<LobbyCommand> create(
            LobbyRuntimeState lobbyState,
            LobbyClientSessionRegistry sessionRegistry,
            LobbyExternalGateway messageEndpoints,
            LobbyLifecycleCoordinator lifecycleCoordinator,
            LobbyChatRelay chatRelay
    ) {
        // NO-OP
        return null;
    }

    public interface LobbyCommand extends CborSerializable { }

    public record EndGame() implements LobbyCommand { }
}
