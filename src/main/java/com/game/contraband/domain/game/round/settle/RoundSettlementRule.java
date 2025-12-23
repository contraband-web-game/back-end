package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.vo.Money;

public interface RoundSettlementRule {

    RoundSettlement settle(Player smuggler, Player inspector, Money smuggleAmount, Money claimedAmount);
}
