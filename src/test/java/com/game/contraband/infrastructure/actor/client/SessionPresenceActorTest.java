package com.game.contraband.infrastructure.actor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.PresenceCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RequestSessionReconnect;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.SendWebSocketPing;
import com.game.contraband.infrastructure.actor.client.SessionPresenceActor.RequestRoomDirectoryPageCommand;
import com.game.contraband.infrastructure.actor.client.SessionPresenceActor.ResubscribeRoomDirectory;
import com.game.contraband.infrastructure.actor.client.SessionPresenceActor.SessionHealthCheck;
import com.game.contraband.infrastructure.actor.client.SessionPresenceActor.SessionPongReceived;
import com.game.contraband.infrastructure.actor.client.SessionPresenceActor.UnregisterSessionCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.LocalDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.RegisterSession;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.RequestRoomDirectoryPage;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.UnregisterSession;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.time.Duration;
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
class SessionPresenceActorTest {

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
    void 클라이언트_세션_헬스_체크시_ping을_보낸다() {
        // given
        TestContext context = createContext();

        context.directory().monitor().receiveMessage();

        // when
        context.actor().ref().tell(new SessionHealthCheck());

        // then
        SendWebSocketPing actual = (SendWebSocketPing) context.outbound().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual).isNotNull(),
                () -> ActorTestUtils.expectNoMessages(context.outbound().monitor(), Duration.ofMillis(300L))
        );
    }

    @Test
    void 헬스_체크가_연속으로_실패하면_웹소켓_새션_재연결을_요청한다() {
        // given
        TestContext context = createContext();

        context.directory().monitor().receiveMessage();

        // when
        context.actor().ref().tell(new SessionHealthCheck());
        context.outbound().monitor().receiveMessage();
        context.actor().ref().tell(new SessionHealthCheck());

        // then
        SendWebSocketPing ping = (SendWebSocketPing) context.outbound().monitor().receiveMessage();
        RequestSessionReconnect reconnect = (RequestSessionReconnect) context.outbound().monitor().receiveMessage();

        assertAll(
                () -> assertThat(ping).isNotNull(),
                () -> assertThat(reconnect).isNotNull()
        );
    }

    @Test
    void pong을_받으면_헬스체크_누락_횟수를_초기화한다() {
        // given
        TestContext context = createContext();

        context.directory().monitor().receiveMessage();
        context.actor().ref().tell(new SessionHealthCheck());
        context.outbound().monitor().receiveMessage();

        context.actor().ref().tell(new SessionPongReceived());

        // when
        context.actor().ref().tell(new SessionHealthCheck());

        // then
        SendWebSocketPing actual = (SendWebSocketPing) context.outbound().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual).isNotNull(),
                () -> ActorTestUtils.expectNoMessages(context.outbound().monitor(), Duration.ofMillis(300L))
        );
    }

    @Test
    void 게임_방_목록_안내_Actor_재_구독을_요청한다() {
        // given
        TestContext context = createContext();
        context.directory().monitor().receiveMessage();

        // when
        context.actor().ref().tell(new ResubscribeRoomDirectory());

        // then
        RegisterSession actual = (RegisterSession) context.directory().monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.userId()).isEqualTo(1L),
                () -> assertThat(actual.session()).isEqualTo(context.gateway().ref())
        );
    }

    @Test
    void 게임_방_목록_페이지를_요청한다() {
        // given
        TestContext context = createContext();
        context.directory().monitor().receiveMessage();

        // when
        context.actor().ref().tell(new RequestRoomDirectoryPageCommand(2, 5));

        // then
        RequestRoomDirectoryPage actual = (RequestRoomDirectoryPage) context.directory().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.page()).isEqualTo(2),
                () -> assertThat(actual.size()).isEqualTo(5)
        );
    }

    @Test
    void 클라이언트_세션_해제_요청을_전파한다() {
        // given
        TestContext context = createContext();
        context.directory().monitor().receiveMessage();

        // when
        context.actor().ref().tell(new UnregisterSessionCommand());

        // then
        UnregisterSession actual = (UnregisterSession) context.directory().monitor().receiveMessage();
        assertThat(actual.userId()).isEqualTo(1L);
    }

    @Test
    void 종료시_클라이언트_세션을_해제한다() {
        // given
        TestContext context = createContext();

        context.directory().monitor().receiveMessage();

        // when
        actorTestKit.stop(context.actor().ref());

        // then
        UnregisterSession actual = (UnregisterSession) context.directory().monitor().receiveMessage();

        assertThat(actual.userId()).isEqualTo(1L);
    }

    private TestContext createContext() {
        ActorTestUtils.MonitoredActor<OutboundCommand> outbound = ActorTestUtils.spawnMonitored(
                actorTestKit,
                OutboundCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> gateway = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<LocalDirectoryCommand> directory =
                ActorTestUtils.spawnMonitored(
                        actorTestKit,
                        LocalDirectoryCommand.class,
                        Behaviors.ignore()
                );

        Behavior<PresenceCommand> behavior = SessionPresenceActor.create(
                1L,
                outbound.ref(),
                gateway.ref(),
                directory.ref()
        );
        ActorTestUtils.MonitoredActor<PresenceCommand> actor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                PresenceCommand.class,
                behavior
        );

        return new TestContext(actor, outbound, gateway, directory);
    }

    private record TestContext(
            ActorTestUtils.MonitoredActor<PresenceCommand> actor,
            ActorTestUtils.MonitoredActor<OutboundCommand> outbound,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> gateway,
            ActorTestUtils.MonitoredActor<LocalDirectoryCommand> directory
    ) {
        public ActorTestUtils.MonitoredActor<PresenceCommand> actor() {
            return actor;
        }

        public ActorTestUtils.MonitoredActor<OutboundCommand> outbound() {
            return outbound;
        }

        public ActorTestUtils.MonitoredActor<ClientSessionCommand> gateway() {
            return gateway;
        }

        public ActorTestUtils.MonitoredActor<LocalDirectoryCommand> directory() {
            return directory;
        }
    }
}
