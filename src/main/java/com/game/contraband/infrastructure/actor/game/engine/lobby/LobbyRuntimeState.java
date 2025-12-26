package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import java.util.Map;
import java.util.Objects;
import org.apache.pekko.actor.typed.ActorRef;

public class LobbyRuntimeState {

    private final Long roomId;
    private final Long hostId;
    private final String entityId;
    private final Lobby lobby;
    private final LobbyClientSessionRegistry clientSessions;

    public LobbyRuntimeState(
            Long roomId,
            Long hostId,
            String entityId,
            Lobby lobby,
            Map<Long, ActorRef<ClientSessionCommand>> clientSessions
    ) {
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.hostId = Objects.requireNonNull(hostId, "hostId");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
        this.clientSessions = new LobbyClientSessionRegistry(clientSessions);
    }

    public Long roomId() {
        return roomId;
    }

    public Long hostId() {
        return hostId;
    }

    public String entityId() {
        return entityId;
    }

    public Lobby lobby() {
        return lobby;
    }

    public LobbyClientSessionRegistry clientSessions() {
        return clientSessions;
    }

    public ActorRef<ClientSessionCommand> clientSession(Long playerId) {
        return clientSessions.get(playerId);
    }

    public ActorRef<ClientSessionCommand> addClientSession(Long playerId, ActorRef<ClientSessionCommand> session) {
        return clientSessions.add(playerId, session);
    }

    public ActorRef<ClientSessionCommand> removeClientSession(Long playerId) {
        return clientSessions.remove(playerId);
    }

    public int clientSessionCount() {
        return clientSessions.size();
    }

    public Iterable<ActorRef<ClientSessionCommand>> clientSessionValues() {
        return clientSessions.values();
    }
}
