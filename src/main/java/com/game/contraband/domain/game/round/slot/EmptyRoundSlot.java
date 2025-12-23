package com.game.contraband.domain.game.round.slot;

import com.game.contraband.domain.game.round.Round;
import java.util.Optional;

public class EmptyRoundSlot implements RoundSlot {

    public static final EmptyRoundSlot INSTANCE = new EmptyRoundSlot();

    private EmptyRoundSlot() {
    }

    @Override
    public boolean hasRound() {
        return false;
    }

    @Override
    public Optional<Round> currentRound() {
        return Optional.empty();
    }
}
