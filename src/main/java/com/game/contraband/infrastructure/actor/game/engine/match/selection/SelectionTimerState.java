package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.apache.pekko.actor.Cancellable;

class SelectionTimerState {

    private Cancellable selectionTimeoutCancellable;
    private Instant selectionTimerStartedAt;
    private Duration selectionTimerDuration;

    void setSelectionTimeoutCancellable(Cancellable cancellable, Instant startedAt, Duration duration) {
        cancelSelectionTimeout();
        this.selectionTimeoutCancellable = cancellable;
        this.selectionTimerStartedAt = startedAt;
        this.selectionTimerDuration = duration;
    }

    void cancelSelectionTimeout() {
        if (selectionTimeoutCancellable != null && !selectionTimeoutCancellable.isCancelled()) {
            selectionTimeoutCancellable.cancel();
        }
        this.selectionTimeoutCancellable = null;
        this.selectionTimerStartedAt = null;
        this.selectionTimerDuration = null;
    }

    Optional<TimerSnapshot> currentSelectionTimer() {
        if (selectionTimerStartedAt == null || selectionTimerDuration == null) {
            return Optional.empty();
        }
        return Optional.of(new TimerSnapshot(selectionTimerStartedAt, selectionTimerDuration));
    }

    record TimerSnapshot(Instant startedAt, Duration duration) { }
}
