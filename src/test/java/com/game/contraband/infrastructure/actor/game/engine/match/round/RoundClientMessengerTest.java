package com.game.contraband.infrastructure.actor.game.engine.match.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartNewRound;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.spy.SpyGameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.EndGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
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
class RoundClientMessengerTest {

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
    void 라운드_시작을_모든_클라이언트_세션에_전파한다() {
        // given
        TestContext context = createContext();
        RoundFlowState state = new RoundFlowState();

        state.assignRound(1L, 2L, 1);

        Instant startedAt = Instant.EPOCH;

        // when
        RoundClientMessenger roundClientMessenger = context.messenger();

        roundClientMessenger.broadcastStartRound(
                state,
                startedAt,
                30_000L,
                startedAt.toEpochMilli(),
                startedAt.plusSeconds(30)
                         .toEpochMilli()
        );

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSession(), PropagateStartNewRound.class),
                () -> ActorTestUtils.expectMessages(context.inspectorSession(), PropagateStartNewRound.class)
        );
    }

    @Test
    void 라운드_정산_결과를_모든_클라이언트_세션에_전파한다() {
        // given
        TestContext context = createContext();
        PropagateFinishedRound finishedRound = new PropagateFinishedRound(
                1L,
                1000,
                2L,
                1200, RoundOutcomeType.INSPECTION_UNDER
        );

        // when
        RoundClientMessenger roundClientMessenger = context.messenger();

        roundClientMessenger.broadcastFinishedRound(finishedRound);

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSession(), PropagateFinishedRound.class),
                () -> ActorTestUtils.expectMessages(context.inspectorSession(), PropagateFinishedRound.class)
        );
    }

    @Test
    void 게임_종료_결과를_모든_클라이언트_세션에_전파한다() {
        // given
        TestContext context = createContext();
        PropagateFinishedGame finishedGame = new PropagateFinishedGame(
                GameWinnerType.SMUGGLER_TEAM,
                3000,
                2500
        );

        // when
        RoundClientMessenger roundClientMessenger = context.messenger();

        roundClientMessenger.broadcastFinishedGame(finishedGame);

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSession(), PropagateFinishedGame.class),
                () -> ActorTestUtils.expectMessages(context.inspectorSession(), PropagateFinishedGame.class)
        );
    }

    @Test
    void 게임_종료를_게임_라이프사이클에_알린다() {
        // given
        SpyGameLifecycleEventPublisher publisher = new SpyGameLifecycleEventPublisher();
        TestContext context = createContext(publisher);

        // when
        RoundClientMessenger roundClientMessenger = context.messenger();

        roundClientMessenger.publishGameEnded();

        // then
        assertAll(
                () -> assertThat(publisher.getRoomId()).isEqualTo(context.roomId()),
                () -> assertThat(publisher.getEntityId()).isEqualTo(context.entityId())
        );
    }

    @Test
    void 게임_종료를_로비에_알린다() {
        // given
        TestContext context = createContext();

        // when
        RoundClientMessenger roundClientMessenger = context.messenger();

        roundClientMessenger.notifyParentEndGame();

        // then
        ActorTestUtils.expectMessages(context.lobby(), EndGame.class);
    }

    @Test
    void 게임_시작_메시지를_지정한_클라이언트_세션에_전송한다() {
        // given
        TestContext context = createContext();
        List<GameStartPlayer> players = List.of(
                new GameStartPlayer(1L, "플레이어 1", TeamRole.SMUGGLER, 3000)
        );

        // when
        RoundClientMessenger roundClientMessenger = context.messenger();

        ActorTestUtils.expectMessages(context.smugglerSession(), PropagateStartGame.class);

        roundClientMessenger.sendStartGameToSession(context.smugglerSession().getRef(), players);

        // then
        ActorTestUtils.expectMessages(context.smugglerSession(), PropagateStartGame.class);
    }

    @Test
    void 전체_세션에서_플레이어_세션을_조회한다() {
        // given
        TestContext context = createContext();

        // when
        ActorRef<ClientSessionCommand> smugglerSession = context.messenger().totalSession(1L);
        ActorRef<ClientSessionCommand> inspectorSession = context.messenger().totalSession(2L);

        // then
        assertAll(
                () -> assertThat(smugglerSession).isEqualTo(context.smugglerSession().getRef()),
                () -> assertThat(inspectorSession).isEqualTo(context.inspectorSession().getRef())
        );
    }

    @Test
    void 팀별_세션에서_플레이어_세션을_조회한다() {
        // given
        TestContext context = createContext();

        // when
        ActorRef<ClientSessionCommand> smugglerSession = context.messenger().teamSession(TeamRole.SMUGGLER, 1L);
        ActorRef<ClientSessionCommand> inspectorSession = context.messenger().teamSession(TeamRole.INSPECTOR, 2L);

        // then
        assertAll(
                () -> assertThat(smugglerSession).isEqualTo(context.smugglerSession().getRef()),
                () -> assertThat(inspectorSession).isEqualTo(context.inspectorSession().getRef())
        );
    }

    private TestContext createContext() {
        return createContext(new SpyGameLifecycleEventPublisher());
    }

    private TestContext createContext(GameLifecycleEventPublisher publisher) {
        Long roomId = 1L;
        String entityId = "entity-1";

        TestProbe<ClientSessionCommand> smugglerSession = actorTestKit.createTestProbe();
        TestProbe<ClientSessionCommand> inspectorSession = actorTestKit.createTestProbe();
        TestProbe<LobbyCommand> lobby = actorTestKit.createTestProbe();
        TestProbe<ContrabandGameCommand> facade = actorTestKit.createTestProbe();

        Map<Long, ActorRef<ClientSessionCommand>> totalSessions = new HashMap<>();
        totalSessions.put(1L, smugglerSession.getRef());
        totalSessions.put(2L, inspectorSession.getRef());

        ClientSessionRegistry registry = createRegistry(totalSessions, roomId, entityId);

        RoundClientMessenger messenger = new RoundClientMessenger(
                registry,
                lobby.getRef(),
                facade.getRef(),
                publisher,
                roomId,
                entityId
        );

        return new TestContext(
                messenger,
                smugglerSession,
                inspectorSession,
                lobby,
                facade,
                roomId,
                entityId
        );
    }

    private ClientSessionRegistry createRegistry(
            Map<Long, ActorRef<ClientSessionCommand>> totalSessions,
            Long roomId,
            String entityId
    ) {
        TestProbe<ClientSessionRegistry> registryProbe = actorTestKit.createTestProbe();

        Behavior<ContrabandGameCommand> behavior = Behaviors.setup(
                context -> {
                    ClientSessionRegistry registry = ClientSessionRegistry.create(
                            contrabandGame(),
                            totalSessions,
                            context,
                            roomId,
                            entityId
                    );
                    registryProbe.getRef()
                                 .tell(registry);
                    return Behaviors.empty();
                }
        );

        ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameCommand.class, behavior);

        return registryProbe.receiveMessage();
    }

    private ContrabandGame contrabandGame() {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사팀", TeamRole.INSPECTOR, List.of(inspector));

        return ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);
    }

    private record TestContext(
            RoundClientMessenger messenger,
            TestProbe<ClientSessionCommand> smugglerSession,
            TestProbe<ClientSessionCommand> inspectorSession,
            TestProbe<LobbyCommand> lobby,
            TestProbe<ContrabandGameCommand> facade,
            Long roomId,
            String entityId
    ) { }
}
