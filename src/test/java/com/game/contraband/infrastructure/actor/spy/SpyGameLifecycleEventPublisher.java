package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;

public class SpyGameLifecycleEventPublisher implements GameLifecycleEventPublisher {

    Long roomId;
    String entityId;

    @Override
    public void publish(GameLifecycleEvent event) {
        if (event.type() == LifecycleType.GAME_ENDED) {
            this.roomId = event.roomId();
            this.entityId = event.entityId();
        }
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getEntityId() {
        return entityId;
    }
}
