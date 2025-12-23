package com.game.contraband.domain.game.round.slot;

import com.game.contraband.domain.game.round.Round;
import java.util.Optional;

public interface RoundSlot {

    boolean hasRound();

    Optional<Round> currentRound();
}
