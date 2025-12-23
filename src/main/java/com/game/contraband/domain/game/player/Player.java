package com.game.contraband.domain.game.player;

import com.game.contraband.domain.game.vo.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "id")
public class Player {

    public static Player create(Long id, String name, TeamRole teamRole, Money balance) {
        return new Player(id, name, teamRole, balance);
    }

    private Player(Long id, String name, TeamRole teamRole, Money balance) {
        this.id = id;
        this.name = name;
        this.teamRole = teamRole;
        this.balance = balance;
    }

    private final Long id;
    private final String name;
    private final TeamRole teamRole;
    private final Money balance;

    public boolean canTransfer(Money amount) {
        return balance.isGreaterThanOrEqual(amount);
    }

    public boolean cannotTransfer(Money amount) {
        return !this.canTransfer(amount);
    }

    public Player withBalance(Money newBalance) {
        return new Player(this.id, this.name, this.teamRole, newBalance);
    }

    public Player plusBalance(Money amount) {
        return withBalance(this.balance.plus(amount));
    }

    public Player minusBalance(Money amount) {
        return withBalance(this.balance.minus(amount));
    }

    public boolean isSmugglerTeam() {
        return teamRole.isSmuggler();
    }

    public boolean isInspectorTeam() {
        return teamRole.isInspector();
    }

    public boolean isEqualId(Long id) {
        return this.id.equals(id);
    }

    public boolean isNotEqualId(Long id) {
        return !isEqualId(id);
    }
}
