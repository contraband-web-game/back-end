package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
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
            java.util.Map<Long, ActorRef<ClientSessionCommand>> clientSessions
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

    public boolean isHost(Long playerId) {
        return hostId.equals(playerId);
    }

    public boolean isNotHost(Long playerId) {
        return !this.isHost(playerId);
    }

    public List<LobbyParticipant> lobbyParticipants() {
        Stream<PlayerProfile> smugglerPlayers = lobby.getSmugglerDraft().stream();
        Stream<PlayerProfile> inspectorPlayers = lobby.getInspectorDraft().stream();

        return Stream.concat(smugglerPlayers, inspectorPlayers)
                     .map(profile -> new LobbyParticipant(
                             profile.getPlayerId(),
                             profile.getName(),
                             profile.getTeamRole(),
                             lobby.getReadyStates().getOrDefault(profile.getPlayerId(), false)
                     ))
                     .toList();
    }

    public PlayerProfile findPlayerProfile(Long playerId) {
        return lobby.findPlayerProfile(playerId);
    }

    public void removePlayer(Long playerId) {
        lobby.removePlayer(playerId);
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
