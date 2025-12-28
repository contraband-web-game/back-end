package com.game.contraband.infrastructure.actor.game.chat.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateKickedMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateLeftMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateMaskedChatBatch;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateNewMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateWelcomeMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher.ChatMessageEvent;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.JoinMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.KickedMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LeftMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.MaskBlockedPlayerMessages;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.SendMessage;
import com.game.contraband.infrastructure.actor.spy.SpyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.spy.SpyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
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
class LobbyChatActorTest {

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
    void 채팅_메시지를_브로드캐스트하고_이벤트를_발행한다() {
        // given
        TestContext context = createContextWithJoined();

        // when
        context.actor().ref().tell(new SendMessage(1L, "호스트", "내용"));

        // then
        PropagateNewMessage hostActual = (PropagateNewMessage) context.host().monitor().receiveMessage();
        PropagateNewMessage joinedActual = (PropagateNewMessage) context.joined().monitor().receiveMessage();
        ChatMessageEvent published = context.publisher().publishedEvents().get(0);

        assertAll(
                () -> assertThat(hostActual.chatMessage().message()).isEqualTo("내용"),
                () -> assertThat(joinedActual.chatMessage().message()).isEqualTo("내용"),
                () -> assertThat(published.chatEvent()).isEqualTo(ChatEventType.LOBBY_CHAT)
        );
    }

    @Test
    void 빈_메시지로는_채팅을_할_수_없다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().ref().tell(new SendMessage(1L, "호스트", " "));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) context.host().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.code()).isEqualTo(ExceptionCode.CHAT_MESSAGE_EMPTY),
                () -> ActorTestUtils.expectNoMessages(context.joined().monitor())
        );
    }

    @Test
    void 차단된_플레이어는_채팅을_할_수_없다() {
        // given
        SpyChatBlacklistRepository repository = new SpyChatBlacklistRepository();

        repository.setBlockedReturn(true);

        TestContext context = createContextWithJoined(repository);

        // when
        context.actor().ref().tell(new SendMessage(1L, "호스트", "내용"));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) context.host().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.code()).isEqualTo(ExceptionCode.CHAT_USER_BLOCKED),
                () -> ActorTestUtils.expectNoMessages(context.joined().monitor())
        );
    }

    @Test
    void 플레이어가_로비에_참여하면_입장_메시지를_전파한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> newcomer = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );

        // when
        context.actor().ref().tell(new JoinMessage(newcomer.ref(), 3L, "신규"));

        // then
        PropagateWelcomeMessage hostActual = (PropagateWelcomeMessage) context.host().monitor().receiveMessage();
        PropagateWelcomeMessage joinedActual = (PropagateWelcomeMessage) newcomer.monitor().receiveMessage();

        assertAll(
                () -> assertThat(hostActual.playerName()).isEqualTo("신규"),
                () -> assertThat(joinedActual.playerName()).isEqualTo("신규")
        );
    }

    @Test
    void 플레이어가_로비에서_나가면_이를_전파한다() {
        // given
        TestContext context = createContextWithJoined();

        // when
        context.actor().ref().tell(new LeftMessage(2L, "참가자"));

        // then
        PropagateLeftMessage actual = (PropagateLeftMessage) context.host().monitor().receiveMessage();

        assertThat(actual.playerName()).isEqualTo("참가자");
    }

    @Test
    void 특정_플레이어가_강퇴되었음을_알린다() {
        // given
        TestContext context = createContextWithJoined();

        // when
        context.actor().ref().tell(new KickedMessage(2L, "참가자"));

        // then
        PropagateKickedMessage actual = (PropagateKickedMessage) context.host().monitor().receiveMessage();

        assertThat(actual.playerName()).isEqualTo("참가자");
    }

    @Test
    void 차단된_사용자의_메시지를_마스킹한다() {
        // given
        TestContext context = createContextWithJoined();

        context.actor().ref().tell(new SendMessage(1L, "호스트", "내용"));

        ActorTestUtils.drainMessages(context.host().monitor());
        ActorTestUtils.drainMessages(context.joined().monitor());

        // when
        context.actor().ref().tell(new MaskBlockedPlayerMessages(1L));

        // then
        PropagateMaskedChatBatch hostActual = (PropagateMaskedChatBatch) context.host().monitor().receiveMessage();
        PropagateMaskedChatBatch joinedActual = (PropagateMaskedChatBatch) context.joined().monitor().receiveMessage();

        assertAll(
                () -> assertThat(hostActual.chatEvent()).isEqualTo(ChatEventType.LOBBY_CHAT),
                () -> assertThat(joinedActual.messageIds()).isEqualTo(hostActual.messageIds())
        );
    }

    @Test
    void 로비_채팅_Actor_종료_시_블랙리스트_리스너를_해제한다() {
        // given
        SpyChatBlacklistRepository repository = new SpyChatBlacklistRepository();
        TestContext context = createContextWithJoined(repository);

        // when
        actorTestKit.stop(context.actor().ref());

        // then
        assertThat(repository.isUnsubscribeInvoked()).isTrue();
    }

    private TestContext createContext() {
        return createContextWithJoined(new SpyChatBlacklistRepository());
    }

    private TestContext createContextWithJoined() {
        return createContextWithJoined(new SpyChatBlacklistRepository());
    }

    private TestContext createContextWithJoined(SpyChatBlacklistRepository repository) {
        ActorTestUtils.MonitoredActor<ClientSessionCommand> host = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> joined = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        SpyChatMessageEventPublisher publisher = new SpyChatMessageEventPublisher();

        Behavior<LobbyChatActor.LobbyChatCommand> behavior = LobbyChatActor.create(
                1L,
                "entity",
                host.ref(),
                1L,
                publisher,
                repository
        );
        ActorTestUtils.MonitoredActor<LobbyChatActor.LobbyChatCommand> actor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatActor.LobbyChatCommand.class,
                behavior
        );

        actor.ref().tell(new LobbyChatActor.JoinMessage(joined.ref(), 2L, "참가자"));
        ActorTestUtils.drainMessages(host.monitor());
        ActorTestUtils.drainMessages(joined.monitor());

        return new TestContext(actor, host, joined, publisher);
    }

    private record TestContext(
            ActorTestUtils.MonitoredActor<LobbyChatActor.LobbyChatCommand> actor,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> host,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> joined,
            SpyChatMessageEventPublisher publisher
    ) { }
}
