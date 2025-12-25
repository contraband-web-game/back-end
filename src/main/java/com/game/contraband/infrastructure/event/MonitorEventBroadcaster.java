package com.game.contraband.infrastructure.event;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorPayload;
import com.game.contraband.infrastructure.monitor.payload.MonitorMessage;
import com.game.contraband.infrastructure.monitor.payload.MonitorRoomDirectoryPayload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class MonitorEventBroadcaster {

    private final Sinks.Many<MonitorMessage> sink;

    public MonitorEventBroadcaster() {
        this.sink = Sinks.many()
                         .multicast()
                         .onBackpressureBuffer();
    }

    public void publish(MonitorMessage message) {
        sink.tryEmitNext(message);
    }

    public void publishActorEvent(String actorPath, String parentPath, String role, String state) {
        publish(
                new MonitorMessage(
                        MonitorEventType.ACTOR_EVENT,
                        new MonitorActorPayload(
                                actorPath,
                                parentPath,
                                role,
                                state
                        )
                )
        );
    }

    public void publishRoomDirectorySnapshot(java.util.List<RoomDirectorySnapshot> rooms) {
        publish(
                new MonitorMessage(
                        MonitorEventType.ROOM_DIRECTORY_SNAPSHOT,
                        new MonitorRoomDirectoryPayload(rooms)
                )
        );
    }

    public Flux<MonitorMessage> flux() {
        return sink.asFlux();
    }
}
