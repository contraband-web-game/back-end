package com.game.contraband.domain.game.engine.lobby;

public enum LobbyPhase {
    LOBBY,
    IN_PROGRESS,
    FINISHED;

    public boolean isLobby() {
        return this == LobbyPhase.LOBBY;
    }

    public boolean isNotLobby() {
        return !this.isLobby();
    }

    public boolean isFinished() {
        return this == LobbyPhase.FINISHED;
    }
}
