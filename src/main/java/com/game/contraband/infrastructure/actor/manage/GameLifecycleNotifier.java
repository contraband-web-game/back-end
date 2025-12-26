package com.game.contraband.infrastructure.actor.manage;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;

public class GameLifecycleNotifier {

    private final GameLifecycleEventPublisher publisher;
    private final String entityId;

    public GameLifecycleNotifier(GameLifecycleEventPublisher publisher, String entityId) {
        this.publisher = publisher;
        this.entityId = entityId;
    }

    public void roomCreated(long roomId) {
        if (publisher != null) {
            publisher.publishRoomCreated(entityId, roomId);
        }
    }

    public void gameStarted(long roomId) {
        if (publisher != null) {
            publisher.publishGameStarted(entityId, roomId);
        }
    }

    public void gameEnded(long roomId) {
        if (publisher != null) {
            publisher.publishGameEnded(entityId, roomId);
        }
    }

    public void roomRemoved(long roomId) {
        if (publisher != null) {
            publisher.publishRoomRemoved(entityId, roomId);
        }
    }

    public GameLifecycleEventPublisher publisher() {
        return publisher;
    }
}
