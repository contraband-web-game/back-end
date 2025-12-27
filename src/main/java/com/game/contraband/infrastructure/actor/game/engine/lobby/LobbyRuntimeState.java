package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
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

    public boolean canAddToLobby() {
        return lobby.canAddToLobby();
    }

    public boolean cannotAddToLobby() {
        return !canAddToLobby();
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

    public int lobbyMaxPlayerCount() {
        return lobby.getMaxPlayerCount();
    }

    public String lobbyName() {
        return lobby.getName();
    }

    public void changeMaxPlayerCount(int newMaxPlayerCount, Long executorId) {
        lobby.changeMaxPlayerCount(newMaxPlayerCount, executorId);
    }

    public void toggleReady(Long playerId) {
        lobby.toggleReady(playerId);
    }

    public boolean readyStateOf(Long playerId) {
        return lobby.getReadyStates().getOrDefault(playerId, false);
    }

    public void toggleTeam(Long playerId) {
        lobby.toggleTeam(playerId);
    }

    public PlayerProfile kick(Long executorId, Long targetPlayerId) {
        return lobby.kick(executorId, targetPlayerId);
    }

    public void deleteLobby(Long executorId) {
        lobby.deleteLobby(executorId);
    }

    public ContrabandGame startGame(int totalRounds, Long executorId) {
        return lobby.startGame(totalRounds, executorId);
    }
}
