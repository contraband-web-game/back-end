package com.game.contraband.infrastructure.actor.stub;

import com.game.contraband.infrastructure.actor.sequence.SnowflakeSequenceGenerator;

public class StubSnowflakeSequenceGenerator extends SnowflakeSequenceGenerator {

    private final long next;

    public StubSnowflakeSequenceGenerator(long next) {
        super(1L);

        this.next = next;
    }

    @Override
    public Long nextSequence() {
        return next;
    }
}
