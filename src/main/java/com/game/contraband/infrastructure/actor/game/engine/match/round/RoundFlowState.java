package com.game.contraband.infrastructure.actor.game.engine.match.round;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.apache.pekko.actor.Cancellable;

public class RoundFlowState {

    private int currentRound = 1;
    private Long smugglerId;
    private Long inspectorId;
    private final RoundRuntimeState runtimeState = new RoundRuntimeState();

    public void assignRound(Long smugglerId, Long inspectorId, int round) {
        this.currentRound = round;
        this.smugglerId = smugglerId;
        this.inspectorId = inspectorId;
        runtimeState.resetActions();
    }

    public int currentRound() {
        return currentRound;
    }

    public int nextRound() {
        return currentRound + 1;
    }

    public Long smugglerId() {
        return smugglerId;
    }

    public Long inspectorId() {
        return inspectorId;
    }

    public boolean isDifferentRound(int round) {
        return this.currentRound != round;
    }

    public boolean isNotCurrentSmuggler(Long targetSmugglerId) {
        return smugglerId == null || !smugglerId.equals(targetSmugglerId);
    }

    public boolean isNotCurrentInspector(Long targetInspectorId) {
        return inspectorId == null || !inspectorId.equals(targetInspectorId);
    }

    public boolean isSmugglerActionNotDone() {
        return runtimeState.isSmugglerActionNotDone();
    }

    public boolean isInspectorActionNotDone() {
        return runtimeState.isInspectorActionNotDone();
    }

    public void markSmugglerActionDone() {
        runtimeState.markSmugglerActionDone();
    }

    public void markInspectorActionDone() {
        runtimeState.markInspectorActionDone();
    }

    public void initRoundTimeout(Cancellable cancellable, Instant startedAt, Duration duration) {
        runtimeState.setRoundTimeoutCancellable(cancellable, startedAt, duration);
    }

    public void cancelRoundTimeout() {
        runtimeState.cancelRoundTimeout();
    }

    public Optional<RoundRuntimeState.TimerSnapshot> currentRoundTimer() {
        return runtimeState.currentRoundTimer();
    }

    public void resetAfterFinish() {
        this.runtimeState.resetActions();
        this.runtimeState.cancelRoundTimeout();
        this.smugglerId = null;
        this.inspectorId = null;
    }
}
