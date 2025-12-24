package com.game.contraband.infrastructure.actor.directory;

import static com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.*;

import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RoomDirectoryUpdated;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.QueryRooms;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.QueryRoomsResult;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryEvent;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.actor.typed.pubsub.Topic;

public class RoomDirectorySubscriberActor extends AbstractBehavior<LocalDirectoryCommand> {

    public static Behavior<LocalDirectoryCommand> create(ActorRef<RoomDirectoryCommand> roomDirectory) {
        return Behaviors.setup(context -> {
            ActorRef<RoomDirectoryEvent> roomEventAdapter = context.messageAdapter(RoomDirectoryEvent.class, WrappedRoomEvent::new);
            ActorRef<QueryRoomsResult> roomDirectoryQueryAdapter = context.messageAdapter(QueryRoomsResult.class, WrappedRoomDirectoryQueryResult::new);
            ActorRef<Topic.Command<RoomDirectoryEvent>> topicRef = context.spawn(
                    Topic.create(RoomDirectoryEvent.class, RoomDirectoryActor.TOPIC_NAME),
                    RoomDirectoryActor.TOPIC_NAME + "-subscriber-" + context.getSelf().path().name()
            );
            RoomDirectorySubscriberActor actor = new RoomDirectorySubscriberActor(
                    context,
                    roomDirectory,
                    roomEventAdapter,
                    roomDirectoryQueryAdapter,
                    topicRef
            );

            topicRef.tell(Topic.subscribe(roomEventAdapter));
            actor.requestPage();
            return actor;
        });
    }

    private final ActorRef<RoomDirectoryCommand> roomDirectory;
    private final ActorRef<RoomDirectoryEvent> roomEventAdapter;
    private final ActorRef<QueryRoomsResult> roomDirectoryQueryAdapter;
    private final List<RoomDirectorySnapshot> roomCache = new ArrayList<>();
    private final Map<Long, ActorRef<ClientSessionCommand>> sessionSubscribers = new HashMap<>();
    private final ActorRef<Topic.Command<RoomDirectoryEvent>> topicRef;
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalCount = 0;

    private RoomDirectorySubscriberActor(
            ActorContext<LocalDirectoryCommand> context,
            ActorRef<RoomDirectoryCommand> roomDirectory,
            ActorRef<RoomDirectoryEvent> roomEventAdapter,
            ActorRef<QueryRoomsResult> roomDirectoryQueryAdapter,
            ActorRef<Topic.Command<RoomDirectoryEvent>> topicRef
    ) {
        super(context);

        this.roomDirectory = roomDirectory;
        this.roomEventAdapter = roomEventAdapter;
        this.roomDirectoryQueryAdapter = roomDirectoryQueryAdapter;
        this.topicRef = topicRef;
    }

    @Override
    public Receive<LocalDirectoryCommand> createReceive() {
        return newReceiveBuilder().onMessage(RegisterSession.class, this::onRegisterSession)
                                  .onMessage(UnregisterSession.class, this::onUnregisterSession)
                                  .onMessage(WrappedRoomEvent.class, this::onRoomEvent)
                                  .onMessage(WrappedRoomDirectoryQueryResult.class, this::onRoomDirectoryQueryResponse)
                                  .onMessage(RequestRoomDirectoryPage.class, this::onRequestRoomDirectoryPage)
                                  .onSignal(PostStop.class, this::onPostStop)
                                  .build();
    }

    private Behavior<LocalDirectoryCommand> onRoomEvent(WrappedRoomEvent command) {
        requestPage();
        return this;
    }

    private Behavior<LocalDirectoryCommand> onRequestRoomDirectoryPage(RequestRoomDirectoryPage command) {
        currentPage = Math.max(0, command.page());
        pageSize = Math.max(1, command.size());
        requestPage();
        return this;
    }

    private Behavior<LocalDirectoryCommand> onRoomDirectoryQueryResponse(WrappedRoomDirectoryQueryResult command) {
        roomCache.clear();
        roomCache.addAll(command.response().rooms());
        totalCount = command.response().totalCount();
        broadcastSnapshot();
        return this;
    }

    private Behavior<LocalDirectoryCommand> onRegisterSession(RegisterSession command) {
        sessionSubscribers.put(command.userId(), command.session());
        command.session()
               .tell(new RoomDirectoryUpdated(roomCache, totalCount));
        return this;
    }

    private Behavior<LocalDirectoryCommand> onUnregisterSession(UnregisterSession command) {
        sessionSubscribers.remove(command.userId());
        return this;
    }

    private void broadcastSnapshot() {
        List<RoomDirectorySnapshot> snapshot = List.copyOf(roomCache);

        sessionSubscribers.values()
                          .forEach(sub -> sub.tell(new RoomDirectoryUpdated(snapshot, totalCount)));
    }

    private Behavior<LocalDirectoryCommand> onPostStop(PostStop signal) {
        if (topicRef != null) {
            topicRef.tell(Topic.unsubscribe(roomEventAdapter));
        }

        return this;
    }

    private void requestPage() {
        roomDirectory.tell(new QueryRooms(currentPage, pageSize, roomDirectoryQueryAdapter));
    }

    public interface LocalDirectoryCommand extends CborSerializable { }

    public record RegisterSession(Long userId, ActorRef<ClientSessionCommand> session) implements LocalDirectoryCommand { }

    public record UnregisterSession(Long userId) implements LocalDirectoryCommand { }

    private record WrappedRoomEvent(RoomDirectoryEvent event) implements LocalDirectoryCommand { }

    private record WrappedRoomDirectoryQueryResult(QueryRoomsResult response) implements LocalDirectoryCommand { }

    public record RequestRoomDirectoryPage(int page, int size) implements LocalDirectoryCommand { }
}
