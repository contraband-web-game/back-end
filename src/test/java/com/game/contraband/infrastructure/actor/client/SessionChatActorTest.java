package com.game.contraband.infrastructure.actor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ChatCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearLobbyChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateInspectorTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateKickedMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateLeftMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateMaskedChatMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateNewMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateRoundChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateSmugglerTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateWelcomeMessage;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.RequestSendChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.RequestSendRoundChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.RequestSendTeamChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncLobbyChat;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.SendMessage;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ChatInRound;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ChatInspectorTeam;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ChatSmugglerTeam;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.spy.SpyChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.spy.SpyClientWebSocketMessageSender;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.time.Duration;
import java.time.LocalDateTime;
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
class SessionChatActorTest {

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
    void 채팅_입장_메시지를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new PropagateWelcomeMessage("플레이어"));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().chatWelcomeName != null);

        assertThat(context.sender().chatWelcomeName).isEqualTo("플레이어");
    }

    @Test
    void 새_채팅을_전파한다() {
        // given
        TestContext context = createContext();
        ChatMessage chatMessage = chatMessage();

        // when
        context.actor().tell(new PropagateNewMessage(chatMessage));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().chatMessageSent != null);

        assertThat(context.sender().chatMessageSent).isEqualTo(chatMessage);
    }

    @Test
    void 채팅_퇴장을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new PropagateLeftMessage("플레이어"));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().chatLeftName != null);
        assertThat(context.sender().chatLeftName).isEqualTo("플레이어");
    }

    @Test
    void 채팅_강퇴를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new PropagateKickedMessage("플레이어"));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().chatKickedName != null);

        assertThat(context.sender().chatKickedName).isEqualTo("플레이어");
    }

    @Test
    void 차단된_플레이어의_채팅_마스킹을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        context.actor().tell(new PropagateMaskedChatMessage(5L, ChatEventType.ROUND_CHAT));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().maskedMessageId != null);

        assertAll(
                () -> assertThat(context.sender().maskedMessageId).isEqualTo(5L),
                () -> assertThat(context.sender().maskedChatEvent).isEqualTo(ChatEventType.ROUND_CHAT.name())
        );
    }

    @Test
    void 밀수꾼_팀_채팅을_전파한다() {
        // given
        TestContext context = createContext();
        ChatMessage chatMessage = chatMessage();

        // when
        context.actor().tell(new PropagateSmugglerTeamChat(chatMessage));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().smugglerTeamChatMessage != null);
        assertThat(context.sender().smugglerTeamChatMessage).isEqualTo(chatMessage);
    }

    @Test
    void 검사관_팀_채팅을_전파한다() {
        // given
        TestContext context = createContext();
        ChatMessage chatMessage = chatMessage();

        // when
        context.actor().tell(new PropagateInspectorTeamChat(chatMessage));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().inspectorTeamChatMessage != null);
        assertThat(context.sender().inspectorTeamChatMessage).isEqualTo(chatMessage);
    }

    @Test
    void 라운드_채팅을_전파한다() {
        // given
        TestContext context = createContext();
        ChatMessage chatMessage = chatMessage();

        // when
        context.actor().tell(new PropagateRoundChat(chatMessage));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().roundChatMessage != null);
        assertThat(context.sender().roundChatMessage).isEqualTo(chatMessage);
    }

    @Test
    void 밀수꾼_팀_채팅을_전송한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.SMUGGLER));

        // when
        context.actor().tell(new RequestSendTeamChat(PLAYER_ID, "플레이어", "팀 채팅"));

        // then
        ChatSmugglerTeam actual = (ChatSmugglerTeam) contrabandChat.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.playerName()).isEqualTo("플레이어"),
                () -> assertThat(actual.message()).isEqualTo("팀 채팅")
        );
    }

    @Test
    void 검사관_팀_채팅을_전송한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.INSPECTOR));

        // when
        context.actor().tell(new RequestSendTeamChat(PLAYER_ID, "플레이어", "검사관 팀"));

        // then
        ChatInspectorTeam actual = (ChatInspectorTeam) contrabandChat.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.playerName()).isEqualTo("플레이어"),
                () -> assertThat(actual.message()).isEqualTo("검사관 팀")
        );
    }

    @Test
    void 라운드_채팅을_전송한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.SMUGGLER));

        // when
        context.actor().tell(new RequestSendRoundChat(PLAYER_ID, "플레이어", "라운드", 2));

        // then
        ChatInRound actual = (ChatInRound) contrabandChat.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.playerName()).isEqualTo("플레이어"),
                () -> assertThat(actual.message()).isEqualTo("라운드"),
                () -> assertThat(actual.currentRound()).isEqualTo(2)
        );
    }

    @Test
    void 차단된_사용자는_팀_채팅을_전송할_수_없다() {
        // given
        TestContext context = createContext();
        context.chatBlacklistRepository().setBlockedReturn(true);
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.SMUGGLER));

        // when
        context.actor().tell(new RequestSendTeamChat(PLAYER_ID, "플레이어", "차단"));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().exceptionCode != null);
        assertAll(
                () -> assertThat(context.sender().exceptionCode).isEqualTo(ExceptionCode.CHAT_USER_BLOCKED),
                () -> ActorTestUtils.expectNoMessages(contrabandChat.monitor(), Duration.ofMillis(300L)),
                () -> assertThat(context.chatBlacklistRepository().lastCheckedPlayerId()).isEqualTo(PLAYER_ID)
        );
    }

    @Test
    void 차단된_사용자는_라운드_채팅을_전송할_수_없다() {
        // given
        TestContext context = createContext();
        context.chatBlacklistRepository().setBlockedReturn(true);
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.SMUGGLER));

        // when
        context.actor().tell(new RequestSendRoundChat(PLAYER_ID, "플레이어", "차단", 1));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().exceptionCode != null);

        assertAll(
                () -> assertThat(context.sender().exceptionCode).isEqualTo(ExceptionCode.CHAT_USER_BLOCKED),
                () -> ActorTestUtils.expectNoMessages(contrabandChat.monitor(), Duration.ofMillis(300L))
        );
    }

    @Test
    void 차단된_사용자는_로비_채팅을_입력할_수_없다() {
        // given
        TestContext context = createContext();
        context.chatBlacklistRepository().setBlockedReturn(true);
        ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncLobbyChat(lobbyChat.ref()));

        // when
        context.actor().tell(new RequestSendChat(PLAYER_ID, "플레이어", "차단"));

        // then
        ActorTestUtils.waitUntilCondition(() -> context.sender().exceptionCode != null);
        assertAll(
                () -> assertThat(context.sender().exceptionCode).isEqualTo(ExceptionCode.CHAT_USER_BLOCKED),
                () -> ActorTestUtils.expectNoMessages(lobbyChat.monitor(), Duration.ofMillis(300L))
        );
    }

    @Test
    void 로비_채팅을_전송한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncLobbyChat(lobbyChat.ref()));

        // when
        context.actor().tell(new RequestSendChat(PLAYER_ID, "플레이어", "로비"));

        // then
        SendMessage actual = (SendMessage) lobbyChat.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.writerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.writerName()).isEqualTo("플레이어"),
                () -> assertThat(actual.message()).isEqualTo("로비")
        );
    }

    @Test
    void 로비가_없는_경우_밀수꾼의_채팅은_밀수꾼_팀_채팅으로_전송한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.SMUGGLER));

        // when
        context.actor().tell(new RequestSendChat(PLAYER_ID, "플레이어", "팀 메시지"));

        // then
        ChatSmugglerTeam actual = (ChatSmugglerTeam) contrabandChat.monitor().receiveMessage();
        
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.playerName()).isEqualTo("플레이어"),
                () -> assertThat(actual.message()).isEqualTo("팀 메시지")
        );
    }

    @Test
    void 로비가_없는_경우_검사관의_채팅은_검사관_팀_채팅으로_전송한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.INSPECTOR));

        // when
        context.actor().tell(new RequestSendChat(PLAYER_ID, "플레이어", "팀 메시지"));

        // then
        ChatInspectorTeam actual = (ChatInspectorTeam) contrabandChat.monitor().receiveMessage();
        
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.playerName()).isEqualTo("플레이어"),
                () -> assertThat(actual.message()).isEqualTo("팀 메시지")
        );
    }

    @Test
    void 게임_채팅_초기화_후에는_팀_채팅을_입력할_수_없다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> contrabandChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncContrabandGameChat(contrabandChat.ref(), TeamRole.SMUGGLER));
        context.actor().tell(new ClearContrabandGameChat());

        // when
        context.actor().tell(new RequestSendTeamChat(PLAYER_ID, "플레이어", "팀 메시지"));

        // then
        ActorTestUtils.expectNoMessages(contrabandChat.monitor(), Duration.ofMillis(300L));
    }

    @Test
    void 로비_채팅_초기화_후에는_로비_채팅을_입력할_수_없다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<LobbyChatCommand> lobbyChat = ActorTestUtils.spawnMonitored(
                actorTestKit,
                LobbyChatCommand.class,
                Behaviors.ignore()
        );
        context.actor().tell(new SyncLobbyChat(lobbyChat.ref()));
        context.actor().tell(new ClearLobbyChat());

        // when
        context.actor().tell(new RequestSendChat(PLAYER_ID, "플레이어", "무시"));

        // then
        ActorTestUtils.expectNoMessages(lobbyChat.monitor(), Duration.ofMillis(300L));
    }

    @Test
    void 로비_채팅_Actor_종료를_동기화_받는다() {
        // given
        TestContext context = createContext();
        ActorRef<LobbyChatCommand> lobbyChat = actorTestKit.spawn(Behaviors.ignore());
        context.actor().tell(new SyncLobbyChat(lobbyChat));

        // when
        actorTestKit.stop(lobbyChat);
        context.actor().tell(new RequestSendChat(PLAYER_ID, "플레이어", "무시"));

        // then
        assertThat(context.sender().exceptionCode).isNull();
    }

    @Test
    void 게임_채팅_Actor_종료를_동기화_받는다() {
        // given
        TestContext context = createContext();
        ActorRef<ContrabandGameChatCommand> contrabandChat = actorTestKit.spawn(Behaviors.ignore());
        context.actor().tell(new SyncContrabandGameChat(contrabandChat, TeamRole.SMUGGLER));

        // when
        actorTestKit.stop(contrabandChat);
        context.actor().tell(new RequestSendTeamChat(PLAYER_ID, "플레이어", "무시"));

        // then
        assertThat(context.sender().exceptionCode).isNull();
    }

    private ChatMessage chatMessage() {
        return new ChatMessage(1L, 1L, PLAYER_ID, "플레이어", "안녕", LocalDateTime.now());
    }

    private TestContext createContext() {
        SpyClientWebSocketMessageSender sender = new SpyClientWebSocketMessageSender();
        SpyChatBlacklistRepository chatBlacklistRepository = new SpyChatBlacklistRepository();
        Behavior<ChatCommand> behavior = SessionChatActor.create(PLAYER_ID, sender, chatBlacklistRepository);
        ActorRef<ChatCommand> actor = actorTestKit.spawn(behavior);
        return new TestContext(actor, sender, chatBlacklistRepository);
    }

    private record TestContext(
            ActorRef<ChatCommand> actor,
            SpyClientWebSocketMessageSender sender,
            SpyChatBlacklistRepository chatBlacklistRepository
    ) {
        public ActorRef<ChatCommand> actor() {
            return actor;
        }

        public SpyClientWebSocketMessageSender sender() {
            return sender;
        }

        public SpyChatBlacklistRepository chatBlacklistRepository() {
            return chatBlacklistRepository;
        }
    }
}
