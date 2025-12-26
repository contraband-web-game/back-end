package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pekko.actor.typed.ActorRef;

public class LobbyClientSessionRegistry {

    private final Map<Long, ActorRef<ClientSessionCommand>> sessions;

    public LobbyClientSessionRegistry(Map<Long, ActorRef<ClientSessionCommand>> seed) {
        this.sessions = seed == null ? new HashMap<>() : new HashMap<>(seed);
    }

    public ActorRef<ClientSessionCommand> get(Long playerId) {
        return sessions.get(playerId);
    }

    public ActorRef<ClientSessionCommand> add(Long playerId, ActorRef<ClientSessionCommand> session) {
        return sessions.put(playerId, session);
    }

    public ActorRef<ClientSessionCommand> remove(Long playerId) {
        return sessions.remove(playerId);
    }

    public int size() {
        return sessions.size();
    }

    public Iterable<ActorRef<ClientSessionCommand>> values() {
        return sessions.values();
    }

    public void forEachSession(Consumer<ActorRef<ClientSessionCommand>> action) {
        sessions.values()
                .forEach(action);
    }

    public Map<Long, ActorRef<ClientSessionCommand>> asMapView() {
        return Map.copyOf(sessions);
    }
}
