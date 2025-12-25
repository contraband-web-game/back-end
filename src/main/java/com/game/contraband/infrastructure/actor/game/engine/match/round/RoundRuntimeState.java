package com.game.contraband.infrastructure.actor.game.engine.match.round;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.apache.pekko.actor.Cancellable;

public class RoundRuntimeState {

    private boolean smugglerActionDone;
    private boolean inspectorActionDone;
    private Cancellable roundTimeoutCancellable;
    private Instant roundTimerStartedAt;
    private Duration roundTimerDuration;

    public boolean isSmugglerActionNotDone() {
        return !smugglerActionDone;
    }

    public boolean isInspectorActionNotDone() {
        return !inspectorActionDone;
    }

    public void markSmugglerActionDone() {
        this.smugglerActionDone = true;
    }

    public void markInspectorActionDone() {
        this.inspectorActionDone = true;
    }

    public void resetActions() {
        this.smugglerActionDone = false;
        this.inspectorActionDone = false;
    }

    public void setRoundTimeoutCancellable(Cancellable cancellable, Instant startedAt, Duration duration) {
        cancelRoundTimeout();
        this.roundTimeoutCancellable = cancellable;
        this.roundTimerStartedAt = startedAt;
        this.roundTimerDuration = duration;
    }

    public void cancelRoundTimeout() {
        if (roundTimeoutCancellable != null && !roundTimeoutCancellable.isCancelled()) {
            roundTimeoutCancellable.cancel();
        }
        this.roundTimeoutCancellable = null;
        this.roundTimerStartedAt = null;
        this.roundTimerDuration = null;
    }

    public Optional<TimerSnapshot> currentRoundTimer() {
        if (roundTimerStartedAt == null || roundTimerDuration == null) {
            return Optional.empty();
        }

        return Optional.of(new TimerSnapshot(roundTimerStartedAt, roundTimerDuration));
    }

    public record TimerSnapshot(Instant startedAt, Duration duration) { }
}
