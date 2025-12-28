package com.game.contraband.domain.game.engine.lobby;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRoster;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lobby {

    public static Lobby create(Long id, String name, PlayerProfile hostProfile, int maxPlayerCount) {
        validateHostProfile(hostProfile);

        LobbyMetadata metadata = LobbyMetadata.create(id, name, hostProfile.getPlayerId(), maxPlayerCount);
        int maxTeamSize = metadata.maxTeamSize();
        RosterDrafts teamDrafts = RosterDrafts.create(maxTeamSize);
        Map<Long, Boolean> readyStates = new HashMap<>();
        LobbyGuards guards = LobbyGuards.create();
        LobbyLifeCycle lifeCycle = LobbyLifeCycle.create(guards);

        Lobby lobby = new Lobby(
                metadata,
                teamDrafts,
                readyStates,
                lifeCycle,
                guards
        );

        addHostProfileToTeam(lobby, hostProfile);
        return lobby;
    }

    private static void validateHostProfile(PlayerProfile hostProfile) {
        if (hostProfile == null) {
            throw new IllegalArgumentException("방장은 필수입니다.");
        }
    }

    private static void addHostProfileToTeam(Lobby lobby, PlayerProfile hostProfile) {
        if (hostProfile.isSmugglerTeam()) {
            lobby.addSmuggler(hostProfile);
            return;
        }

        lobby.addInspector(hostProfile);
    }

    private LobbyMetadata metadata;
    private final LobbyLifeCycle lifeCycle;
    private final RosterDrafts teamDrafts;
    private final Map<Long, Boolean> readyStates;
    private final LobbyGuards guards;

    private Lobby(
            LobbyMetadata metadata,
            RosterDrafts teamDrafts,
            Map<Long, Boolean> readyStates,
            LobbyLifeCycle lifeCycle,
            LobbyGuards guards
    ) {
        this.metadata = metadata;
        this.teamDrafts = teamDrafts;
        this.readyStates = readyStates;
        this.lifeCycle = lifeCycle;
        this.guards = guards;
    }

    public void addSmuggler(PlayerProfile profile) {
        lifeCycle.requireLobbyPhase();
        validatePlayerNotReady(profile.getPlayerId());
        validateLobbyCapacity(profile);
        teamDrafts.addSmuggler(profile);
        readyStates.putIfAbsent(profile.getPlayerId(), false);
    }

    public void addInspector(PlayerProfile profile) {
        lifeCycle.requireLobbyPhase();
        validatePlayerNotReady(profile.getPlayerId());
        validateLobbyCapacity(profile);
        teamDrafts.addInspector(profile);
        readyStates.putIfAbsent(profile.getPlayerId(), false);
    }

    public void removeSmuggler(Long playerId) {
        lifeCycle.requireLobbyPhase();
        validatePlayerNotReady(playerId);
        teamDrafts.removeSmuggler(playerId);
        readyStates.remove(playerId);
    }

    public void removeInspector(Long playerId) {
        lifeCycle.requireLobbyPhase();
        validatePlayerNotReady(playerId);
        teamDrafts.removeInspector(playerId);
        readyStates.remove(playerId);
    }

    public void removePlayer(Long playerId) {
        lifeCycle.requireLobbyPhase();

        if (metadata.isHost(playerId)) {
            throw new IllegalStateException("방장은 나갈 수 없습니다.");
        }
        if (teamDrafts.doesNotHavePlayer(playerId)) {
            throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
        }

        teamDrafts.removePlayer(playerId);
        readyStates.remove(playerId);
    }

    public void toggleReady(Long playerId) {
        lifeCycle.requireLobbyPhase();

        if (teamDrafts.doesNotHavePlayer(playerId)) {
            throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
        }

        boolean current = readyStates.getOrDefault(playerId, false);

        readyStates.put(playerId, !current);
    }

    public void toggleTeam(Long playerId) {
        lifeCycle.requireLobbyPhase();
        validatePlayerNotReady(playerId);

        if (teamDrafts.doesNotHavePlayer(playerId)) {
            throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
        }

        teamDrafts.toggleTeam(playerId);
        readyStates.putIfAbsent(playerId, false);
    }

    public PlayerProfile findPlayerProfile(Long playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
        }

        return teamDrafts.getPlayer(playerId);
    }

    public void deleteLobby(Long executorId) {
        lifeCycle.requireLobbyPhase();
        guards.requireHost(executorId, metadata.getHostId());

        readyStates.clear();
        lifeCycle.finishFromLobby();
    }

    public ContrabandGame startGame(int totalRounds, Long executorId) {
        lifeCycle.requireLobbyPhase();
        guards.requireHost(executorId, metadata.getHostId());

        int smugglerSize = teamDrafts.smugglerPlayers().size();
        int inspectorSize = teamDrafts.inspectorPlayers().size();

        if (smugglerSize != inspectorSize) {
            throw new IllegalStateException("두 팀의 인원 수가 같아야 게임을 시작할 수 있습니다.");
        }
        if (!areAllReady()) {
            throw new IllegalStateException("모든 플레이어가 준비 완료(ready) 상태여야 게임을 시작할 수 있습니다.");
        }

        TeamRoster smugglerRoster = teamDrafts.smugglerRoster();
        TeamRoster inspectorRoster = teamDrafts.inspectorRoster();
        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, totalRounds);

        lifeCycle.start(game);

        return game;
    }

    public ContrabandGame currentGame() {
        return lifeCycle.currentGame();
    }

    public void markFinishedIfDone() {
        lifeCycle.markFinishedIfDone();
    }

    private void validatePlayerNotReady(Long playerId) {
        if (Boolean.TRUE.equals(readyStates.getOrDefault(playerId, false))) {
            throw new IllegalStateException("ready 상태에서는 팀을 변경할 수 없습니다.");
        }
    }

    private void validateLobbyCapacity(PlayerProfile profile) {
        if (isPlayerInLobby(profile.getPlayerId())) {
            throw new IllegalStateException("로비에 이미 존재하는 플레이어입니다.");
        }

        if (cannotAddToLobby()) {
            throw new IllegalStateException("로비에 더 이상 플레이어를 추가할 수 없습니다.");
        }
    }

    public void changeMaxPlayerCount(int newMaxPlayerCount, Long executorId) {
        lifeCycle.requireLobbyPhase();
        guards.requireHost(executorId, metadata.getHostId());

        if (newMaxPlayerCount < totalPlayerCount()) {
            throw new IllegalStateException("새 최대 인원은 현재 참가자 수보다 작을 수 없습니다.");
        }

        int newTeamSize = calculateNewTeamSize(newMaxPlayerCount);
        if (!teamDrafts.canResizeTo(newTeamSize)) {
            throw new IllegalStateException("새 최대 팀 정원은 현재 팀 구성보다 작을 수 없습니다.");
        }

        teamDrafts.updateMaxTeamSize(newTeamSize);
        this.metadata = metadata.withMaxPlayerCount(newMaxPlayerCount);
    }

    public PlayerProfile kick(Long executorId, Long targetPlayerId) {
        lifeCycle.requireLobbyPhase();
        guards.requireHost(executorId, metadata.getHostId(), "방장만 강퇴할 수 있습니다.");

        PlayerProfile targetProfile = teamDrafts.getPlayer(targetPlayerId);

        teamDrafts.removePlayer(targetPlayerId);
        readyStates.remove(targetPlayerId);

        return targetProfile;
    }

    private boolean areAllReady() {
        if (readyStates.isEmpty()) {
            return false;
        }

        return readyStates.values()
                          .stream()
                          .allMatch(Boolean::booleanValue);
    }

    public boolean canAddSmuggler(Long playerId) {
        if (playerId == null) {
            return false;
        }

        if (cannotAddToLobby()) {
            return false;
        }

        return teamDrafts.canAddSmuggler(playerId);
    }

    public boolean canAddInspector(Long playerId) {
        if (playerId == null) {
            return false;
        }

        if (cannotAddToLobby()) {
            return false;
        }

        return teamDrafts.canAddInspector(playerId);
    }

    public boolean canAddToLobby() {
        if (!lifeCycle.isLobbyPhase()) {
            return false;
        }

        return totalPlayerCount() < metadata.getMaxPlayerCount();
    }

    public boolean cannotAddToLobby() {
        return !canAddToLobby();
    }

    public Map<Long, Boolean> getReadyStates() {
        return Map.copyOf(readyStates);
    }

    public List<PlayerProfile> getSmugglerDraft() {
        if (lifeCycle.isLobbyPhase()) {
            return teamDrafts.smugglerPlayers();
        }

        return currentGame().getSmugglerDraft();
    }

    public List<PlayerProfile> getInspectorDraft() {
        if (lifeCycle.isLobbyPhase()) {
            return teamDrafts.inspectorPlayers();
        }

        return currentGame().getInspectorDraft();
    }

    private int totalPlayerCount() {
        return teamDrafts.totalPlayerCount();
    }

    private boolean isPlayerInLobby(Long playerId) {
        return teamDrafts.hasPlayer(playerId);
    }

    private int calculateNewTeamSize(int newMaxPlayerCount) {
        LobbyMetadata updatedMetadata = metadata.withMaxPlayerCount(newMaxPlayerCount);
        return updatedMetadata.maxTeamSize();
    }

    public Long getId() {
        return metadata.getId();
    }

    public String getName() {
        return metadata.getName();
    }

    public Long getHostId() {
        return metadata.getHostId();
    }

    public int getMaxPlayerCount() {
        return metadata.getMaxPlayerCount();
    }

    public LobbyPhase getPhase() {
        return lifeCycle.getPhase();
    }
}
