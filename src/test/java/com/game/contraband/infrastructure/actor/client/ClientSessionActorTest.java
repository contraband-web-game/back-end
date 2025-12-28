package com.game.contraband.infrastructure.actor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.application.game.dto.ActiveGameView;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ChatCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClearActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.FetchRoomDirectoryPage;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.InboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.PresenceCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.QueryActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ReSyncClientSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UpdateActiveGame;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.lang.reflect.Constructor;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.pattern.StatusReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"NonAsciiCharacters", "unchecked"})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientSessionActorTest {

    private static final Long PLAYER_ID = 1L;

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
    void Outbound_메시지를_위임한다() {
        // given
        TestContext context = createContext();
        DummyOutbound outboundMessage = new DummyOutbound();

        // when
        context.actor().tell(outboundMessage);

        // then
        DummyOutbound actual = (DummyOutbound) context.outbound().monitor().receiveMessage();

        assertThat(actual).isEqualTo(outboundMessage);
    }

    @Test
    void Inbound_메시지를_위임한다() {
        // given
        TestContext context = createContext();
        DummyInbound inboundMessage = new DummyInbound();

        // when
        context.actor().tell(inboundMessage);

        // then
        DummyInbound actual = (DummyInbound) context.inbound().monitor().receiveMessage();

        assertThat(actual).isEqualTo(inboundMessage);
    }

    @Test
    void Presence_메시지를_위임한다() {
        // given
        TestContext context = createContext();
        DummyPresence presenceMessage = new DummyPresence();

        // when
        context.actor().tell(presenceMessage);

        // then
        DummyPresence actual = (DummyPresence) context.presence().monitor().receiveMessage();

        assertThat(actual).isEqualTo(presenceMessage);
    }

    @Test
    void 채팅_메시지를_위임한다() {
        // given
        TestContext context = createContext();
        DummyChat chatMessage = new DummyChat();

        // when
        context.actor().tell(chatMessage);

        // then
        DummyChat actual = (DummyChat) context.chat().monitor().receiveMessage();

        assertThat(actual).isEqualTo(chatMessage);
    }

    @Test
    void 진행_중인_게임을_설정한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new UpdateActiveGame(10L, "entity"));

        // then
        ActorTestUtils.MonitoredActor<StatusReply<ActiveGameView>> reply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                (Class<StatusReply<ActiveGameView>>) (Class<?>) StatusReply.class,
                Behaviors.ignore()
        );
        context.actor().tell(new QueryActiveGame(reply.ref()));
        StatusReply<ActiveGameView> actual = reply.monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.getValue().roomId()).isEqualTo(10L),
                () -> assertThat(actual.getValue().entityId()).isEqualTo("entity")
        );
    }

    @Test
    void 진행_중인_게임을_초기화한다() {
        // given
        TestContext context = createContext();
        context.actor().tell(new UpdateActiveGame(10L, "entity"));
        context.actor().tell(new ClearActiveGame());

        // when
        ActorTestUtils.MonitoredActor<StatusReply<ActiveGameView>> reply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                (Class<StatusReply<ActiveGameView>>) (Class<?>) StatusReply.class,
                Behaviors.ignore()
        );
        context.actor().tell(new QueryActiveGame(reply.ref()));

        // then
        StatusReply<ActiveGameView> actual = reply.monitor().receiveMessage();

        assertThat(actual.getValue()).isNull();
    }

    @Test
    void 세션을_재_동기화한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new ReSyncClientSession(PLAYER_ID));

        // then
        SessionInboundActor.ReSyncConnection reSyncConnection =
                (SessionInboundActor.ReSyncConnection) context.inbound().monitor().receiveMessage();
        SessionPresenceActor.ResubscribeRoomDirectory resubscribe =
                (SessionPresenceActor.ResubscribeRoomDirectory) context.presence().monitor().receiveMessage();

        assertAll(
                () -> assertThat(reSyncConnection.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(resubscribe).isNotNull()
        );
    }

    @Test
    void 게임_방_목록을_요청한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new FetchRoomDirectoryPage(2, 5));

        // then
        SessionPresenceActor.RequestRoomDirectoryPageCommand actual =
                (SessionPresenceActor.RequestRoomDirectoryPageCommand) context.presence().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.page()).isEqualTo(2),
                () -> assertThat(actual.size()).isEqualTo(5)
        );
    }

    @Test
    void 자기_자신_종료_시_세션을_해제한다() {
        // given
        TestContext context = createContext();

        // when
        actorTestKit.stop(context.actor());

        // then
        SessionPresenceActor.UnregisterSessionCommand actual =
                (SessionPresenceActor.UnregisterSessionCommand) context.presence().monitor().receiveMessage();

        assertThat(actual).isNotNull();
    }

    private TestContext createContext() {
        ActorTestUtils.MonitoredActor<OutboundCommand> outbound = ActorTestUtils.spawnMonitored(
                actorTestKit,
                OutboundCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<InboundCommand> inbound = ActorTestUtils.spawnMonitored(
                actorTestKit,
                InboundCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<PresenceCommand> presence = ActorTestUtils.spawnMonitored(
                actorTestKit,
                PresenceCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ChatCommand> chat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ChatCommand.class,
                Behaviors.ignore()
        );
        Behavior<ClientSessionCommand> behavior = createClientSessionBehavior(
                outbound.ref(),
                inbound.ref(),
                presence.ref(),
                chat.ref()
        );
        ActorRef<ClientSessionCommand> actor = actorTestKit.spawn(behavior);
        return new TestContext(actor, outbound, inbound, presence, chat);
    }

    private Behavior<ClientSessionCommand> createClientSessionBehavior(
            ActorRef<OutboundCommand> outbound,
            ActorRef<InboundCommand> inbound,
            ActorRef<PresenceCommand> presence,
            ActorRef<ChatCommand> chat
    ) {
        return Behaviors.setup(
                context -> {
                    try {
                        Constructor<ClientSessionActor> constructor = ClientSessionActor.class.getDeclaredConstructor(
                                ActorContext.class,
                                ActorRef.class,
                                ActorRef.class,
                                ActorRef.class,
                                ActorRef.class
                        );
                        constructor.setAccessible(true);
                        return constructor.newInstance(context, outbound, inbound, presence, chat);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }
        );
    }

    private record TestContext(
            ActorRef<ClientSessionCommand> actor,
            ActorTestUtils.MonitoredActor<OutboundCommand> outbound,
            ActorTestUtils.MonitoredActor<InboundCommand> inbound,
            ActorTestUtils.MonitoredActor<PresenceCommand> presence,
            ActorTestUtils.MonitoredActor<ChatCommand> chat
    ) {
        public ActorRef<ClientSessionCommand> actor() {
            return actor;
        }

        public ActorTestUtils.MonitoredActor<OutboundCommand> outbound() {
            return outbound;
        }

        public ActorTestUtils.MonitoredActor<InboundCommand> inbound() {
            return inbound;
        }

        public ActorTestUtils.MonitoredActor<PresenceCommand> presence() {
            return presence;
        }

        public ActorTestUtils.MonitoredActor<ChatCommand> chat() {
            return chat;
        }
    }

    private static class DummyOutbound implements OutboundCommand { }

    private static class DummyInbound implements InboundCommand { }

    private static class DummyPresence implements PresenceCommand { }

    private static class DummyChat implements ChatCommand, ContrabandGameCommand { }
}
