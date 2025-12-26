package com.game.contraband.infrastructure.actor.directory;

import static com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.*;

import com.game.contraband.global.actor.CborSerializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.actor.typed.pubsub.Topic;

public class RoomDirectoryActor extends AbstractBehavior<RoomDirectoryCommand> {

    public static final String TOPIC_NAME = "room-directory-topic";

    public static Behavior<RoomDirectoryCommand> create() {
        return Behaviors.setup(RoomDirectoryActor::new);
    }

    private final Map<Long, RoomDirectorySnapshot> rooms = new HashMap<>();
    private final ActorRef<Topic.Command<RoomDirectoryEvent>> topic;

    private RoomDirectoryActor(ActorContext<RoomDirectoryCommand> context) {
        super(context);
        this.topic = context.spawn(Topic.create(RoomDirectoryEvent.class, TOPIC_NAME), TOPIC_NAME);
    }

    @Override
    public Receive<RoomDirectoryCommand> createReceive() {
        return newReceiveBuilder().onMessage(SyncRoomRegistered.class, this::onSyncRoomRegistered)
                                  .onMessage(SyncRoomRemoved.class, this::onSyncRoomRemoved)
                                  .onMessage(RemoveRoom.class, this::onRemoveRoom)
                                  .onMessage(QueryRooms.class, this::onQueryRooms)
                                  .build();
    }

    private Behavior<RoomDirectoryCommand> onSyncRoomRegistered(SyncRoomRegistered command) {
        rooms.put(command.roomSummary().roomId(), command.roomSummary());
        topic.tell(
                Topic.publish(
                        new RoomDirectoryEvent(
                                RoomEventType.ADDED,
                                command.roomSummary(),
                                command.roomSummary().roomId()
                        )
                )
        );
        return this;
    }

    private Behavior<RoomDirectoryCommand> onSyncRoomRemoved(SyncRoomRemoved command) {
        RoomDirectorySnapshot removed = rooms.remove(command.roomId());

        if (removed != null) {
            topic.tell(
                    Topic.publish(
                            new RoomDirectoryEvent(RoomEventType.REMOVED, removed, command.roomId())
                    )
            );
        }

        return this;
    }

    private Behavior<RoomDirectoryCommand> onRemoveRoom(RemoveRoom command) {
        RoomDirectorySnapshot removed = rooms.remove(command.roomId());

        if (removed != null) {
            topic.tell(
                    Topic.publish(
                            new RoomDirectoryEvent(RoomEventType.REMOVED, removed, command.roomId())
                    )
            );
        }

        return this;
    }

    private Behavior<RoomDirectoryCommand> onQueryRooms(QueryRooms command) {
        List<RoomDirectorySnapshot> sorted = rooms.values()
                                                         .stream()
                                                         .sorted(
                                                                 Comparator.comparing(RoomDirectorySnapshot::gameStarted)
                                                                           .thenComparing(RoomDirectorySnapshot::roomId)
                                                         )
                                                         .toList();
        int total = sorted.size();
        int from = Math.max(command.page() * command.size(), 0);
        if (from >= total) {
            command.replyTo().tell(new QueryRoomsResult(List.of(), total));
            return this;
        }
        int to = Math.min(from + command.size(), total);

        List<RoomDirectorySnapshot> page = sorted.subList(from, to)
                                                       .stream()
                                                       .toList();

        command.replyTo().tell(new QueryRoomsResult(page, total));
        return this;
    }

    public interface RoomDirectoryCommand extends CborSerializable { }

    public record SyncRoomRegistered(RoomDirectorySnapshot roomSummary) implements RoomDirectoryCommand { }

    public record SyncRoomRemoved(Long roomId) implements RoomDirectoryCommand { }

    public record QueryRooms(int page, int size, ActorRef<QueryRoomsResult> replyTo) implements RoomDirectoryCommand { }

    public record QueryRoomsResult(List<RoomDirectorySnapshot> rooms, int totalCount) implements RoomDirectoryCommand { }

    public record RoomDirectoryEvent(RoomEventType type, RoomDirectorySnapshot roomSummary, Long roomId) implements CborSerializable { }

    public record RoomDirectorySnapshot(Long roomId, String lobbyName, int maxPlayerCount, int currentPlayerCount, String entityId, boolean gameStarted) implements CborSerializable { }

    public record RemoveRoom(Long roomId) implements RoomDirectoryCommand { }
}
