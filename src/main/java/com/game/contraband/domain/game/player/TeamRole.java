package com.game.contraband.domain.game.player;

public enum TeamRole {
    SMUGGLER, INSPECTOR;

    public boolean isSmuggler() {
        return this == TeamRole.SMUGGLER;
    }

    public boolean isInspector() {
        return this == TeamRole.INSPECTOR;
    }
}
