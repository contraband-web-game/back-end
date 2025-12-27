package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.SyncRoomRegistered;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySync;
import com.game.contraband.infrastructure.actor.dummy.DummyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.LifecycleType;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.manage.CoordinatorGateway;
import com.game.contraband.infrastructure.actor.manage.GameLifecycleNotifier;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RegisterRoom;
import com.game.contraband.infrastructure.actor.manage.RoomRegistry;
import com.game.contraband.infrastructure.actor.spy.SpyGameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.stub.StubSequenceGenerator;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyCreationHandlerTest {

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
    void 로비_생성_정보를_준비한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyCreationHandler handler = new LobbyCreationHandler(
                "entity-1",
                new StubSequenceGenerator(100L),
                new DummyChatBlacklistRepository()
        );

        // when
        LobbyCreationHandler.LobbyCreationPlan actual = handler.prepare(
                hostSession.ref(),
                1L,
                "호스트",
                4,
                "테스트방",
                new DummyChatMessageEventPublisher()
        );

        // then
        PlayerProfile hostProfile = actual.lobbyState().findPlayerProfile(1L);
        assertAll(
                () -> assertThat(actual.roomId()).isEqualTo(100L),
                () -> assertThat(actual.lobbyName()).isEqualTo("테스트방"),
                () -> assertThat(actual.maxPlayerCount()).isEqualTo(4),
                () -> assertThat(actual.sessionRegistry().get(1L)).isEqualTo(hostSession.ref()),
                () -> assertThat(hostProfile.getTeamRole()).isEqualTo(TeamRole.INSPECTOR)
        );
    }

    @Test
    void 로비_이름이_없으면_기본값을_사용한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyCreationHandler handler = new LobbyCreationHandler(
                "entity-1",
                new StubSequenceGenerator(200L),
                new DummyChatBlacklistRepository()
        );

        // when
        LobbyCreationHandler.LobbyCreationPlan actual = handler.prepare(
                hostSession.ref(),
                1L,
                "호스트",
                6,
                " ",
                new DummyChatMessageEventPublisher()
        );

        // then
        assertAll(
                () -> assertThat(actual.lobbyName()).isEqualTo("게임방200"),
                () -> assertThat(actual.roomId()).isEqualTo(200L)
        );
    }

    @Test
    void 로비_생성을_완료한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyCreationHandler handler = new LobbyCreationHandler(
                "entity-1",
                new StubSequenceGenerator(300L),
                new DummyChatBlacklistRepository()
        );
        LobbyCreationHandler.LobbyCreationPlan plan = handler.prepare(
                hostSession.ref(),
                1L,
                "호스트",
                6,
                "로비",
                new DummyChatMessageEventPublisher()
        );
        ActorTestUtils.MonitoredActor<RoomDirectoryCommand> roomDirectory = ActorTestUtils.spawnMonitored(
                actorTestKit,
                RoomDirectoryCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<GameRoomCoordinatorCommand> coordinator = ActorTestUtils.spawnMonitored(
                actorTestKit,
                GameRoomCoordinatorCommand.class,
                Behaviors.ignore()
        );
        RoomRegistry roomRegistry = new RoomRegistry();
        RoomDirectorySync roomDirectorySync = new RoomDirectorySync(roomDirectory.ref());
        CoordinatorGateway coordinatorGateway = new CoordinatorGateway(coordinator.ref());
        SpyGameLifecycleEventPublisher lifecyclePublisher = new SpyGameLifecycleEventPublisher();
        GameLifecycleNotifier lifecycleNotifier = new GameLifecycleNotifier(lifecyclePublisher, "entity-1");
        ActorTestUtils.MonitoredActor<LobbyCommand> lobbyActor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyCommand.class,
                Behaviors.ignore()
        );

        // when
        handler.completeCreation(
                plan,
                lobbyActor.ref(),
                roomRegistry,
                roomDirectorySync,
                coordinatorGateway,
                lifecycleNotifier
        );

        // then
        RoomDirectoryCommand directoryCommand = roomDirectory.monitor().receiveMessage();
        SyncRoomRegistered registered = (SyncRoomRegistered) directoryCommand;
        RegisterRoom registerRoom = (RegisterRoom) coordinator.monitor().receiveMessage();
        RoomDirectorySnapshot snapshot = registered.roomSummary();
        assertAll(
                () -> assertThat(roomRegistry.get(plan.roomId())).isEqualTo(lobbyActor.ref()),
                () -> assertThat(snapshot.roomId()).isEqualTo(plan.roomId()),
                () -> assertThat(snapshot.lobbyName()).isEqualTo("로비"),
                () -> assertThat(snapshot.maxPlayerCount()).isEqualTo(6),
                () -> assertThat(snapshot.currentPlayerCount()).isEqualTo(1),
                () -> assertThat(snapshot.entityId()).isEqualTo("entity-1"),
                () -> assertThat(registerRoom.roomId()).isEqualTo(plan.roomId()),
                () -> assertThat(registerRoom.entityId()).isEqualTo("entity-1"),
                () -> assertThat(lifecyclePublisher.getLastType()).isEqualTo(LifecycleType.ROOM_CREATED),
                () -> assertThat(lifecyclePublisher.getRoomId()).isEqualTo(plan.roomId())
        );
    }
}
