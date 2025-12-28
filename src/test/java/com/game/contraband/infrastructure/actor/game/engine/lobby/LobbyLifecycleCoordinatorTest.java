package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateWelcomeMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncLobbyChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateCreateLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateCreatedLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.dummy.DummyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncDeleteLobby;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncEndGame;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncRoomPlayerCount;
import com.game.contraband.infrastructure.actor.spy.SpyGameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import java.util.Map;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyLifecycleCoordinatorTest {

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
    void 호스트_클라이언트_세션을_초기화하고_메시지를_전달한다() {
        // given
        TestContext context = createContext();

        Behavior<LobbyCommand> behavior = Behaviors.setup(
                actorContext -> {
                    context.coordinator()
                           .initializeHost(actorContext, context.chatRelay());
                    return Behaviors.stopped();
                }
        );

        // when
        BehaviorTestUtils.createHarness(behavior);

        // then
        ActorTestUtils.expectMessages(
                context.hostSession().monitor(),
                PropagateCreateLobby.class,
                UpdateLobby.class,
                PropagateCreatedLobby.class,
                PropagateWelcomeMessage.class,
                SyncLobbyChat.class
        );
    }

    @Test
    void 현재_플레이어_수와_정보를_부모에_알린다() {
        // given
        TestContext context = createContext();

        // when
        context.coordinator()
               .notifyRoomPlayerCount(false);

        // then
        GameManagerCommand received = context.parent().monitor().receiveMessage();
        SyncRoomPlayerCount actual = (SyncRoomPlayerCount) received;

        assertAll(
                () -> assertThat(actual.roomId()).isEqualTo(100L),
                () -> assertThat(actual.gameStarted()).isFalse()
        );
    }

    @Test
    void 로비_삭제를_부모에_알린다() {
        // given
        TestContext context = createContext();

        // when
        context.coordinator().notifyDeleteLobby();

        // then
        GameManagerCommand received = context.parent().monitor().receiveMessage();
        SyncDeleteLobby actual = (SyncDeleteLobby) received;
        assertThat(actual.roomId()).isEqualTo(100L);
    }

    @Test
    void 게임_종료를_부모에_알린다() {
        // given
        TestContext context = createContext();

        // when
        context.coordinator().notifyEndGame();

        // then
        GameManagerCommand received = context.parent().monitor().receiveMessage();
        SyncEndGame actual = (SyncEndGame) received;

        assertThat(actual.roomId()).isEqualTo(100L);
    }

    private TestContext createContext() {
        ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
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
        SpyGameLifecycleEventPublisher lifecyclePublisher = new SpyGameLifecycleEventPublisher();

        LobbyRuntimeState lobbyState = new LobbyRuntimeState(100L, 1L, "entity-1", defaultLobby());
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, hostSession.ref())
        );
        LobbyExternalGateway gateway = new LobbyExternalGateway(
                lobbyChat.ref(),
                parent.ref(),
                new DummyChatMessageEventPublisher(),
                lifecyclePublisher,
                new DummyChatBlacklistRepository()
        );
        LobbyLifecycleCoordinator coordinator = new LobbyLifecycleCoordinator(
                lobbyState,
                registry,
                gateway
        );
        LobbyChatRelay chatRelay = new LobbyChatRelay(gateway);

        return new TestContext(
                coordinator,
                chatRelay,
                hostSession,
                lobbyChat,
                parent
        );
    }

    private Lobby defaultLobby() {
        PlayerProfile host = PlayerProfile.create(1L, "호스트", TeamRole.SMUGGLER);

        return Lobby.create(100L, "로비", host, 4);
    }

    private record TestContext(
            LobbyLifecycleCoordinator coordinator,
            LobbyChatRelay chatRelay,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> hostSession,
            ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat,
            ActorTestUtils.MonitoredActor<GameManagerCommand> parent
    ) { }
}
