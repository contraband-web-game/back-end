package com.game.contraband.infrastructure.actor.game.chat.lobby;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.pekko.actor.typed.ActorRef;

public final class LobbyChatParticipants {

    private final Map<Long, ActorRef<ClientSessionCommand>> sessions = new HashMap<>();

    public LobbyChatParticipants(Long hostId, ActorRef<ClientSessionCommand> hostSession) {
        Objects.requireNonNull(hostId, "hostId");
        Objects.requireNonNull(hostSession, "hostSession");
        add(hostId, hostSession);
    }

    public void add(Long playerId, ActorRef<ClientSessionCommand> session) {
        sessions.put(playerId, session);
    }

    public ActorRef<ClientSessionCommand> remove(Long playerId) {
        return sessions.remove(playerId);
    }

    public ActorRef<ClientSessionCommand> get(Long playerId) {
        return sessions.get(playerId);
    }

    public void forEach(Consumer<ActorRef<ClientSessionCommand>> action) {
        sessions.values()
                .forEach(action);
    }
}
