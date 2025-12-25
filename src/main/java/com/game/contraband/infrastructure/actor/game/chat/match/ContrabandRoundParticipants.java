package com.game.contraband.infrastructure.actor.game.chat.match;

import java.util.Objects;

public class ContrabandRoundParticipants {

    private int currentRound = 1;
    private Long smugglerId;
    private Long inspectorId;

    public boolean cannotRoundChat() {
        return smugglerId == null || inspectorId == null;
    }

    public void registerSmuggler(Long smugglerId) {
        this.smugglerId = Objects.requireNonNull(smugglerId, "smugglerId");
    }

    public void registerInspector(Long inspectorId) {
        this.inspectorId = Objects.requireNonNull(inspectorId, "inspectorId");
    }

    public boolean isNotRoundParticipant(Long playerId) {
        return !isSmuggler(playerId) && !isInspector(playerId);
    }

    public boolean isRoundMismatch(int round) {
        return this.currentRound != round;
    }

    public boolean isSmuggler(Long playerId) {
        return smugglerId != null && smugglerId.equals(playerId);
    }

    public boolean isInspector(Long playerId) {
        return inspectorId != null && inspectorId.equals(playerId);
    }

    public Long smugglerId() {
        return smugglerId;
    }

    public Long inspectorId() {
        return inspectorId;
    }

    public void clear() {
        this.smugglerId = null;
        this.inspectorId = null;
        this.currentRound++;
    }

    public int currentRound() {
        return currentRound;
    }
}
