package com.game.contraband.infrastructure.actor.stub;

import com.game.contraband.infrastructure.actor.sequence.SnowflakeSequenceGenerator;

public class StubSequenceGenerator extends SnowflakeSequenceGenerator {

    private final Long next;

    public StubSequenceGenerator(long next) {
        super(0L);
        this.next = next;
    }

    @Override
    public Long nextSequence() {
        return next;
    }
}
