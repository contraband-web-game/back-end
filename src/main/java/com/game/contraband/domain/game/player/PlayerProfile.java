package com.game.contraband.domain.game.player;

import com.game.contraband.domain.game.vo.Money;
import lombok.Getter;

@Getter
public class PlayerProfile {

    public static PlayerProfile create(Long id, String name, TeamRole teamRole) {
        return new PlayerProfile(id, name, teamRole);
    }

    private PlayerProfile(Long playerId, String name, TeamRole teamRole) {
        this.playerId = playerId;
        this.name = name;
        this.teamRole = teamRole;
    }

    private final Long playerId;
    private final String name;
    private final TeamRole teamRole;

    public Player toPlayer(Money startingMoney) {
        return Player.create(this.playerId, this.name, this.teamRole, startingMoney);
    }

    public PlayerProfile withTeamRole(TeamRole newTeamRole) {
        if (newTeamRole == null) {
            throw new IllegalArgumentException("팀 역할은 비어 있을 수 없습니다.");
        }
        if (this.teamRole == newTeamRole) {
            return this;
        }

        return new PlayerProfile(playerId, name, newTeamRole);
    }


    public boolean isSmugglerTeam() {
        return teamRole.isSmuggler();
    }

    public boolean isInspectorTeam() {
        return teamRole.isInspector();
    }

    public boolean isEqualId(Long id) {
        return this.playerId.equals(id);
    }

    public boolean isSameRole(TeamRole teamRole) {
        return this.teamRole == teamRole;
    }

    public boolean isDifferentRole(TeamRole teamRole) {
        return !this.isSameRole(teamRole);
    }
}
