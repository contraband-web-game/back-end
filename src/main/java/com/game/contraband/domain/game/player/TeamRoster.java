package com.game.contraband.domain.game.player;

import java.util.List;
import lombok.Getter;

@Getter
public class TeamRoster {

    public static TeamRoster create(String name, TeamRole role, List<PlayerProfile> players) {
        return new TeamRoster(name, role, List.copyOf(players));
    }

    private TeamRoster(String name, TeamRole role, List<PlayerProfile> players) {
        this.name = name;
        this.role = role;
        this.players = players;
    }

    private final String name;
    private final TeamRole role;
    private final List<PlayerProfile> players;

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
