package com.game.contraband.domain.game.engine.match.slot;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import java.util.Optional;

public class EmptyGameSlot implements GameSlot {

    public static final EmptyGameSlot INSTANCE = new EmptyGameSlot();

    private EmptyGameSlot() {
    }

    @Override
    public boolean hasGame() {
        return false;
    }

    @Override
    public Optional<ContrabandGame> currentGame() {
        return Optional.empty();
    }
}
