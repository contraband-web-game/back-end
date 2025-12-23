package com.game.contraband.domain.game.round;

public enum RoundStatus {
    NEW,
    SMUGGLE_DECLARED,
    INSPECTION_DECISION_DECLARED,
    INSPECTION_DECIDED;

    public boolean isNew() {
        return this == RoundStatus.NEW;
    }

    public boolean isNotNew() {
        return !isNew();
    }

    public boolean isSmuggleDeclared() {
        return this == RoundStatus.SMUGGLE_DECLARED || this == RoundStatus.INSPECTION_DECIDED;
    }

    public boolean isNotSmuggleDeclared() {
        return !isSmuggleDeclared();
    }

    public boolean isInspectionDecisionDeclared() {
        return this == RoundStatus.INSPECTION_DECISION_DECLARED;
    }

    public boolean isInspectionDecided() {
        return this == RoundStatus.INSPECTION_DECIDED;
    }

    public boolean isNotInspectionDecided() {
        return !isInspectionDecided();
    }
}
