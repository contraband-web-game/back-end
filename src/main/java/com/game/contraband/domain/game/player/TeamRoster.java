package com.game.contraband.domain.game.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class TeamRoster {

    public static TeamRoster create(Long id, String name, TeamRole role, List<PlayerProfile> players) {
        return new TeamRoster(id, name, role, List.copyOf(players));
    }

    private TeamRoster(Long id, String name, TeamRole role, List<PlayerProfile> players) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.players = players;
    }

    private final Long id;
    private final String name;
    private final TeamRole role;
    private final List<PlayerProfile> players;

    public TeamRoster addPlayer(PlayerProfile profile) {
        if (profile.isDifferentRole(role)) {
            throw new IllegalArgumentException("플레이어 역할이 로스터와 일치하지 않습니다.");
        }
        if (this.hasPlayer(profile.getPlayerId())) {
            throw new IllegalArgumentException("이미 로스터에 포함된 플레이어입니다.");
        }

        List<PlayerProfile> updated = new ArrayList<>(players);
        updated.add(profile);

        return new TeamRoster(id, name, role, updated);
    }

    public TeamRoster removePlayer(Long playerId) {
        if (this.lacksPlayer(playerId)) {
            throw new IllegalArgumentException("로스터에 존재하지 않는 플레이어입니다.");
        }

        List<PlayerProfile> updated = players.stream()
                                             .filter(profile -> !profile.isEqualId(playerId))
                                             .toList();

        return new TeamRoster(id, name, role, updated);
    }

    public boolean isSmugglerTeam() {
        return role == TeamRole.SMUGGLER;
    }

    public boolean isInspectorTeam() {
        return role == TeamRole.INSPECTOR;
    }

    public boolean hasPlayer(Long playerId) {
        return players.stream()
                      .anyMatch(profile -> profile.isEqualId(playerId));
    }

    public boolean lacksPlayer(Long playerId) {
        return !this.hasPlayer(playerId);
    }
}
