package com.game.contraband.infrastructure.actor.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RemoveRoom;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.SyncRoomRegistered;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.GameLifecycleEvent;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.LifecycleType;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.CreateLobby;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.JoinLobby;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncDeleteLobby;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncEndGame;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncRoomPlayerCount;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncRoomStarted;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RegisterRoom;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RoomRemovalNotification;
import com.game.contraband.infrastructure.actor.spy.SpyLifecyclePublisher;
import com.game.contraband.infrastructure.actor.stub.StubChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
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
class GameManagerEntityTest {

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
    void 로비_생성_시_게임방_목록에_등록하고_이벤트를_발행한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );

        // when
        context.actor().tell(
                new CreateLobby(
                        hostSession.ref(),
                        1L,
                        "host",
                        4,
                        "방",
                        new DummyChatMessageEventPublisher()
                )
        );

        // then
        GameRoomCoordinatorCommand actualCoordinator = context.coordinator().monitor().receiveMessage();
        RoomDirectoryCommand actualDirectory = context.roomDirectory().monitor().receiveMessage();
        ActorTestUtils.waitUntilCondition(() -> context.lifecyclePublisher().events().size() >= 1);
        GameLifecycleEvent actualEvent = context.lifecyclePublisher().events().get(0);

        assertAll(
                () -> assertThat(actualCoordinator).isInstanceOf(RegisterRoom.class),
                () -> assertThat(actualDirectory).isInstanceOf(SyncRoomRegistered.class),
                () -> assertThat(((RegisterRoom) actualCoordinator).roomId()).isPositive(),
                () -> assertThat(((SyncRoomRegistered) actualDirectory).roomSummary().roomId()).isPositive(),
                () -> assertThat(actualEvent.type()).isEqualTo(LifecycleType.ROOM_CREATED)
        );
    }

    @Test
    void 로비_참여시_지정한_로비가_없다면_참여할_수_없다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> clientSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );

        // when
        context.actor().tell(new JoinLobby(clientSession.ref(), 99L, 3L, "플레이어"));

        // then
        ClientSessionCommand actual = clientSession.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual).isInstanceOf(HandleExceptionMessage.class),
                () -> assertThat(((HandleExceptionMessage) actual).code()).isNotNull()
        );
    }

    @Test
    void 게임_시작_동기화_시_게임_방_목록에서_게임_상태를_갱신하고_이벤트를_발행한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new SyncRoomStarted(2L, "방", 5, 3));

        // then
        RoomDirectoryCommand actualDirectory = context.roomDirectory().monitor().receiveMessage();
        ActorTestUtils.waitUntilCondition(() -> !context.lifecyclePublisher().events().isEmpty());
        GameLifecycleEvent actualEvent = context.lifecyclePublisher().events().get(0);

        assertAll(
                () -> assertThat(actualDirectory).isInstanceOf(SyncRoomRegistered.class),
                () -> assertThat(((SyncRoomRegistered) actualDirectory).roomSummary().gameStarted()).isTrue(),
                () -> assertThat(actualEvent.type()).isEqualTo(LifecycleType.GAME_STARTED)
        );
    }

    @Test
    void 게임_종료_동기화_시_게임_방_목록에서_해당_게임방을_삭제하고_이벤트를_발행한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new SyncEndGame(4L));

        // then
        GameRoomCoordinatorCommand actualCoordinator = context.coordinator().monitor().receiveMessage();
        RoomDirectoryCommand actualDirectory = context.roomDirectory().monitor().receiveMessage();
        ActorTestUtils.waitUntilCondition(() -> context.lifecyclePublisher().events().size() >= 2);
        List<GameLifecycleEvent> events = context.lifecyclePublisher().events();
        GameLifecycleEvent actualGameEnded = events.get(0);
        GameLifecycleEvent actualRoomRemoved = events.get(1);

        assertAll(
                () -> assertThat(actualCoordinator).isInstanceOf(RoomRemovalNotification.class),
                () -> assertThat(((RoomRemovalNotification) actualCoordinator).roomId()).isEqualTo(4L),
                () -> assertThat(actualDirectory).isInstanceOf(RemoveRoom.class),
                () -> assertThat(((RemoveRoom) actualDirectory).roomId()).isEqualTo(4L),
                () -> assertThat(actualGameEnded.type()).isEqualTo(LifecycleType.GAME_ENDED),
                () -> assertThat(actualRoomRemoved.type()).isEqualTo(LifecycleType.ROOM_REMOVED)
        );
    }

    @Test
    void 로비_삭제_동기화_시_해당_로비를_삭제하고_이벤트를_발행한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new SyncDeleteLobby(6L));

        // then
        GameRoomCoordinatorCommand actualCoordinator = context.coordinator().monitor().receiveMessage();
        RoomDirectoryCommand actualDirectory = context.roomDirectory().monitor().receiveMessage();
        ActorTestUtils.waitUntilCondition(() -> !context.lifecyclePublisher().events().isEmpty());
        GameLifecycleEvent actualEvent = context.lifecyclePublisher().events().get(0);

        assertAll(
                () -> assertThat(actualCoordinator).isInstanceOf(RoomRemovalNotification.class),
                () -> assertThat(((RoomRemovalNotification) actualCoordinator).roomId()).isEqualTo(6L),
                () -> assertThat(actualDirectory).isInstanceOf(RemoveRoom.class),
                () -> assertThat(((RemoveRoom) actualDirectory).roomId()).isEqualTo(6L),
                () -> assertThat(actualEvent.type()).isEqualTo(LifecycleType.ROOM_REMOVED)
        );
    }

    @Test
    void 플레이어_수를_동기화한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor()
               .tell(
                       new SyncRoomPlayerCount(
                               7L,
                               "방",
                               6,
                               4,
                               false
                       )
               );

        // then
        RoomDirectoryCommand actualDirectory = context.roomDirectory().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actualDirectory).isInstanceOf(SyncRoomRegistered.class),
                () -> assertThat(((SyncRoomRegistered) actualDirectory).roomSummary().currentPlayerCount()).isEqualTo(4),
                () -> assertThat(((SyncRoomRegistered) actualDirectory).roomSummary().maxPlayerCount()).isEqualTo(6),
                () -> assertThat(((SyncRoomRegistered) actualDirectory).roomSummary().gameStarted()).isFalse()
        );
    }

    private TestContext createContext() {
        SpyLifecyclePublisher lifecyclePublisher = new SpyLifecyclePublisher();
        ActorTestUtils.MonitoredActor<GameRoomCoordinatorCommand> coordinator = ActorTestUtils.spawnMonitored(
                actorTestKit,
                GameRoomCoordinatorCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory = ActorTestUtils.spawnMonitored(
                actorTestKit,
                RoomDirectoryCommand.class,
                Behaviors.ignore()
        );
        Behavior<GameManagerCommand> behavior = GameManagerEntity.create(
                "game-rooms-1",
                1L,
                coordinator.ref(),
                roomDirectory.ref(),
                lifecyclePublisher,
                new StubChatBlacklistRepository()
        );
        ActorRef<GameManagerCommand> actor = actorTestKit.spawn(behavior);
        return new TestContext(actor, coordinator, roomDirectory, lifecyclePublisher);
    }

    private record TestContext(
            ActorRef<GameManagerCommand> actor,
            ActorTestUtils.MonitoredActor<GameRoomCoordinatorCommand> coordinator,
            ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory,
            SpyLifecyclePublisher lifecyclePublisher
    ) {
        public ActorRef<GameManagerCommand> actor() {
            return actor;
        }

        public ActorTestUtils.MonitoredActor<GameRoomCoordinatorCommand> coordinator() {
            return coordinator;
        }

        public ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory() {
            return roomDirectory;
        }

        public SpyLifecyclePublisher lifecyclePublisher() {
            return lifecyclePublisher;
        }
    }
}
