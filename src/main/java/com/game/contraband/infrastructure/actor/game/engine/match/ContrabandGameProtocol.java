package com.game.contraband.infrastructure.actor.game.engine.match;

import com.game.contraband.global.actor.CborSerializable;

public interface ContrabandGameProtocol {

    interface ContrabandGameCommand extends CborSerializable { }

    record SyncReconnectedPlayer(Long playerId) implements ContrabandGameCommand { }
}
