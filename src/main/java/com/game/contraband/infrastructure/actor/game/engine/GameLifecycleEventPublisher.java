package com.game.contraband.infrastructure.actor.game.engine;

public interface GameLifecycleEventPublisher {

    void publish(GameLifecycleEvent event);

    default void publishEntityCreated(String entityId) {
        publish(new GameLifecycleEvent(LifecycleType.ENTITY_CREATED, entityId, null));
    }

    default void publishEntityRemoved(String entityId) {
        publish(new GameLifecycleEvent(LifecycleType.ENTITY_REMOVED, entityId, null));
    }

    default void publishRoomCreated(String entityId, Long roomId) {
        publish(new GameLifecycleEvent(LifecycleType.ROOM_CREATED, entityId, roomId));
    }

    default void publishRoomRemoved(String entityId, Long roomId) {
        publish(new GameLifecycleEvent(LifecycleType.ROOM_REMOVED, entityId, roomId));
    }

    default void publishGameStarted(String entityId, Long roomId) {
        publish(new GameLifecycleEvent(LifecycleType.GAME_STARTED, entityId, roomId));
    }

    default void publishGameEnded(String entityId, Long roomId) {
        publish(new GameLifecycleEvent(LifecycleType.GAME_ENDED, entityId, roomId));
    }

    record GameLifecycleEvent(
            LifecycleType type,
            String entityId,
            Long roomId
    ) {
        public GameLifecycleEvent {
            if (type == null) {
                throw new IllegalArgumentException("type은 비어 있을 수 없습니다.");
            }
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("entityId는 비어 있을 수 없습니다.");
            }
        }
    }

    enum LifecycleType {
        ENTITY_CREATED,
        ENTITY_REMOVED,
        ROOM_CREATED,
        ROOM_REMOVED,
        GAME_STARTED,
        GAME_ENDED
    }
}
