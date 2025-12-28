package com.game.contraband.infrastructure.actor.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RoomDirectoryUpdated;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.QueryRooms;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.QueryRoomsResult;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.LocalDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.RegisterSession;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.RequestRoomDirectoryPage;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.UnregisterSession;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.event.MonitorEventBroadcaster;
import com.game.contraband.infrastructure.event.MonitorEventType;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorPayload;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorRole;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorState;
import com.game.contraband.infrastructure.monitor.payload.MonitorMessage;
import com.game.contraband.infrastructure.monitor.payload.MonitorRoomDirectoryPayload;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoomDirectorySubscriberActorTest {

    private ActorTestKit actorTestKit;

    @BeforeEach
    void setUp() {
        actorTestKit = ActorTestKit.create();
    }

    @AfterEach
    void tearDown() {
        actorTestKit.shutdownTestKit();
    }

    @Test
    void 클라이언트_세션_등록_시_게임방_목록_스냅샷을_전송한다() {
        // given
        TestContext context = createContext();
        QueryRooms initialQuery = (QueryRooms) context.roomDirectory().monitor().receiveMessage();
        QueryRoomsResult response = new QueryRoomsResult(List.of(), 0);
        initialQuery.replyTo().tell(response);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );

        // when
        context.actor().tell(new RegisterSession(1L, session.ref()));

        // then
        RoomDirectoryUpdated actual = (RoomDirectoryUpdated) session.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.rooms()).isEmpty(),
                () -> assertThat(actual.totalCount()).isZero()
        );
    }

    @Test
    void 게임방_목록_요청_시_요청한_페이지의_게임방_목록을_전파한다() {
        // given
        TestContext context = createContext();
        context.roomDirectory().monitor().receiveMessage();

        // when
        context.actor().tell(new RequestRoomDirectoryPage(2, 5));

        // then
        QueryRooms actual = (QueryRooms) context.roomDirectory().monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.page()).isEqualTo(2),
                () -> assertThat(actual.size()).isEqualTo(5)
        );
    }

    @Test
    void 게임방_목록_요청의_결과를_캐싱한다() {
        // given
        TestContext context = createContext();
        QueryRooms initialQuery = (QueryRooms) context.roomDirectory().monitor().receiveMessage();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new RegisterSession(1L, session.ref()));
        session.monitor().receiveMessage();
        RoomDirectorySnapshot snapshot = new RoomDirectorySnapshot(
                3L,
                "방",
                4,
                1,
                "entity",
                false
        );
        QueryRoomsResult response = new QueryRoomsResult(List.of(snapshot), 7);

        // when
        initialQuery.replyTo().tell(response);

        // then
        RoomDirectoryUpdated actual = (RoomDirectoryUpdated) session.monitor().receiveMessage();
        ActorTestUtils.waitUntilCondition(() -> !context.monitor().published().isEmpty());
        MonitorMessage monitorMessage = context.monitor().published().get(0);
        MonitorRoomDirectoryPayload payload = (MonitorRoomDirectoryPayload) monitorMessage.payload();

        assertAll(
                () -> assertThat(actual.rooms()).containsExactly(snapshot),
                () -> assertThat(actual.totalCount()).isEqualTo(7),
                () -> assertThat(monitorMessage.type()).isEqualTo(MonitorEventType.ROOM_DIRECTORY_SNAPSHOT),
                () -> assertThat(payload.rooms()).containsExactly(snapshot)
        );
    }

    @Test
    void 클라이언트_세션을_해제하면_더_이상_게임방_목록의_변경을_전파하지_않는다() {
        // given
        TestContext context = createContext();
        QueryRooms initialQuery = (QueryRooms) context.roomDirectory().monitor().receiveMessage();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new RegisterSession(1L, session.ref()));
        session.monitor().receiveMessage();
        context.actor().tell(new UnregisterSession(1L));
        RoomDirectorySnapshot snapshot = new RoomDirectorySnapshot(3L, "방", 4, 1, "entity", false);
        QueryRoomsResult response = new QueryRoomsResult(List.of(snapshot), 7);

        // when
        initialQuery.replyTo().tell(response);

        // then
        ActorTestUtils.expectNoMessages(session.monitor(), Duration.ofMillis(300L));
    }

    @Test
    void 자기_자신을_종료하면_Topic_구독_해제를_전달하고_종료_이벤트를_발생한다() {
        // given
        TestContext context = createContext();
        context.roomDirectory().monitor().receiveMessage();

        // when
        actorTestKit.stop(context.actor());

        // then
        MonitorMessage monitorMessage = context.monitor().published().get(0);
        MonitorActorPayload payload = (MonitorActorPayload) monitorMessage.payload();
        assertAll(
                () -> assertThat(monitorMessage.type()).isEqualTo(MonitorEventType.ACTOR_EVENT),
                () -> assertThat(payload.role()).isEqualTo(MonitorActorRole.ROOM_DIRECTORY_SUBSCRIBER_ACTOR),
                () -> assertThat(payload.state()).isEqualTo(MonitorActorState.STOPPED)
        );
    }

    private TestContext createContext() {
        ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory = ActorTestUtils.spawnMonitored(
                actorTestKit,
                RoomDirectoryCommand.class,
                Behaviors.ignore()
        );
        SpyMonitorEventBroadcaster monitor = new SpyMonitorEventBroadcaster();
        Behavior<LocalDirectoryCommand> behavior = RoomDirectorySubscriberActor.create(
                roomDirectory.ref(),
                monitor
        );
        ActorRef<LocalDirectoryCommand> actor = actorTestKit.spawn(behavior, "room-directory-subscriber");
        return new TestContext(actor, roomDirectory, monitor);
    }

    private record TestContext(
            ActorRef<LocalDirectoryCommand> actor,
            ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory,
            SpyMonitorEventBroadcaster monitor
    ) {
        public ActorRef<LocalDirectoryCommand> actor() {
            return actor;
        }

        public ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory() {
            return roomDirectory;
        }

        public SpyMonitorEventBroadcaster monitor() {
            return monitor;
        }
    }

    private static class SpyMonitorEventBroadcaster extends MonitorEventBroadcaster {

        private final List<MonitorMessage> published = new ArrayList<>();

        @Override
        public void publish(MonitorMessage message) {
            published.add(message);
        }

        @Override
        public void publishActorEvent(String actorPath, String parentPath, MonitorActorRole role, MonitorActorState state) {
            published.add(new MonitorMessage(MonitorEventType.ACTOR_EVENT, new MonitorActorPayload(actorPath, parentPath, role, state)));
        }

        @Override
        public void publishRoomDirectorySnapshot(List<RoomDirectorySnapshot> rooms) {
            published.add(new MonitorMessage(MonitorEventType.ROOM_DIRECTORY_SNAPSHOT, new MonitorRoomDirectoryPayload(rooms)));
        }

        public List<MonitorMessage> published() {
            return published;
        }
    }
}
