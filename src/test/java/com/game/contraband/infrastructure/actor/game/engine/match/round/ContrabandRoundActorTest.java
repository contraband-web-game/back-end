package com.game.contraband.infrastructure.actor.game.engine.match.round;

import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedInspection;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedPass;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateDecidedSmuggleAmount;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartNewRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateTransfer;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ClearRoundChatId;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.EndGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideInspection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecidePass;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.DecideSmuggleAmount;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FinishCurrentRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FinishedGame;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.GameCleanup;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.PrepareNextSelection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundTimeout;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartSelectedRound;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.TransferAmount;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.ManualTime;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
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
class ContrabandRoundActorTest {

    private ActorTestKit actorTestKit;

    @BeforeEach
    void setUp() {
        actorTestKit = ActorTestKit.create(ManualTime.config());
    }

    @AfterEach
    void tearDown() {
        actorTestKit.shutdownTestKit();
    }

    @Test
    void 라운드를_시작하면_모든_플레이어에게_알린다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId);

        // when
        context.commandRef().tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class)
        );
    }

    @Test
    void 밀수꾼이_밀수_금액을_결정하면_모든_플레이어에게_전파한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        // when
        contrabandGameActor.tell(new DecideSmuggleAmount(smugglerId, 500));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId),
                        PropagateDecidedSmuggleAmount.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId),
                        PropagateDecidedSmuggleAmount.class
                )
        );
    }

    @Test
    void 라운드가_시작되고_밀수꾼과_검사관이_모두_행동을_결정했다면_다음_라운드를_진행하기_위한_메시지를_전달한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        contrabandGameActor.tell(new DecideSmuggleAmount(smugglerId, 500));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateDecidedSmuggleAmount.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateDecidedSmuggleAmount.class);

        // when
        contrabandGameActor.tell(new DecidePass(inspectorId));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(smugglerId), PropagateDecidedPass.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId), PropagateDecidedPass.class),
                () -> ActorTestUtils.expectMessages(context.facade(), FinishCurrentRound.class)
        );
    }

    @Test
    void 검사관이_통과를_선택하면_결정을_전파한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        contrabandGameActor.tell(new DecideSmuggleAmount(smugglerId, 500));
        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateDecidedSmuggleAmount.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateDecidedSmuggleAmount.class);

        // when
        contrabandGameActor.tell(new DecidePass(inspectorId));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(smugglerId), PropagateDecidedPass.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId), PropagateDecidedPass.class)
        );
    }

    @Test
    void 팀_내_송금을_성공하면_송금에_참여한_모든_플레이어에게_송금_결과를_전파한다() {
        // given
        Long smugglerId1 = 10L;
        Long smugglerId2 = 11L;
        Long inspectorId1 = 20L;
        Long inspectorId2 = 21L;
        RoundTestContext context = createRoundContext(
                List.of(smugglerId1, smugglerId2),
                List.of(inspectorId1, inspectorId2),
                5
        );

        Money transferAmount = Money.from(500);

        // when
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new TransferAmount(smugglerId1, smugglerId2, transferAmount));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(smugglerId1), PropagateTransfer.class),
                () -> ActorTestUtils.expectMessages(context.session(smugglerId2), PropagateTransfer.class),
                () -> ActorTestUtils.expectNoMessages(context.session(inspectorId1), Duration.ofMillis(300L)),
                () -> ActorTestUtils.expectNoMessages(context.session(inspectorId2), Duration.ofMillis(300L))
        );
    }

    @Test
    void 검사관이_검문을_선택하면_결정을_전파한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        // when
        contrabandGameActor.tell(new DecideInspection(inspectorId, 600));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId),
                        PropagateDecidedInspection.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId),
                        PropagateDecidedInspection.class
                )
        );
    }

    @Test
    void 라운드를_완료하면_정산_결과와_다음_라운드_준비를_알린다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId, 5);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        contrabandGameActor.tell(new DecideSmuggleAmount(smugglerId, 500));
        contrabandGameActor.tell(new DecidePass(inspectorId));

        ActorTestUtils.expectMessages(context.facade(), FinishCurrentRound.class);

        // when
        contrabandGameActor.tell(new FinishCurrentRound());

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(smugglerId), PropagateFinishedRound.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId), PropagateFinishedRound.class),
                () -> ActorTestUtils.expectMessages(context.facade(), PrepareNextSelection.class),
                () -> ActorTestUtils.expectMessages(context.gameChat(), ClearRoundChatId.class)
        );
    }

    @Test
    void 게임이_종료되면_완료_메시지를_전파하고_정리한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId, 1);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        contrabandGameActor.tell(new DecideSmuggleAmount(smugglerId, 500));
        contrabandGameActor.tell(new DecidePass(inspectorId));

        ActorTestUtils.expectMessages(context.facade(), FinishCurrentRound.class);

        contrabandGameActor.tell(new FinishCurrentRound());

        ActorTestUtils.expectMessages(context.facade(), FinishedGame.class);

        // when
        contrabandGameActor.tell(new FinishedGame());

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(smugglerId), PropagateFinishedGame.class, ClearContrabandGameChat.class),
                () -> ActorTestUtils.expectMessages(context.session(inspectorId), PropagateFinishedGame.class, ClearContrabandGameChat.class),
                () -> ActorTestUtils.expectMessages(context.lobby(), EndGame.class),
                () -> ActorTestUtils.expectMessages(context.facade(), GameCleanup.class)
        );
    }

    @Test
    void 라운드_행동_시간이_만료되면_밀수꾼은_0원_밀수와_검사관은_통과를_강제로_발생시키고_라운드를_종료한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(smugglerId, inspectorId);
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        // when
        ManualTime manualTime = ManualTime.get(actorTestKit.system());

        manualTime.timePasses(Duration.ofSeconds(33L));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.facade(),
                        DecideSmuggleAmount.class,
                        DecidePass.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(smugglerId),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.session(inspectorId),
                        PropagateDecidedSmuggleAmount.class,
                        PropagateDecidedPass.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.facade(),
                        RoundTimeout.class,
                        FinishCurrentRound.class
                )
        );
    }

    @Test
    void 재접속한_플레이어에게_현재_라운드_상태를_동기화한다() {
        // given
        Long smugglerId = 10L;
        Long inspectorId = 20L;
        RoundTestContext context = createRoundContext(
                List.of(smugglerId),
                List.of(inspectorId),
                5,
                false
        );
        ActorRef<ContrabandGameCommand> contrabandGameActor = context.commandRef();

        contrabandGameActor.tell(new StartSelectedRound(smugglerId, inspectorId, 1));

        ActorTestUtils.expectMessages(context.session(smugglerId), PropagateStartNewRound.class);
        ActorTestUtils.expectMessages(context.session(inspectorId), PropagateStartNewRound.class);

        contrabandGameActor.tell(new DecideSmuggleAmount(smugglerId, 500));

        ActorTestUtils.waitUntilMessages(
                context.session(smugglerId),
                PropagateStartGame.class,
                PropagateDecidedSmuggleAmount.class
        );

        contrabandGameActor.tell(new SyncReconnectedPlayer(smugglerId));

        // then
        ActorTestUtils.expectMessages(
                context.session(smugglerId),
                PropagateStartGame.class,
                PropagateDecidedSmuggleAmount.class
        );
    }

    private RoundTestContext createRoundContext(Long smugglerId, Long inspectorId) {
        return createRoundContext(
                List.of(smugglerId),
                List.of(inspectorId),
                5,
                true
        );
    }

    private RoundTestContext createRoundContext(Long smugglerId, Long inspectorId, int totalRounds) {
        return createRoundContext(
                List.of(smugglerId),
                List.of(inspectorId),
                totalRounds,
                true
        );
    }

    private RoundTestContext createRoundContext(
            List<Long> smugglerIds,
            List<Long> inspectorIds,
            int totalRounds
    ) {
        return createRoundContext(smugglerIds, inspectorIds, totalRounds, true);
    }

    private RoundTestContext createRoundContext(
            List<Long> smugglerIds,
            List<Long> inspectorIds,
            int totalRounds,
            boolean forwardRoundTimeout
    ) {
        Long roomId = 1L;
        String entityId = "entity-1";

        ContrabandGame contrabandGame = createGame(smugglerIds, inspectorIds, totalRounds);
        Map<Long, TestProbe<ClientSessionCommand>> sessions = createSessionProbes(smugglerIds, inspectorIds);
        Map<Long, ActorRef<ClientSessionCommand>> sessionRefs = toSessionRefs(sessions);

        RegistryContext registryContext = createRegistry(contrabandGame, sessionRefs, roomId, entityId);

        TestProbe<LobbyCommand> lobby = actorTestKit.createTestProbe();
        TestProbe<ContrabandGameCommand> facadeMonitor = actorTestKit.createTestProbe();
        TestProbe<ContrabandGameChatCommand> gameChat = actorTestKit.createTestProbe(ContrabandGameChatCommand.class);

        AtomicReference<ActorRef<ContrabandGameCommand>> roundRef = new AtomicReference<>();
        ActorRef<ContrabandGameCommand> forwardingFacade = actorTestKit.spawn(
                Behaviors.monitor(
                        ContrabandGameCommand.class,
                        facadeMonitor.getRef(),
                        Behaviors.receiveMessage(msg -> {
                            ActorRef<ContrabandGameCommand> target = roundRef.get();
                            if (!forwardRoundTimeout && msg instanceof RoundTimeout) {
                                return Behaviors.same();
                            }
                            if (target != null) {
                                target.tell(msg);
                            }
                            return Behaviors.same();
                        })
                )
        );

        RoundClientMessenger clientMessenger = new RoundClientMessenger(
                registryContext.registry(),
                lobby.getRef(),
                forwardingFacade,
                null,
                roomId,
                entityId
        );
        RoundChatCoordinator chatCoordinator = new RoundChatCoordinator(gameChat.getRef(), registryContext.registry());

        Behavior<ContrabandGameCommand> behavior = ContrabandRoundActor.create(
                roomId,
                entityId,
                new RoundGameContext(contrabandGame),
                clientMessenger,
                chatCoordinator
        );

        ActorTestUtils.MonitoredActor<ContrabandGameCommand> roundActor = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ContrabandGameCommand.class,
                behavior
        );
        roundRef.set(roundActor.ref());

        return new RoundTestContext(roundActor, sessions, registryContext.registry(), facadeMonitor, lobby, gameChat);
    }

    private RegistryContext createRegistry(
            ContrabandGame contrabandGame,
            Map<Long, ActorRef<ClientSessionCommand>> clientSessions,
            Long roomId,
            String entityId
    ) {
        TestInbox<ClientSessionRegistry> registryInbox = TestInbox.create();

        Behavior<ContrabandGameCommand> behavior = Behaviors.setup(
                context -> {
                    ClientSessionRegistry registry = ClientSessionRegistry.create(
                            contrabandGame,
                            clientSessions,
                            context,
                            roomId,
                            entityId
                    );
                    registryInbox.getRef().tell(registry);

                    return Behaviors.empty();
                }
        );

        BehaviorTestUtils.BehaviorTestHarness<ContrabandGameCommand> harness = BehaviorTestUtils.createHarness(behavior);

        ClientSessionRegistry registry = registryInbox.receiveMessage();
        ActorRef<ContrabandGameCommand> contrabandGameRef = harness.kit().getRef();

        return new RegistryContext(registry, contrabandGameRef);
    }

    private ContrabandGame createGame(
            List<Long> smugglerIds,
            List<Long> inspectorIds,
            int totalRounds
    ) {
        List<PlayerProfile> smugglers = smugglerIds.stream()
                                                   .map(id -> PlayerProfile.create(id, "밀수꾼", TeamRole.SMUGGLER))
                                                   .toList();
        List<PlayerProfile> inspectors = inspectorIds.stream()
                                                     .map(id -> PlayerProfile.create(id, "검사관", TeamRole.INSPECTOR))
                                                     .toList();

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, smugglers);
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, inspectors);

        return ContrabandGame.notStarted(smugglerRoster, inspectorRoster, totalRounds);
    }

    private Map<Long, TestProbe<ClientSessionCommand>> createSessionProbes(
            List<Long> smugglerIds,
            List<Long> inspectorIds
    ) {
        Map<Long, TestProbe<ClientSessionCommand>> probes = new LinkedHashMap<>();
        smugglerIds.forEach(id -> probes.put(id, actorTestKit.createTestProbe(ClientSessionCommand.class)));
        inspectorIds.forEach(id -> probes.put(id, actorTestKit.createTestProbe(ClientSessionCommand.class)));
        return probes;
    }

    private Map<Long, ActorRef<ClientSessionCommand>> toSessionRefs(
            Map<Long, TestProbe<ClientSessionCommand>> sessions
    ) {
        Map<Long, ActorRef<ClientSessionCommand>> refs = new LinkedHashMap<>();
        for (Map.Entry<Long, TestProbe<ClientSessionCommand>> entry : sessions.entrySet()) {
            refs.put(entry.getKey(), entry.getValue().getRef());
        }
        return refs;
    }

    private record RegistryContext(
            ClientSessionRegistry registry,
            ActorRef<ContrabandGameCommand> contrabandGameRef
    ) { }

    private record RoundTestContext(
            ActorTestUtils.MonitoredActor<ContrabandGameCommand> monitoredRound,
            Map<Long, TestProbe<ClientSessionCommand>> sessions,
            ClientSessionRegistry registry,
            TestProbe<ContrabandGameCommand> facade,
            TestProbe<LobbyCommand> lobby,
            TestProbe<ContrabandGameChatCommand> gameChat
    ) {
        ActorRef<ContrabandGameCommand> commandRef() {
            return monitoredRound.ref();
        }

        TestProbe<ClientSessionCommand> session(Long playerId) {
            return sessions.get(playerId);
        }
    }
}
