package com.game.contraband.infrastructure.actor.dummy;

import org.apache.pekko.actor.Cancellable;

public class DummyCancellable implements Cancellable {

    private boolean cancelled;

    @Override
    public boolean cancel() {
        cancelled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
