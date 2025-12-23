package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.vo.Money;

public class InspectionHitSettlementRule implements RoundSettlementRule {

    @Override
    public RoundSettlement settle(Player smuggler, Player inspector, Money smuggleAmount, Money claimedAmount) {
        Player updatedInspector = inspector.plusBalance(claimedAmount);

        return new RoundSettlement(smuggler, updatedInspector, RoundOutcomeType.INSPECTION_HIT);
    }
}

