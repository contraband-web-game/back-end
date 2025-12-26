package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class LobbyRuntimeState {

    private final Long roomId;
    private final Long hostId;
    private final String entityId;
    private final Lobby lobby;

    public LobbyRuntimeState(Long roomId, Long hostId, String entityId, Lobby lobby) {
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.hostId = Objects.requireNonNull(hostId, "hostId");
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.lobby = Objects.requireNonNull(lobby, "lobby");
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
}
