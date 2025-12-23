package com.game.contraband.domain.game.engine.lobby;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RosterDraft {

    public static RosterDraft create(long id, String name, TeamRole role, int maxTeamSize) {
        if (maxTeamSize < MIN_TEAM_SIZE) {
            throw new IllegalArgumentException("팀 인원은 최소 " + MIN_TEAM_SIZE + "명 이상이어야 합니다.");
        }
        Map<Long, PlayerProfile> players = new LinkedHashMap<>();
        return new RosterDraft(id, name, role, maxTeamSize, players);
    }

    private RosterDraft(long id, String name, TeamRole role, int maxTeamSize, Map<Long, PlayerProfile> players) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.players = players;
        this.maxTeamSize = maxTeamSize;
    }

    private static final int MIN_TEAM_SIZE = 1;

    private final long id;
    private final String name;
    private final TeamRole role;
    private final Map<Long, PlayerProfile> players;
    private int maxTeamSize;

    public void add(PlayerProfile profile) {
        if (profile.isDifferentRole(role)) {
            throw new IllegalArgumentException("잘못된 팀 역할입니다.");
        }
        if (players.size() >= maxTeamSize) {
            throw new IllegalStateException("팀 인원은 최대 " + maxTeamSize + "명까지 가능합니다.");
        }

        players.put(profile.getPlayerId(), profile);
    }

    public void remove(Long playerId) {
        players.remove(playerId);
    }

    public boolean hasPlayer(Long playerId) {
        return players.containsKey(playerId);
    }

    public List<PlayerProfile> players() {
        return List.copyOf(new ArrayList<>(players.values()));
    }

    public boolean hasCapacity() {
        return players.size() < maxTeamSize;
    }

    public void updateMaxTeamSize(int maxTeamSize) {
        if (maxTeamSize < MIN_TEAM_SIZE) {
            throw new IllegalArgumentException("팀 인원은 최소 " + MIN_TEAM_SIZE + "명 이상이어야 합니다.");
        }
        if (maxTeamSize < players.size()) {
            throw new IllegalStateException("팀 인원은 현재 구성보다 작을 수 없습니다.");
        }
        this.maxTeamSize = maxTeamSize;
    }

    public PlayerProfile getPlayer(Long playerId) {
        PlayerProfile profile = players.get(playerId);
        if (profile == null) {
            throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
        }

        return profile;
    }

    public TeamRoster toRoster() {
        if (players.isEmpty()) {
            throw new IllegalStateException("팀 인원은 최소 " + MIN_TEAM_SIZE + "명 이상이어야 합니다.");
        }

        return TeamRoster.create(id, name, role, players());
    }
}
