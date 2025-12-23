package com.game.contraband.domain.game.round.vo;

import com.game.contraband.domain.game.round.InspectionDecision;
import com.game.contraband.domain.game.vo.Money;
import lombok.Getter;

@Getter
public class InspectionState {

    public static InspectionState initial(Long inspectorId) {
        return new InspectionState(InspectionDecision.NONE, Money.ZERO, false, inspectorId);
    }

    private InspectionState(InspectionDecision decision, Money threshold, boolean provided, Long inspectorId) {
        this.decision = decision;
        this.threshold = threshold;
        this.provided = provided;
        this.inspectorId = inspectorId;
    }

    private final InspectionDecision decision;
    private final Money threshold;
    private final boolean provided;
    private final Long inspectorId;

    public InspectionState decidePass(Long inspectorId) {
        if (provided) {
            throw new IllegalStateException("검사관의 선택은 한 번만 할 수 있습니다.");
        }
        return new InspectionState(InspectionDecision.PASS, Money.ZERO, true, inspectorId);
    }

    public InspectionState decideInspection(Long inspectorId, Money threshold) {
        if (provided) {
            throw new IllegalStateException("검사관의 선택은 한 번만 할 수 있습니다.");
        }
        return new InspectionState(InspectionDecision.INSPECTION, threshold, true, inspectorId);
    }

    public boolean isDecisionNone() {
        return decision.isNone();
    }
}
