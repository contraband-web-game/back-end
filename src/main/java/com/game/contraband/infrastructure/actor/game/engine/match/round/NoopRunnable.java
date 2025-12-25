package com.game.contraband.infrastructure.actor.game.engine.match.round;

public final class NoopRunnable implements Runnable {

    public static final NoopRunnable INSTANCE = new NoopRunnable();

    private NoopRunnable() { }

    @Override
    public void run() {
        // NO-OP
    }
}
