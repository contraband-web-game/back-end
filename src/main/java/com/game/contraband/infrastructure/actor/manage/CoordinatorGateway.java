package com.game.contraband.infrastructure.actor.manage;

import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RegisterRoom;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RoomRemovalNotification;
import org.apache.pekko.actor.typed.ActorRef;

public class CoordinatorGateway {

    private final ActorRef<GameRoomCoordinatorCommand> coordinator;

    public CoordinatorGateway(ActorRef<GameRoomCoordinatorCommand> coordinator) {
        this.coordinator = coordinator;
    }

    public void registerRoom(long roomId, String entityId) {
        if (coordinator != null) {
            coordinator.tell(new RegisterRoom(roomId, entityId));
        }
    }

    public void notifyRoomRemoved(long roomId) {
        if (coordinator != null) {
            coordinator.tell(new RoomRemovalNotification(roomId));
        }
    }
}
