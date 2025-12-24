package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.global.actor.CborSerializable;

public class ClientSessionActor {

    public interface ClientSessionCommand extends CborSerializable { }

    public interface ChatCommand extends ClientSessionCommand { }
}
