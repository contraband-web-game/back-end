package com.game.contraband.domain.game.engine.match.slot;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import java.util.Optional;

public class ActiveGameSlot implements GameSlot {

    private final ContrabandGame game;

    public static ActiveGameSlot create(ContrabandGame game) {
        validateGame(game);

        return new ActiveGameSlot(game);
    }

    private static void validateGame(ContrabandGame game) {
        if (game == null) {
            throw new IllegalArgumentException("게임은 비어 있을 수 없습니다.");
        }
    }

    public ActiveGameSlot(ContrabandGame game) {
        this.game = game;
    }

    @Override
    public boolean hasGame() {
        return true;
    }

    @Override
    public Optional<ContrabandGame> currentGame() {
        return Optional.of(game);
    }
}
