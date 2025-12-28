package com.game.contraband.infrastructure.actor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClearActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.OutboundCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UpdateActiveGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateCreateLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateCreatedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedInspection;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedPass;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedSmuggleAmount;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorIdForSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerIdForInspector;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateHostDeletedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateInspectorApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateJoinedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateKicked;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateLeftLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateLobbyDeleted;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateOtherPlayerJoinedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateOtherPlayerKicked;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateOtherPlayerLeftLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSelectionTimer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSmugglerApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartNewRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateToggleReady;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateToggleTeam;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransfer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransferFailed;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RequestSessionReconnect;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.RoomDirectoryUpdated;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.SendWebSocketPing;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import com.game.contraband.infrastructure.actor.spy.SpyClientWebSocketMessageSender;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.List;
import java.util.Set;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SessionOutboundActorTest {

    private static final Long PLAYER_ID = 1L;

    @Test
    void 예외_메시지를_전송한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new HandleExceptionMessage(ExceptionCode.UNKNOWN_ERROR, "예외 메시지"));

        // then
        assertAll(
                () -> assertThat(context.sender().exceptionCode).isEqualTo(ExceptionCode.UNKNOWN_ERROR),
                () -> assertThat(context.sender().exceptionMessage).isEqualTo("예외 메시지")
        );
    }

    @Test
    void 웹소켓_ping을_전송한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new SendWebSocketPing());

        // then
        assertThat(context.sender().pingSent).isTrue();
    }

    @Test
    void 세션_재연결을_요청한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new RequestSessionReconnect());

        // then
        assertThat(context.sender().reconnectRequested).isTrue();
    }

    @Test
    void 게임_방_목록_업데이트를_전송한다() {
        // given
        TestContext context = createContext();
        List<RoomDirectorySnapshot> rooms = List.of(new RoomDirectorySnapshot(10L, "방", 4, 2, "entity", false));

        // when
        send(context, new RoomDirectoryUpdated(rooms, 5));

        // then
        assertAll(
                () -> assertThat(context.sender().roomSnapshots).isEqualTo(rooms),
                () -> assertThat(context.sender().roomTotalCount).isEqualTo(5)
        );
    }

    @Test
    void 게임_시작을_전파하고_게이트웨이를_업데이트한다() {
        // given
        TestContext context = createContext();
        List<GameStartPlayer> players = List.of(new GameStartPlayer(PLAYER_ID, "나", TeamRole.SMUGGLER, 0));

        // when
        send(context, new PropagateStartGame(null, 100L, "entity", players));

        // then
        List<ClientSessionCommand> gatewayMessages = context.gateway().getAllReceived();
        UpdateActiveGame update = (UpdateActiveGame) gatewayMessages.get(0);

        assertAll(
                () -> assertThat(context.sender().startGameSent).isTrue(),
                () -> assertThat(context.sender().startGamePlayers).isEqualTo(players),
                () -> assertThat(update.roomId()).isEqualTo(100L)
        );
    }

    @Test
    void 라운드_타이머를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateSelectionTimer(1, 10L, 20L, 30L, 40L));

        // then
        SpyClientWebSocketMessageSender.SelectionTimer actual = context.sender().selectionTimer;

        assertAll(
                () -> assertThat(actual.round).isEqualTo(1),
                () -> assertThat(actual.eventAtMillis).isEqualTo(10L),
                () -> assertThat(actual.durationMillis).isEqualTo(20L),
                () -> assertThat(actual.serverNowMillis).isEqualTo(30L),
                () -> assertThat(actual.endAtMillis).isEqualTo(40L)
        );
    }

    @Test
    void 밀수꾼_후보_등록을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);

        // when
        send(context, new PropagateRegisterSmugglerId(5L));

        // then
        assertThat(context.sender().registeredSmugglerId).isEqualTo(5L);
    }

    @Test
    void 밀수꾼_후보_선정을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);

        // when
        send(context, new PropagateFixedSmugglerId(6L));

        // then
        assertThat(context.sender().fixedSmugglerId).isEqualTo(6L);
    }

    @Test
    void 검사관에게_밀수꾼_후보_선정을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);

        // when
        send(context, new PropagateFixedSmugglerIdForInspector());

        // then
        assertThat(context.sender().fixedSmugglerIdForInspectorSent).isTrue();
    }

    @Test
    void 검사관_후보_등록을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);

        // when
        send(context, new PropagateRegisterInspectorId(7L));

        // then
        assertThat(context.sender().registeredInspectorId).isEqualTo(7L);
    }

    @Test
    void 검사관_후보_선정을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);

        // when
        send(context, new PropagateFixedInspectorId(8L));

        // then
        assertThat(context.sender().fixedInspectorId).isEqualTo(8L);
    }

    @Test
    void 밀수꾼에게_검사관_후보_선정을_알린다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);

        // when
        send(context, new PropagateFixedInspectorIdForSmuggler());

        // then
        assertThat(context.sender().fixedInspectorIdForSmugglerSent).isTrue();
    }

    @Test
    void 밀수꾼_찬성_상태를_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);
        Set<Long> approvals = Set.of(2L, 3L);

        // when
        send(context, new PropagateSmugglerApprovalState(10L, approvals, true));

        // then
        SpyClientWebSocketMessageSender.ApprovalState actual = context.sender().smugglerApprovalState;

        assertAll(
                () -> assertThat(actual.candidateId).isEqualTo(10L),
                () -> assertThat(actual.approverIds).isEqualTo(approvals),
                () -> assertThat(actual.fixed).isTrue()
        );
    }

    @Test
    void 검사관_찬성_상태를_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);
        Set<Long> approvals = Set.of(4L);

        // when
        send(context, new PropagateInspectorApprovalState(11L, approvals, false));

        // then
        SpyClientWebSocketMessageSender.ApprovalState actual = context.sender().inspectorApprovalState;

        assertAll(
                () -> assertThat(actual.candidateId).isEqualTo(11L),
                () -> assertThat(actual.approverIds).isEqualTo(approvals),
                () -> assertThat(actual.fixed).isFalse()
        );
    }

    @Test
    void 새_라운드_시작을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateStartNewRound(2, 10L, 20L, 30L, 40L, 50L, 60L));

        // then
        SpyClientWebSocketMessageSender.StartRound actual = context.sender().startRound;

        assertAll(
                () -> assertThat(actual.currentRound).isEqualTo(2),
                () -> assertThat(actual.smugglerId).isEqualTo(10L),
                () -> assertThat(actual.inspectorId).isEqualTo(20L)
        );
    }

    @Test
    void 라운드_종료를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateFinishedRound(1L, 100, 2L, 200, RoundOutcomeType.PASS));

        // then
        SpyClientWebSocketMessageSender.FinishedRound actual = context.sender().finishedRound;

        assertAll(
                () -> assertThat(actual.smugglerId).isEqualTo(1L),
                () -> assertThat(actual.smugglerAmount).isEqualTo(100),
                () -> assertThat(actual.outcomeType).isEqualTo(RoundOutcomeType.PASS)
        );
    }

    @Test
    void 게임_종료를_전파하고_게이트웨이를_초기화한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateFinishedGame(GameWinnerType.SMUGGLER_TEAM, 300, 200));

        // then
        SpyClientWebSocketMessageSender.FinishedGame actual = context.sender().finishedGame;
        ClearActiveGame clear = (ClearActiveGame) context.gateway().getAllReceived().get(0);

        assertAll(
                () -> assertThat(actual.winnerType).isEqualTo(GameWinnerType.SMUGGLER_TEAM),
                () -> assertThat(clear).isNotNull()
        );
    }

    @Test
    void 송금_실패를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateTransferFailed(TransferFailureReason.INSUFFICIENT_BALANCE, "송금에 실패했습니다."));

        // then
        SpyClientWebSocketMessageSender.TransferFailure actual = context.sender().transferFailure;

        assertAll(
                () -> assertThat(actual.reason).isEqualTo(TransferFailureReason.INSUFFICIENT_BALANCE),
                () -> assertThat(actual.message).isEqualTo("송금에 실패했습니다.")
        );
    }

    @Test
    void 검사관이_검문을_하지_않고_통과했음을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);

        // when
        send(context, new PropagateDecidedPass(2L));

        // then
        assertThat(context.sender().decidedPass.inspectorId).isEqualTo(2L);
    }

    @Test
    void 밀수꾼에게_검사관_통과_행동_결정을_알린다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);

        // when
        send(context, new PropagateDecidedPass(2L));

        // then
        assertThat(context.sender().decidedPass.smugglerView).isTrue();
    }

    @Test
    void 검사관_검문_금액_결정을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);

        // when
        send(context, new PropagateDecidedInspection(2L, 50));

        // then
        assertAll(
                () -> assertThat(context.sender().decidedInspection.inspectorId).isEqualTo(2L),
                () -> assertThat(context.sender().decidedInspection.amount).isEqualTo(50)
        );
    }

    @Test
    void 밀수꾼에게_검사관_검문_행동_결정을_알린다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);

        // when
        send(context, new PropagateDecidedInspection(2L, 50));

        // then
        assertThat(context.sender().decidedPass.smugglerView).isTrue();
    }

    @Test
    void 밀수꾼의_밀수_금액_결정을_전파한다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.INSPECTOR);

        // when
        send(context, new PropagateDecidedSmuggleAmount(PLAYER_ID, 70));

        // then
        assertThat(context.sender().decidedSmuggle.smugglerView).isFalse();
    }

    @Test
    void 밀수꾼_팀에_밀수_금액_결정을_알린다() {
        // given
        TestContext context = createContextWithTeam(TeamRole.SMUGGLER);

        // when
        send(context, new PropagateDecidedSmuggleAmount(PLAYER_ID, 70));

        // then
        assertAll(
                () -> assertThat(context.sender().decidedSmuggle.smugglerId).isEqualTo(PLAYER_ID),
                () -> assertThat(context.sender().decidedSmuggle.amount).isEqualTo(70),
                () -> assertThat(context.sender().decidedSmuggle.smugglerView).isTrue()
        );
    }

    @Test
    void 송금을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateTransfer(1L, 2L, 100, 200, 30));

        // then
        SpyClientWebSocketMessageSender.Transfer actual = context.sender().transfer;
        assertAll(
                () -> assertThat(actual.senderId).isEqualTo(1L),
                () -> assertThat(actual.targetId).isEqualTo(2L),
                () -> assertThat(actual.amount).isEqualTo(30)
        );
    }

    @Test
    void 로비_생성을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateCreateLobby(null, 4, "방", TeamRole.SMUGGLER));

        // then
        SpyClientWebSocketMessageSender.CreateLobby actual = context.sender().createLobby;
        assertAll(
                () -> assertThat(actual.maxPlayerCount).isEqualTo(4),
                () -> assertThat(actual.lobbyName).isEqualTo("방"),
                () -> assertThat(actual.teamRole).isEqualTo(TeamRole.SMUGGLER)
        );
    }

    @Test
    void 로비_생성_완료를_전파한다() {
        // given
        TestContext context = createContext();
        List<LobbyParticipant> participants = List.of(new LobbyParticipant(1L, "호스트", TeamRole.SMUGGLER, false));

        // when
        send(context, new PropagateCreatedLobby(null, 10L, 1L, 4, 1, "방", participants));

        // then
        SpyClientWebSocketMessageSender.CreatedLobby actual = context.sender().createdLobby;
        assertAll(
                () -> assertThat(actual.roomId).isEqualTo(10L),
                () -> assertThat(actual.currentPlayerCount).isEqualTo(1)
        );
    }

    @Test
    void 다른_플레이어의_로비_입장을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateOtherPlayerJoinedLobby(2L, "참가", TeamRole.INSPECTOR, 2));

        // then
        SpyClientWebSocketMessageSender.OtherJoined actual = context.sender().otherJoined;
        assertAll(
                () -> assertThat(actual.joinerId).isEqualTo(2L),
                () -> assertThat(actual.teamRole).isEqualTo(TeamRole.INSPECTOR)
        );
    }

    @Test
    void 자기_자신의로비_참여를_전파한다() {
        // given
        TestContext context = createContext();
        List<LobbyParticipant> participants = List.of(new LobbyParticipant(1L, "호스트", TeamRole.SMUGGLER, false));

        // when
        send(context, new PropagateJoinedLobby(null, 10L, 1L, 4, 1, "방", participants));

        // then
        SpyClientWebSocketMessageSender.JoinedLobby actual = context.sender().joinedLobby;
        assertThat(actual.lobbyName).isEqualTo("방");
    }

    @Test
    void 준비_토글을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateToggleReady(2L, true));

        // then
        assertAll(
                () -> assertThat(context.sender().toggleReady.playerId).isEqualTo(2L),
                () -> assertThat(context.sender().toggleReady.toggleReadyState).isTrue()
        );
    }

    @Test
    void 팀_변경을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateToggleTeam(2L, "참가", TeamRole.INSPECTOR));

        // then
        assertAll(
                () -> assertThat(context.sender().toggleTeam.playerId).isEqualTo(2L),
                () -> assertThat(context.sender().toggleTeam.teamRole).isEqualTo(TeamRole.INSPECTOR)
        );
    }

    @Test
    void 자기_자신의로비_퇴장을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateLeftLobby());

        // then
        assertThat(context.sender().leftLobby).isTrue();
    }

    @Test
    void 다른_플레이어의_로비_퇴장을_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateOtherPlayerLeftLobby(3L));

        // then
        assertThat(context.sender().otherLeftLobby).isEqualTo(3L);
    }

    @Test
    void 자기_자신의_강퇴를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateKicked());

        // then
        assertThat(context.sender().kickedLobby).isTrue();
    }

    @Test
    void 다른_플레이어_강퇴를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateOtherPlayerKicked(4L));

        // then
        assertThat(context.sender().otherKicked).isEqualTo(4L);
    }

    @Test
    void 호스트에게_로비_삭제를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateHostDeletedLobby());

        // then
        assertThat(context.sender().hostDeletedLobby).isTrue();
    }

    @Test
    void 호스트가_아닌_로비에_참여한_플레이어에게_로비_삭제를_전파한다() {
        // given
        TestContext context = createContext();

        // when
        send(context, new PropagateLobbyDeleted());

        // then
        assertThat(context.sender().lobbyDeleted).isTrue();
    }

    private void send(TestContext context, OutboundCommand command) {
        context.harness().kit().run(command);
    }

    private TestContext createContext() {
        SpyClientWebSocketMessageSender sender = new SpyClientWebSocketMessageSender();
        TestInbox<ClientSessionCommand> gateway = TestInbox.create();
        BehaviorTestUtils.BehaviorTestHarness<OutboundCommand> harness = BehaviorTestUtils.createHarness(
                SessionOutboundActor.create(PLAYER_ID, sender, gateway.getRef())
        );
        return new TestContext(sender, gateway, harness);
    }

    private TestContext createContextWithTeam(TeamRole teamRole) {
        TestContext context = createContext();
        List<GameStartPlayer> players = List.of(new GameStartPlayer(PLAYER_ID, "나", teamRole, 0));
        send(context, new PropagateStartGame(null, 10L, "entity", players));
        context.gateway().getAllReceived();
        return context;
    }

    private record TestContext(
            SpyClientWebSocketMessageSender sender,
            TestInbox<ClientSessionCommand> gateway,
            BehaviorTestUtils.BehaviorTestHarness<OutboundCommand> harness
    ) {
        public SpyClientWebSocketMessageSender sender() {
            return sender;
        }

        public TestInbox<ClientSessionCommand> gateway() {
            return gateway;
        }

        public BehaviorTestUtils.BehaviorTestHarness<OutboundCommand> harness() {
            return harness;
        }
    }
}
