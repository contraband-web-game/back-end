package com.game.contraband.domain.game.round.dto;

import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.settle.RoundSettlement;

public record RoundDto(Round round, RoundSettlement settlement) {
}
