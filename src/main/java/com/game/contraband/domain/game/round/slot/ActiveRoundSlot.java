package com.game.contraband.domain.game.round.slot;

import com.game.contraband.domain.game.round.Round;
import java.util.Optional;

public class ActiveRoundSlot implements RoundSlot {

    public static ActiveRoundSlot create(Round round) {
        validateRound(round);

        return new ActiveRoundSlot(round);
    }

    private static void validateRound(Round round) {
        if (round == null) {
            throw new IllegalArgumentException("라운드는 비어 있을 수 없습니다.");
        }
    }

    private ActiveRoundSlot(Round round) {
        this.round = round;
    }

    private final Round round;

    @Override
    public boolean hasRound() {
        return true;
    }

    @Override
    public Optional<Round> currentRound() {
        return Optional.of(round);
    }
}
