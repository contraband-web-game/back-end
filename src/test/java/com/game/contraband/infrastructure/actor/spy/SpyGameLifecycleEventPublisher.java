package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;

public class SpyGameLifecycleEventPublisher implements GameLifecycleEventPublisher {

    private Long roomId;
    private String entityId;
    private LifecycleType lastType;

    @Override
    public void publish(GameLifecycleEvent event) {
        this.lastType = event.type();
        this.roomId = event.roomId();
        this.entityId = event.entityId();
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getEntityId() {
        return entityId;
    }

    public LifecycleType getLastType() {
        return lastType;
    }
}
