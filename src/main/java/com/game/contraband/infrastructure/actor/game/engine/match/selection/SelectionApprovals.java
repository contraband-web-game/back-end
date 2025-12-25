package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class SelectionApprovals {

    private Long smugglerCandidateId;
    private final Set<Long> smugglerApprovals = new HashSet<>();
    private Long inspectorCandidateId;
    private final Set<Long> inspectorApprovals = new HashSet<>();

    void setSmugglerCandidate(Long candidateId) {
        if (!Objects.equals(smugglerCandidateId, candidateId)) {
            smugglerCandidateId = candidateId;
            smugglerApprovals.clear();
        }
    }

    void setInspectorCandidate(Long candidateId) {
        if (!Objects.equals(inspectorCandidateId, candidateId)) {
            inspectorCandidateId = candidateId;
            inspectorApprovals.clear();
        }
    }

    void resetAll() {
        smugglerCandidateId = null;
        inspectorCandidateId = null;
        smugglerApprovals.clear();
        inspectorApprovals.clear();
    }

    void addSmugglerApproval(Long voterId, Long currentCandidateId) {
        if (voterId == null || currentCandidateId == null) {
            return;
        }
        ensureCurrentSmugglerCandidate(currentCandidateId);
        if (Objects.equals(currentCandidateId, voterId)) {
            return;
        }
        smugglerApprovals.add(voterId);
    }

    void addInspectorApproval(Long voterId, Long currentCandidateId) {
        if (voterId == null || currentCandidateId == null) {
            return;
        }
        ensureCurrentInspectorCandidate(currentCandidateId);
        if (Objects.equals(currentCandidateId, voterId)) {
            return;
        }
        inspectorApprovals.add(voterId);
    }

    boolean hasEnoughSmugglerApprovals(Long currentCandidateId, int requiredApprovals) {
        if (!Objects.equals(currentCandidateId, smugglerCandidateId)) {
            return false;
        }
        return smugglerApprovals.size() >= requiredApprovals;
    }

    boolean hasEnoughInspectorApprovals(Long currentCandidateId, int requiredApprovals) {
        if (!Objects.equals(currentCandidateId, inspectorCandidateId)) {
            return false;
        }
        return inspectorApprovals.size() >= requiredApprovals;
    }

    boolean canReplaceSmugglerCandidate(Long nextCandidateId) {
        if (!hasSmugglerApprovals()) {
            return true;
        }
        return Objects.equals(smugglerCandidateId, nextCandidateId);
    }

    boolean canReplaceInspectorCandidate(Long nextCandidateId) {
        if (!hasInspectorApprovals()) {
            return true;
        }
        return Objects.equals(inspectorCandidateId, nextCandidateId);
    }

    boolean hasSmugglerApprovals() {
        return !smugglerApprovals.isEmpty();
    }

    boolean hasInspectorApprovals() {
        return !inspectorApprovals.isEmpty();
    }

    boolean hasSmugglerApprovalFrom(Long voterId) {
        return smugglerApprovals.contains(voterId);
    }

    boolean hasInspectorApprovalFrom(Long voterId) {
        return inspectorApprovals.contains(voterId);
    }

    boolean toggleSmugglerApproval(Long voterId, Long candidateId) {
        if (candidateId == null || voterId == null) {
            return false;
        }
        ensureCurrentSmugglerCandidate(candidateId);
        if (smugglerApprovals.remove(voterId)) {
            return false;
        }
        smugglerApprovals.add(voterId);
        return true;
    }

    boolean toggleInspectorApproval(Long voterId, Long candidateId) {
        if (candidateId == null || voterId == null) {
            return false;
        }
        ensureCurrentInspectorCandidate(candidateId);
        if (inspectorApprovals.remove(voterId)) {
            return false;
        }
        inspectorApprovals.add(voterId);
        return true;
    }

    Long smugglerCandidateId() {
        return smugglerCandidateId;
    }

    Long inspectorCandidateId() {
        return inspectorCandidateId;
    }

    Set<Long> smugglerApprovalsSnapshot() {
        return Set.copyOf(smugglerApprovals);
    }

    Set<Long> inspectorApprovalsSnapshot() {
        return Set.copyOf(inspectorApprovals);
    }

    void ensureCanReplaceSmuggler(Long candidateId) {
        if (!canReplaceSmugglerCandidate(candidateId)) {
            throw new IllegalStateException("이미 찬성이 진행된 후보는 교체할 수 없습니다.");
        }
    }

    void ensureCanReplaceInspector(Long candidateId) {
        if (!canReplaceInspectorCandidate(candidateId)) {
            throw new IllegalStateException("이미 찬성이 진행된 후보는 교체할 수 없습니다.");
        }
    }

    private void ensureCurrentSmugglerCandidate(Long currentCandidateId) {
        if (!Objects.equals(currentCandidateId, smugglerCandidateId)) {
            smugglerCandidateId = currentCandidateId;
            smugglerApprovals.clear();
        }
    }

    private void ensureCurrentInspectorCandidate(Long currentCandidateId) {
        if (!Objects.equals(currentCandidateId, inspectorCandidateId)) {
            inspectorCandidateId = currentCandidateId;
            inspectorApprovals.clear();
    }
}
}
