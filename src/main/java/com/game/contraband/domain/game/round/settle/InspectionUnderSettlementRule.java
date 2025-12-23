package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.vo.Money;

public class InspectionUnderSettlementRule implements RoundSettlementRule {

    @Override
    public RoundSettlement settle(Player smuggler, Player inspector, Money smuggleAmount, Money claimedAmount) {
        Money compensation = claimedAmount.half();
        Player updatedSmuggler = smuggler.plusBalance(smuggleAmount).plusBalance(compensation);
        Player updatedInspector = inspector.minusBalance(compensation);

        return new RoundSettlement(updatedSmuggler, updatedInspector, RoundOutcomeType.INSPECTION_UNDER);
    }
}
