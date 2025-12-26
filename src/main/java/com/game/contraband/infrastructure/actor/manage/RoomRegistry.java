package com.game.contraband.infrastructure.actor.manage;

import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;

public class RoomRegistry {

    private final Map<Long, ActorRef<LobbyCommand>> rooms = new HashMap<>();

    public ActorRef<LobbyCommand> get(Long roomId) {
        return rooms.get(roomId);
    }

    public ActorRef<LobbyCommand> add(Long roomId, ActorRef<LobbyCommand> lobby) {
        return rooms.put(roomId, lobby);
    }

    public ActorRef<LobbyCommand> remove(Long roomId) {
        return rooms.remove(roomId);
    }
}
