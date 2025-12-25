package com.game.contraband.infrastructure.monitor.payload;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import java.util.List;

public record MonitorRoomDirectoryPayload(List<RoomDirectorySnapshot> rooms) { }
