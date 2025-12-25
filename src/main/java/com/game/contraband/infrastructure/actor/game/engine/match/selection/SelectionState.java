package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.actor.Cancellable;

public class SelectionState {

    private final SelectionApprovals approvals = new SelectionApprovals();
    private final SelectionTimerState timerState = new SelectionTimerState();
    private int currentRound = 1;
    private Long smugglerId;
    private boolean fixedSmugglerId;
    private Long inspectorId;
    private boolean fixedInspectorId;

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

    public boolean isReady() {
        return fixedInspectorId && fixedSmugglerId;
    }

    public boolean isRoundNotReady() {
        return !isReady();
    }

    public boolean isDifferentRound(int round) {
        return this.currentRound != round;
    }

    public boolean isNotSameSmuggler(Long targetId) {
        return smugglerId == null || !smugglerId.equals(targetId);
    }

    public boolean isNotSameInspector(Long targetId) {
        return inspectorId == null || !inspectorId.equals(targetId);
    }

    public void registerSmugglerId(int currentRound, Long candidateId) {
        ensureCurrentRound(currentRound);
        ensureSmugglerNotFixed();
        approvals.ensureCanReplaceSmuggler(candidateId);
        this.smugglerId = candidateId;
        approvals.setSmugglerCandidate(candidateId);
    }

    public void registerInspectorId(int currentRound, Long candidateId) {
        ensureCurrentRound(currentRound);
        ensureInspectorNotFixed();
        approvals.ensureCanReplaceInspector(candidateId);
        this.inspectorId = candidateId;
        approvals.setInspectorCandidate(candidateId);
    }

    public void registerSmugglerId(Long candidateId) {
        registerSmugglerId(currentRound, candidateId);
    }

    public void registerInspectorId(Long candidateId) {
        registerInspectorId(currentRound, candidateId);
    }

    public void fixSmugglerId() {
        this.fixedSmugglerId = true;
    }

    public void fixInspectorId() {
        this.fixedInspectorId = true;
    }

    public void seed(Long smugglerId, Long inspectorId) {
        this.smugglerId = smugglerId;
        this.inspectorId = inspectorId;
        this.fixedSmugglerId = true;
        this.fixedInspectorId = true;
        approvals.resetAll();
    }

    public void prepareNextRound() {
        this.smugglerId = null;
        this.fixedSmugglerId = false;
        this.inspectorId = null;
        this.fixedInspectorId = false;
        this.currentRound++;
        approvals.resetAll();
        timerState.cancelSelectionTimeout();
    }

    public boolean isSmugglerReady() {
        return smugglerId != null;
    }

    public boolean isInspectorReady() {
        return inspectorId != null;
    }

    public boolean isSmugglerFixed() {
        return fixedSmugglerId;
    }

    public boolean isInspectorFixed() {
        return fixedInspectorId;
    }

    public Optional<SelectionTimerState.TimerSnapshot> currentSelectionTimer() {
        return timerState.currentSelectionTimer();
    }

    public void initSelectionTimeout(Cancellable cancellable, Instant startedAt, Duration duration) {
        timerState.setSelectionTimeoutCancellable(cancellable, startedAt, duration);
    }

    public void cancelSelectionTimeout() {
        timerState.cancelSelectionTimeout();
    }

    public void addSmugglerApproval(Long voterId) {
        approvals.addSmugglerApproval(voterId, smugglerId);
    }

    public void addInspectorApproval(Long voterId) {
        approvals.addInspectorApproval(voterId, inspectorId);
    }

    public boolean hasEnoughSmugglerApprovals(int requiredApprovals) {
        return approvals.hasEnoughSmugglerApprovals(smugglerId, requiredApprovals);
    }

    public boolean hasEnoughInspectorApprovals(int requiredApprovals) {
        return approvals.hasEnoughInspectorApprovals(inspectorId, requiredApprovals);
    }

    public boolean hasSmugglerApprovalFrom(Long voterId) {
        return approvals.hasSmugglerApprovalFrom(voterId);
    }

    public boolean hasInspectorApprovalFrom(Long voterId) {
        return approvals.hasInspectorApprovalFrom(voterId);
    }

    public boolean toggleSmugglerApproval(Long voterId) {
        return approvals.toggleSmugglerApproval(voterId, smugglerId);
    }

    public boolean toggleInspectorApproval(Long voterId) {
        return approvals.toggleInspectorApproval(voterId, inspectorId);
    }

    public Long smugglerCandidateId() {
        return approvals.smugglerCandidateId();
    }

    public Long inspectorCandidateId() {
        return approvals.inspectorCandidateId();
    }

    public Set<Long> smugglerApprovalsSnapshot() {
        return approvals.smugglerApprovalsSnapshot();
    }

    public Set<Long> inspectorApprovalsSnapshot() {
        return approvals.inspectorApprovalsSnapshot();
    }

    private void ensureCurrentRound(int round) {
        if (this.currentRound != round) {
            throw new IllegalArgumentException("현재 진행중인 라운드가 아닙니다.");
        }
    }

    private void ensureSmugglerNotFixed() {
        if (fixedSmugglerId) {
            throw new IllegalArgumentException("이미 이번 라운드에 참여할 밀수꾼이 확정되었습니다.");
        }
    }

    private void ensureInspectorNotFixed() {
        if (fixedInspectorId) {
            throw new IllegalArgumentException("이미 이번 라운드에 참여할 검사관이 확정되었습니다.");
        }
    }
}
