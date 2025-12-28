package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncLobbyChat;
import com.game.contraband.infrastructure.actor.dummy.DummyGameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.KickedMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.Shutdown;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.dummy.DummyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
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
class LobbyChatRelayTest {

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
    void 클라이언트_세션에_로비_채팅을_동기화한다() {
        // given
        TestContext context = createContext();

        // when
        context.relay().syncLobbyChat(context.clientSession().ref());

        // then
        SyncLobbyChat actual = (SyncLobbyChat) context.clientSession().monitor().receiveMessage();
        assertThat(actual.lobbyChat()).isEqualTo(context.gateway().lobbyChat());
    }

    @Test
    void 채팅으로_메시지를_전달한다() {
        // given
        TestContext context = createContext();
        LobbyChatCommand command = new KickedMessage(1L, "강퇴");

        // when
        context.relay().sendToChat(command);

        // then
        KickedMessage actual = (KickedMessage) context.lobbyChat().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(1L),
                () -> assertThat(actual.playerName()).isEqualTo("강퇴")
        );
    }

    @Test
    void 채팅을_중지한다() {
        // given
        TestContext context = createContext();

        // when
        ActorTestUtils.spawnMonitored(
                actorTestKit,
                Object.class,
                Behaviors.setup(
                        actorContext -> {
                            context.relay().stopChat();
                            return Behaviors.stopped();
                        }
                )
        );

        // then
        context.lobbyChat().monitor().expectTerminated(context.lobbyChat().ref());
    }

    private TestContext createContext() {
        ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatCommand.class,
                Behaviors.receiveMessage(
                        message -> {
                            if (message instanceof Shutdown) {
                                return Behaviors.stopped();
                            }
                            return Behaviors.same();
                        }
                )
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> clientSession = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
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

        return new TestContext(
                relay,
                gateway,
                lobbyChat,
                clientSession,
                parent
        );
    }

    private record TestContext(
            LobbyChatRelay relay,
            LobbyExternalGateway gateway,
            ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> clientSession,
            ActorTestUtils.MonitoredActor<GameManagerCommand> parent
    ) { }
}
