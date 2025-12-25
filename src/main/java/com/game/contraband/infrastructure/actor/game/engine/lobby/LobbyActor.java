package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.global.actor.CborSerializable;

public class LobbyActor {

    public interface LobbyCommand extends CborSerializable { }

    public record EndGame() implements LobbyCommand { }
}
