package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.vo.Money;

public class PassSettlementRule implements RoundSettlementRule {

    @Override
    public RoundSettlement settle(Player smuggler, Player inspector, Money smuggleAmount, Money claimedAmount) {
        Player updatedSmuggler = smuggler.plusBalance(smuggleAmount);

        return new RoundSettlement(updatedSmuggler, inspector, RoundOutcomeType.PASS);
    }
}
