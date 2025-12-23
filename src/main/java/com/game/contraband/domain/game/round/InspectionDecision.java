package com.game.contraband.domain.game.round;

public enum InspectionDecision {
    NONE,
    PASS,
    INSPECTION;

    public boolean isNone() {
        return this == InspectionDecision.NONE;
    }

    public boolean isNotNone() {
        return !this.isNone();
    }

    public boolean isPass() {
        return this == InspectionDecision.PASS;
    }

    public boolean isNotPass() {
        return !this.isPass();
    }

    public boolean isInspection() {
        return this == InspectionDecision.INSPECTION;
    }

    public boolean isNotInspection() {
        return !this.isInspection();
    }
}
