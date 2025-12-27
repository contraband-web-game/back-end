package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateHostDeletedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateJoinedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateKicked;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateToggleReady;
import com.game.contraband.infrastructure.actor.dummy.DummyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.dummy.DummyGameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ChangeMaxPlayerCount;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.EndGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.KickPlayer;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LeaveLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ReSyncPlayer;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.RequestDeleteLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.StartGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.SyncPlayerJoined;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ToggleReady;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ToggleTeam;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.util.HashMap;
import java.util.Map;
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
class LobbyActorTest {

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
    void 로비에_정원이_가득차면_입장할_수_없다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 1);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> clientSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new SyncPlayerJoined(clientSession.ref(), "신규", 2L));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) clientSession.monitor().receiveMessage();

        assertThat(actual.code().name()).isEqualTo("LOBBY_FULL");
    }

    @Test
    void 호스트가_아니면_로비_최대_정원을_변경할_수_없다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> clientSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        Map<Long, ActorRef<ClientSessionCommand>> sessions = new HashMap<>();
        sessions.put(1L, hostSession.ref());
        sessions.put(2L, clientSession.ref());
        TestContext context = createContext(
                lobby,
                sessions
        );

        // when
        context.actor().ref().tell(new ChangeMaxPlayerCount(6, 2L));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) clientSession.monitor().receiveMessage();

        assertThat(actual.code().name()).isEqualTo("LOBBY_MAX_PLAYER_CHANGE_FORBIDDEN");
    }

    @Test
    void 호스트가_로비_최대_정원을_변경한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new ChangeMaxPlayerCount(6, 1L));

        // then
        assertThat(context.lobbyState().lobbyMaxPlayerCount()).isEqualTo(6);
    }

    @Test
    void 플레이어가_준비_상태를_토글한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new ToggleReady(1L));

        // then
        assertAll(
                () -> assertThat(context.lobbyState().readyStateOf(1L)).isTrue(),
//                () -> assertThat(hostSession.monitor().receiveMessage()).isInstanceOf(PropagateToggleReady.class)
                () -> ActorTestUtils.expectMessages(hostSession.monitor(), PropagateToggleReady.class)
        );
    }

    @Test
    void 플레이어가_팀을_변경한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new ToggleTeam(1L));

        // then
        assertThat(context.lobbyState().findPlayerProfile(1L).getTeamRole()).isEqualTo(TeamRole.SMUGGLER);
    }

    @Test
    void 호스트는_로비에서_나갈_수_없다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new LeaveLobby(1L));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) hostSession.monitor().receiveMessage();

        assertThat(actual.code().name()).isEqualTo("LOBBY_INVALID_OPERATION");
    }

    @Test
    void 호스트가_플레이어를_강퇴한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        PlayerProfile other = PlayerProfile.create(2L, "참가자", TeamRole.SMUGGLER);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        lobby.addSmuggler(other);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> otherSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        Map<Long, ActorRef<ClientSessionCommand>> sessions = new HashMap<>();
        sessions.put(1L, hostSession.ref());
        sessions.put(2L, otherSession.ref());
        TestContext context = createContext(
                lobby,
                sessions
        );

        // when
        context.actor().ref().tell(new KickPlayer(1L, 2L));

        // then
        ActorTestUtils.expectMessages(otherSession.monitor(), PropagateKicked.class);
    }

    @Test
    void 호스트가_로비를_삭제한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new RequestDeleteLobby(1L));

        // then
        ActorTestUtils.expectMessages(hostSession.monitor(), PropagateHostDeletedLobby.class);
    }

    @Test
    void 준비가_완료되지_않으면_게임_시작을_거부한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );

        // when
        context.actor().ref().tell(new StartGame(1L, 3));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) hostSession.monitor().receiveMessage();
        assertThat(actual.code().name()).isEqualTo("LOBBY_INVALID_OPERATION");
    }

    @Test
    void 게임을_종료하면_로비_Actor는_stopped_시그널이_발생한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        TestContext context = createContext(
                lobby,
                Map.of(1L, hostSession.ref())
        );
        ActorTestUtils.MonitoredActor<LobbyCommand> watcher = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyCommand.class,
                Behaviors.ignore()
        );
        watcher.monitor().expectTerminated(context.actor().ref());

        // when
        context.actor().ref().tell(new EndGame());

        // then
        watcher.monitor().receiveMessage();
    }

    @Test
    void 클라이언트_세션을_다시_동기화한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "호스트", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(100L, "로비", hostProfile, 4);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> clientSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyRuntimeState lobbyState = new LobbyRuntimeState(100L, 1L, "entity-1", lobby);
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(new HashMap<>());
        ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<GameManagerCommand> parent = ActorTestUtils.spawnMonitored(
                actorTestKit,
                GameManagerCommand.class,
                Behaviors.ignore()
        );
        LobbyExternalGateway gateway = new LobbyExternalGateway(
                lobbyChat.ref(),
                parent.ref(),
                new DummyChatMessageEventPublisher(),
                new DummyGameLifecycleEventPublisher(),
                new DummyChatBlacklistRepository()
        );
        LobbyChatRelay relay = new LobbyChatRelay(gateway);
        LobbyLifecycleCoordinator lifecycleCoordinator = new LobbyLifecycleCoordinator(
                lobbyState,
                registry,
                gateway
        );
        Behavior<LobbyCommand> behavior = LobbyActor.create(
                lobbyState,
                registry,
                gateway,
                lifecycleCoordinator,
                relay
        );
        ActorTestUtils.MonitoredActor<LobbyCommand> actor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyCommand.class,
                behavior
        );

        // when
        actor.ref().tell(new ReSyncPlayer(2L, clientSession.ref()));

        // then
        PropagateJoinedLobby actual = (PropagateJoinedLobby) clientSession.monitor().receiveMessage();

        assertThat(actual.maxPlayerCount()).isEqualTo(4);
    }

    private TestContext createContext(
            Lobby lobby,
            Map<Long, ActorRef<ClientSessionCommand>> initialSessions
    ) {
        LobbyRuntimeState lobbyState = new LobbyRuntimeState(100L, 1L, "entity-1", lobby);
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(initialSessions);

        ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<GameManagerCommand> parent = ActorTestUtils.spawnMonitored(
                actorTestKit,
                GameManagerCommand.class,
                Behaviors.ignore()
        );
        LobbyExternalGateway gateway = new LobbyExternalGateway(
                lobbyChat.ref(),
                parent.ref(),
                new DummyChatMessageEventPublisher(),
                new DummyGameLifecycleEventPublisher(),
                new DummyChatBlacklistRepository()
        );
        LobbyChatRelay relay = new LobbyChatRelay(gateway);
        LobbyLifecycleCoordinator lifecycleCoordinator = new LobbyLifecycleCoordinator(
                lobbyState,
                registry,
                gateway
        );
        Behavior<LobbyCommand> behavior = LobbyActor.create(
                lobbyState,
                registry,
                gateway,
                lifecycleCoordinator,
                relay
        );
        ActorTestUtils.MonitoredActor<LobbyCommand> actor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyCommand.class,
                behavior
        );

        return new TestContext(
                actor,
                lobbyState
        );
    }

    private record TestContext(
            ActorTestUtils.MonitoredActor<LobbyCommand> actor,
            LobbyRuntimeState lobbyState
    ) { }
}
