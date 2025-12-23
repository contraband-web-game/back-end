package com.game.contraband.domain.game.round.settle;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.RoundOutcomeType;

public record RoundSettlement(Player smuggler, Player inspector, RoundOutcomeType outcomeType) {
}
