package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.dummy.DummyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.dummy.DummyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.KickedMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncDeleteLobby;
import com.game.contraband.infrastructure.actor.spy.SpyGameLifecycleEventPublisher;
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
class LobbyExternalGatewayTest {

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
    void 로비_채팅으로_메시지를_전달한다() {
        // given
        TestContext context = createContext();
        LobbyChatCommand command = new KickedMessage(10L, "강퇴됨");

        // when
        context.gateway().sendToLobbyChat(command);

        // then
        KickedMessage actual = (KickedMessage) context.lobbyChat().monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(10L),
                () -> assertThat(actual.playerName()).isEqualTo("강퇴됨")
        );
    }

    @Test
    void 부모에게_명령을_전달한다() {
        // given
        TestContext context = createContext();
        GameManagerCommand command = new SyncDeleteLobby(200L);

        // when
        context.gateway().notifyParent(command);

        // then
        SyncDeleteLobby actual = (SyncDeleteLobby) context.parent().monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.roomId()).isEqualTo(200L)
        );
    }

    @Test
    void 게임_시작을_발행한다() {
        // given
        TestContext context = createContext();

        // when
        context.gateway().publishGameStarted("entity-1", 400L);

        // then
        assertAll(
                () -> assertThat(context.lifecyclePublisher().getEntityId()).isEqualTo("entity-1"),
                () -> assertThat(context.lifecyclePublisher().getRoomId()).isEqualTo(400L)
        );
    }

    private TestContext createContext() {
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

        LobbyExternalGateway gateway = new LobbyExternalGateway(
                lobbyChat.ref(),
                parent.ref(),
                new DummyChatMessageEventPublisher(),
                lifecyclePublisher,
                new DummyChatBlacklistRepository()
        );

        return new TestContext(
                gateway,
                lobbyChat,
                parent,
                lifecyclePublisher
        );
    }

    private record TestContext(
            LobbyExternalGateway gateway,
            ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat,
            ActorTestUtils.MonitoredActor<GameManagerCommand> parent,
            SpyGameLifecycleEventPublisher lifecyclePublisher
    ) { }
}
