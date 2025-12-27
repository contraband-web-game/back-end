package com.game.contraband.infrastructure.actor.spy;

import org.apache.pekko.actor.Cancellable;

public class SpyCancellable implements Cancellable {

    private boolean cancelled;

    @Override
    public boolean cancel() {
        this.cancelled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
