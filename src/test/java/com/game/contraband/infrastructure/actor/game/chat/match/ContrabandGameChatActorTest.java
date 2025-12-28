package com.game.contraband.infrastructure.actor.game.chat.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateMaskedChatMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateRoundChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateSmugglerTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.spy.SpyChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.spy.SpyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandGameChatActorTest {

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
    void 밀수꾼_팀_채팅을_전파하고_이벤트를_발행한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().ref().tell(new ContrabandGameChatActor.ChatSmugglerTeam(1L, "밀수꾼", "내용"));

        // then
        PropagateSmugglerTeamChat actual = (PropagateSmugglerTeamChat) context.smuggler().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.chatMessage().message()).isEqualTo("내용"),
                () -> assertThat(context.publisher().hasEvent(ChatEventType.SMUGGLER_TEAM_CHAT)).isTrue()
        );
    }

    @Test
    void 차단된_사용자는_팀_채팅이_거부된다() {
        // given
        SpyChatBlacklistRepository repository = new SpyChatBlacklistRepository();
        repository.setBlockedReturn(true);
        TestContext context = createContext(repository);

        // when
        context.actor().ref().tell(new ContrabandGameChatActor.ChatSmugglerTeam(1L, "밀수꾼", "내용"));

        // then
        HandleExceptionMessage actual = (HandleExceptionMessage) context.smuggler().monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.code()).isEqualTo(ExceptionCode.CHAT_USER_BLOCKED),
                () -> ActorTestUtils.expectNoMessages(context.inspector().monitor()),
                () -> assertThat(context.publisher().publishedEvents()).isEmpty()
        );
    }

    @Test
    void 라운드_채팅을_전파한다() {
        // given
        TestContext context = createContext();

        context.actor().ref().tell(new ContrabandGameChatActor.SyncRoundChatId(1L, 2L));

        // when
        context.actor().ref().tell(new ContrabandGameChatActor.ChatInRound(1L, "밀수", "라운드", 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smuggler().monitor(), PropagateRoundChat.class),
                () -> ActorTestUtils.expectMessages(context.inspector().monitor(), PropagateRoundChat.class)
        );
    }

    @Test
    void 라운드_참여자가_없으면_채팅이_무시된다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().ref().tell(new ContrabandGameChatActor.ChatInRound(1L, "밀수", "라운드", 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectNoMessages(context.smuggler().monitor()),
                () -> ActorTestUtils.expectNoMessages(context.inspector().monitor())
        );
    }

    @Test
    void 차단된_플레이어_메시지를_마스킹한다() {
        // given
        TestContext context = createContext();
        context.actor().ref().tell(new ContrabandGameChatActor.ChatSmugglerTeam(1L, "밀수", "팀메시지"));
        context.actor().ref().tell(new ContrabandGameChatActor.ChatInspectorTeam(2L, "검사", "다른팀"));
        context.actor().ref().tell(new ContrabandGameChatActor.SyncRoundChatId(1L, 2L));
        context.actor().ref().tell(new ContrabandGameChatActor.ChatInRound(1L, "밀수", "라운드", 1));

        // when
        context.actor().ref().tell(new ContrabandGameChatActor.MaskBlockedPlayerMessages(1L));

        // then
        assertAll(
                () -> assertThat(collectMasked(context.smuggler().monitor())).isNotEmpty(),
                () -> assertThat(collectMasked(context.inspector().monitor())).isNotEmpty()
        );
    }

    private List<PropagateMaskedChatMessage> collectMasked(org.apache.pekko.actor.testkit.typed.javadsl.TestProbe<ClientSessionCommand> probe) {
        List<PropagateMaskedChatMessage> collected = new ArrayList<>();
        while (true) {
            try {
                ClientSessionCommand message = probe.receiveMessage(Duration.ofMillis(50));
                if (message instanceof PropagateMaskedChatMessage masked) {
                    collected.add(masked);
                }
            } catch (AssertionError timeout) {
                break;
            }
        }
        return collected;
    }

    private TestContext createContext() {
        return createContext(new SpyChatBlacklistRepository());
    }

    private TestContext createContext(SpyChatBlacklistRepository repository) {
        ActorTestUtils.MonitoredActor<ClientSessionCommand> smuggler = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                org.apache.pekko.actor.typed.javadsl.Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<ClientSessionCommand> inspector = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                org.apache.pekko.actor.typed.javadsl.Behaviors.ignore()
        );
        Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> sessionsByTeam = new HashMap<>();
        sessionsByTeam.put(TeamRole.SMUGGLER, Map.of(1L, smuggler.ref()));
        sessionsByTeam.put(TeamRole.INSPECTOR, Map.of(2L, inspector.ref()));

        SpyChatMessageEventPublisher publisher = new SpyChatMessageEventPublisher();
        Behavior<ContrabandGameChatActor.ContrabandGameChatCommand> behavior = ContrabandGameChatActor.create(
                1L,
                "entity",
                publisher,
                sessionsByTeam,
                repository
        );
        ActorTestUtils.MonitoredActor<ContrabandGameChatActor.ContrabandGameChatCommand> actor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatActor.ContrabandGameChatCommand.class,
                behavior
        );

        return new TestContext(actor, smuggler, inspector, publisher);
    }

    private record TestContext(
            ActorTestUtils.MonitoredActor<ContrabandGameChatActor.ContrabandGameChatCommand> actor,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> smuggler,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> inspector,
            SpyChatMessageEventPublisher publisher
    ) { }
}
