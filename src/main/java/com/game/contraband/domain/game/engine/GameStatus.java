package com.game.contraband.domain.game.engine;

public enum GameStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED;

    public boolean isStarted() {
        return !this.isNotStarted();
    }

    public boolean isNotStarted() {
        return this == GameStatus.NOT_STARTED;
    }

    public boolean isInProgress() {
        return this == GameStatus.IN_PROGRESS;
    }

    public boolean isNotInProgress() {
        return !this.isInProgress();
    }

    public boolean isFinished() {
        return this == GameStatus.FINISHED;
    }

    public boolean isNotFinished() {
        return !this.isFinished();
    }
}
