package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.round.InspectionDecision;
import com.game.contraband.domain.game.vo.Money;

public class RoundSettlementRuleSelector {

    public RoundSettlementRule selectRule(
            InspectionDecision inspectionDecision,
            Money smuggleAmount,
            Money claimedAmount
    ) {
        if (inspectionDecision.isPass()) {
            return new PassSettlementRule();
        }
        if (claimedAmount.isGreaterThan(smuggleAmount)) {
            return new InspectionUnderSettlementRule();
        }
        return new InspectionHitSettlementRule();
    }
}
