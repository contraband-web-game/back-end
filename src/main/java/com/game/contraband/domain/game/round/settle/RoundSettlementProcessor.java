package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.Round;

public class RoundSettlementProcessor {

    public static RoundSettlementProcessor create() {
        return new RoundSettlementProcessor(new RoundSettlementRuleSelector());
    }

    private RoundSettlementProcessor(RoundSettlementRuleSelector settlementRuleSelector) {
        this.settlementRuleSelector = settlementRuleSelector;
    }

    private final RoundSettlementRuleSelector settlementRuleSelector;

    public RoundSettlementResult process(Round round, Player smuggler, Player inspector) {
        RoundSettlement settlement = round.settle(smuggler, inspector, settlementRuleSelector);

        return new RoundSettlementResult(settlement);
    }

    public record RoundSettlementResult(RoundSettlement settlement) {

        public Player updatedSmuggler() {
            return settlement.smuggler();
        }

        public Player updatedInspector() {
            return settlement.inspector();
        }
    }
}
