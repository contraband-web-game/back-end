package com.game.contraband.infrastructure.actor.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.QueryRooms;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.QueryRoomsResult;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.SyncRoomRegistered;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.SyncRoomRemoved;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RemoveRoom;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.time.Duration;
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
class RoomDirectoryActorTest {

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
    void 게임방_생성_시_생성된_게임방을_기록한다() {
        // given
        TestContext context = createContext();
        RoomDirectorySnapshot snapshot = new RoomDirectorySnapshot(
                1L,
                "방",
                4,
                1,
                "entity1",
                false
        );

        // when
        context.actor().tell(new SyncRoomRegistered(snapshot));

        // then
        context.actor().tell(new QueryRooms(0, 10, context.query().ref()));
        QueryRoomsResult actual = context.query().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.rooms()).containsExactly(snapshot),
                () -> assertThat(actual.totalCount()).isEqualTo(1)
        );
    }

    @Test
    void 게임방_삭제_요청_시_해당_게임방이_기록에_없다면_무시한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new RemoveRoom(10L));

        // then
        ActorTestUtils.expectNoMessages(context.query().monitor(), Duration.ofMillis(300L));
    }

    @Test
    void 게임방_삭제_시_기에서_제거된다() {
        // given
        TestContext context = createContext();
        RoomDirectorySnapshot snapshot = new RoomDirectorySnapshot(
                1L,
                "방",
                4,
                1,
                "entity1",
                false
        );
        context.actor().tell(new SyncRoomRegistered(snapshot));

        // when
        context.actor().tell(new SyncRoomRemoved(1L));

        // then
        context.actor().tell(new QueryRooms(0, 10, context.query().ref()));
        QueryRoomsResult actual = context.query().monitor().receiveMessage();
        assertThat(actual.rooms()).isEmpty();
    }

    @Test
    void 게임방_목록을_페이지네이션_방법으로_조회한다() {
        // given
        TestContext context = createContext();
        context.actor().tell(new SyncRoomRegistered(new RoomDirectorySnapshot(3L, "3", 4, 1, "e3", false)));
        context.actor().tell(new SyncRoomRegistered(new RoomDirectorySnapshot(2L, "2", 4, 1, "e2", true)));
        context.actor().tell(new SyncRoomRegistered(new RoomDirectorySnapshot(1L, "1", 4, 1, "e1", false)));

        // when
        context.actor().tell(new QueryRooms(0, 2, context.query().ref()));

        // then
        QueryRoomsResult actualPage1 = context.query().monitor().receiveMessage();
        context.actor().tell(new QueryRooms(1, 2, context.query().ref()));
        QueryRoomsResult actualPage2 = context.query().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actualPage1.rooms().stream().map(RoomDirectorySnapshot::roomId).toList()).containsExactly(1L, 3L),
                () -> assertThat(actualPage1.totalCount()).isEqualTo(3),
                () -> assertThat(actualPage2.rooms().stream().map(RoomDirectorySnapshot::roomId).toList()).containsExactly(2L),
                () -> assertThat(actualPage2.totalCount()).isEqualTo(3)
        );
    }

    private TestContext createContext() {
        ActorTestUtils.MonitoredActor<QueryRoomsResult> query = ActorTestUtils.spawnMonitored(
                actorTestKit,
                QueryRoomsResult.class,
                Behaviors.ignore()
        );
        Behavior<RoomDirectoryCommand> behavior = RoomDirectoryActor.create();
        ActorRef<RoomDirectoryCommand> actor = actorTestKit.spawn(behavior);
        return new TestContext(actor, query);
    }

    private record TestContext(
            ActorRef<RoomDirectoryCommand> actor,
            ActorTestUtils.MonitoredActor<QueryRoomsResult> query
    ) {
        public ActorRef<RoomDirectoryCommand> actor() {
            return actor;
        }

        public ActorTestUtils.MonitoredActor<QueryRoomsResult> query() {
            return query;
        }
    }
}
