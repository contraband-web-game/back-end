package com.game.contraband.domain.game.engine.match.slot;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import java.util.Optional;

public interface GameSlot {

    boolean hasGame();

    Optional<ContrabandGame> currentGame();
}
