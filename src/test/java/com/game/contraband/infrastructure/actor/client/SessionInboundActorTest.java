package com.game.contraband.infrastructure.actor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.InboundCommand;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.ClearLobby;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.ReSyncConnection;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestChangeMaxPlayerCount;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestDecideInspection;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestDecidePass;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestDecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestFixInspector;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestFixSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestKickPlayer;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestLeaveLobby;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestLobbyDeletion;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestRegisterInspector;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestRegisterSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestStartGame;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestToggleReady;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestToggleTeam;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestTransferMoney;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateContrabandGame;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ChangeMaxPlayerCount;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.KickPlayer;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LeaveLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ReSyncPlayer;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.RequestDeleteLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.StartGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ToggleReady;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.ToggleTeam;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideInspection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecidePass;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterInspector;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterSmuggler;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.TransferAmount;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SessionInboundActorTest {

    private static final Long PLAYER_ID = 1L;

    @Test
    void 클라이언트_세션_재_동기화시_매치_참여중이면_게임에_동기화_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new ReSyncConnection(PLAYER_ID));

        // then
        SyncReconnectedPlayer actual = (SyncReconnectedPlayer) contrabandGame.getAllReceived().get(0);

        assertThat(actual.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 클라이언트_세션_재_동기화시_로비_참여중이면_로비에_동기화_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new ReSyncConnection(PLAYER_ID));

        // then
        ReSyncPlayer actual = (ReSyncPlayer) lobby.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.playerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.clientSession()).isEqualTo(context.gateway().getRef())
        );
    }

    @Test
    void 참여하고_있는_게임_정보를_갱신한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> original = TestInbox.create();
        TestInbox<ContrabandGameCommand> updated = TestInbox.create();
        send(context, new UpdateContrabandGame(original.getRef()));
        send(context, new UpdateContrabandGame(updated.getRef()));

        // when
        send(context, new RequestDecidePass(PLAYER_ID));

        // then
        DecidePass actual = (DecidePass) updated.getAllReceived().get(0);
        assertThat(actual.inspectorId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 참여하고_있는_로비_정보를_갱신한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> original = TestInbox.create();
        TestInbox<LobbyCommand> updated = TestInbox.create();
        send(context, new UpdateLobby(original.getRef()));
        send(context, new UpdateLobby(updated.getRef()));

        // when
        send(context, new RequestToggleReady(PLAYER_ID));

        // then
        ToggleReady actual = (ToggleReady) updated.getAllReceived().get(0);
        assertThat(actual.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 로비_정보를_비운다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new ClearLobby());
        send(context, new RequestLeaveLobby(PLAYER_ID));

        // then
        assertThat(lobby.getAllReceived()).isEmpty();
    }

    @Test
    void 송금_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestTransferMoney(PLAYER_ID, 2L, 500));

        // then
        TransferAmount actual = (TransferAmount) contrabandGame.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.fromPlayerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.toPlayerId()).isEqualTo(2L),
                () -> assertThat(actual.amount()).isEqualTo(Money.from(500))
        );
    }

    @Test
    void 검사관의_검문_결정_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestDecideInspection(PLAYER_ID, 300));

        // then
        DecideInspection actual = (DecideInspection) contrabandGame.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.inspectorId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.amount()).isEqualTo(300)
        );
    }

    @Test
    void 검사관의_검문을_하지_않고_통과_결정_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestDecidePass(PLAYER_ID));

        // then
        DecidePass actual = (DecidePass) contrabandGame.getAllReceived().get(0);
        assertThat(actual.inspectorId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 밀수꾼의_밀수_금액_결정_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestDecideSmuggleAmount(PLAYER_ID, 700));

        // then
        DecideSmuggleAmount actual = (DecideSmuggleAmount) contrabandGame.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.smugglerId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.amount()).isEqualTo(700)
        );
    }

    @Test
    void 검사관_후보_등록_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestRegisterInspector(PLAYER_ID));

        // then
        RegisterInspector actual = (RegisterInspector) contrabandGame.getAllReceived().get(0);
        assertThat(actual.inspectorId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 밀수꾼_후보_등록_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestRegisterSmuggler(PLAYER_ID));

        // then
        RegisterSmuggler actual = (RegisterSmuggler) contrabandGame.getAllReceived().get(0);
        assertThat(actual.smugglerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 검사관_후보_선정_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestFixInspector(PLAYER_ID));

        // then
        FixInspectorId actual = (FixInspectorId) contrabandGame.getAllReceived().get(0);
        assertThat(actual.requesterId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 밀수꾼_후보_선정_요청을_전파한다() {
        // given
        TestContext context = createContext();
        TestInbox<ContrabandGameCommand> contrabandGame = TestInbox.create();
        send(context, new UpdateContrabandGame(contrabandGame.getRef()));

        // when
        send(context, new RequestFixSmuggler(PLAYER_ID));

        // then
        FixSmugglerId actual = (FixSmugglerId) contrabandGame.getAllReceived().get(0);
        assertThat(actual.requesterId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 로비_플레이어_강퇴를_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestKickPlayer(PLAYER_ID, 3L));

        // then
        KickPlayer actual = (KickPlayer) lobby.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.executorId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.targetPlayerId()).isEqualTo(3L)
        );
    }

    @Test
    void 로비_퇴장을_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestLeaveLobby(PLAYER_ID));

        // then
        LeaveLobby actual = (LeaveLobby) lobby.getAllReceived().get(0);
        assertThat(actual.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 게임_시작을_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestStartGame(PLAYER_ID, 5));

        // then
        StartGame actual = (StartGame) lobby.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.executorId()).isEqualTo(PLAYER_ID),
                () -> assertThat(actual.totalRounds()).isEqualTo(5)
        );
    }

    @Test
    void 로비_최대_인원_변경을_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestChangeMaxPlayerCount(6, PLAYER_ID));

        // then
        ChangeMaxPlayerCount actual = (ChangeMaxPlayerCount) lobby.getAllReceived().get(0);
        assertAll(
                () -> assertThat(actual.maxPlayerCount()).isEqualTo(6),
                () -> assertThat(actual.executorId()).isEqualTo(PLAYER_ID)
        );
    }

    @Test
    void 로비_팀_변경을_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestToggleTeam(PLAYER_ID));

        // then
        ToggleTeam actual = (ToggleTeam) lobby.getAllReceived().get(0);
        assertThat(actual.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 준비_토글을_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestToggleReady(PLAYER_ID));

        // then
        ToggleReady actual = (ToggleReady) lobby.getAllReceived().get(0);
        assertThat(actual.playerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void 로비_삭제를_요청한다() {
        // given
        TestContext context = createContext();
        TestInbox<LobbyCommand> lobby = TestInbox.create();
        send(context, new UpdateLobby(lobby.getRef()));

        // when
        send(context, new RequestLobbyDeletion(PLAYER_ID));

        // then
        RequestDeleteLobby actual = (RequestDeleteLobby) lobby.getAllReceived().get(0);
        assertThat(actual.executorId()).isEqualTo(PLAYER_ID);
    }

    private void send(TestContext context, InboundCommand command) {
        context.harness().kit().run(command);
    }

    private TestContext createContext() {
        TestInbox<ClientSessionCommand> gateway = TestInbox.create();
        BehaviorTestUtils.BehaviorTestHarness<InboundCommand> harness = BehaviorTestUtils.createHarness(
                SessionInboundActor.create(gateway.getRef())
        );
        return new TestContext(gateway, harness);
    }

    private record TestContext(
            TestInbox<ClientSessionCommand> gateway,
            BehaviorTestUtils.BehaviorTestHarness<InboundCommand> harness
    ) {
        public TestInbox<ClientSessionCommand> gateway() {
            return gateway;
        }

        public BehaviorTestUtils.BehaviorTestHarness<InboundCommand> harness() {
            return harness;
        }
    }
}
