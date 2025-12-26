package com.game.contraband.infrastructure.actor.directory;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RemoveRoom;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import org.apache.pekko.actor.typed.ActorRef;

public class RoomDirectorySync {

    private final ActorRef<RoomDirectoryCommand> roomDirectory;

    public RoomDirectorySync(ActorRef<RoomDirectoryCommand> roomDirectory) {
        this.roomDirectory = roomDirectory;
    }

    public void register(RoomDirectorySnapshot snapshot) {
        if (roomDirectory != null) {
            roomDirectory.tell(new RoomDirectoryActor.SyncRoomRegistered(snapshot));
        }
    }

    public void remove(long roomId) {
        if (roomDirectory != null) {
            roomDirectory.tell(new RemoveRoom(roomId));
        }
    }
}
