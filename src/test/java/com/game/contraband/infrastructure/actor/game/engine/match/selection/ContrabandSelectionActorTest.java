package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedInspectorIdForSmuggler;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFixedSmugglerIdForInspector;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterInspectorId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSelectionTimer;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSmugglerApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateInspectorApprovalState;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateContrabandGame;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.SyncRoundChatId;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterInspectorId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RegisterSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.FixSmugglerId;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.PrepareNextSelection;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundReady;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.RoundSelectionTimeout;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.SyncReconnectedPlayer;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.StartNewRound;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
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
class ContrabandSelectionActorTest {

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
    void 각_팀에_단_한_명만_있다면_후보_선정_단계를_거치지_않고_바로_라운드를_진행한다() {
        // given
        ContrabandSelectionTestContext context = createContext(twoPlayerParticipants());
        ActorTestUtils.waitUntilMessages(
                context.smugglerSessionProbe(),
                PropagateStartGame.class,
                UpdateContrabandGame.class
        );
        ActorTestUtils.waitUntilMessages(
                context.inspectorSessionProbe(),
                PropagateStartGame.class,
                UpdateContrabandGame.class
        );

        // when
        context.selectionActor();

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.smugglerSessionProbe(),
                        PropagateRegisterSmugglerId.class,
                        PropagateFixedSmugglerId.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.inspectorSessionProbe(),
                        PropagateRegisterInspectorId.class,
                        PropagateFixedInspectorId.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.gameChatProbe(),
                        SyncRoundChatId.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.facadeProbe(),
                        RoundReady.class,
                        StartNewRound.class
                )
        );
    }

    @Test
    void 밀수꾼_후보를_등록하면_밀수꾼_팀에_후보가_등록되었음을_알린다() {
        // given
        ContrabandSelectionTestContext context = createContext(multiPlayerParticipants());
        drainInitialSelectionTimer(context);

        // when
        context.selectionActor().ref().tell(new RegisterSmugglerId(1L, 1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.smugglerSessionProbe(),
                        PropagateRegisterSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectNoMessages(context.inspectorSessionProbe(), Duration.ofMillis(300L))
        );
    }

    @Test
    void 검사관_후보를_확정하면_양_팀에_확정_사실을_알린다() {
        // given
        ContrabandSelectionTestContext context = contextWithRegisteredInspector();

        // when
        context.selectionActor().ref().tell(new FixInspectorId(2L));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.inspectorSessionProbe(),
                        PropagateFixedInspectorId.class,
                        PropagateInspectorApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.smugglerSessionProbe(),
                        PropagateFixedInspectorIdForSmuggler.class
                )
        );
    }

    @Test
    void 밀수꾼_후보를_확정하면_양_팀에_확정_사실을_알린다() {
        // given
        ContrabandSelectionTestContext context = contextWithRegisteredSmuggler();

        // when
        context.selectionActor().ref().tell(new FixSmugglerId(1L));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.smugglerSessionProbe(),
                        PropagateFixedSmugglerId.class,
                        PropagateSmugglerApprovalState.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.inspectorSessionProbe(),
                        PropagateFixedSmugglerIdForInspector.class
                )
        );
    }

    @Test
    void 후보_선정_시간이_끝나면_남은_후보를_랜덤으로_선정하고_라운드를_시작한다() {
        // given
        ContrabandSelectionTestContext context = createContext(timeoutParticipants());

        drainInitialSelectionTimer(context);

        // when
        context.selectionActor().ref().tell(new RoundSelectionTimeout(1));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.smugglerSessionProbe(),
                        PropagateRegisterSmugglerId.class,
                        PropagateFixedSmugglerId.class,
                        PropagateFixedInspectorIdForSmuggler.class
                ),
                () -> ActorTestUtils.expectMessages(
                        context.inspectorSessionProbe(),
                        PropagateRegisterInspectorId.class,
                        PropagateFixedInspectorId.class,
                        PropagateFixedSmugglerIdForInspector.class
                ),
                () -> ActorTestUtils.expectMessages(context.gameChatProbe(), SyncRoundChatId.class),
                () -> ActorTestUtils.expectMessages(context.facadeProbe(), RoundReady.class, StartNewRound.class)
        );
    }

    @Test
    void 다음_라운드_진행을_위해_새로운_후보_선정_타이머를_모든_클라이언트_세션에_전파한다() {
        // given
        ContrabandSelectionTestContext context = createContext(multiPlayerParticipants());

        drainInitialSelectionTimer(context);

        // when
        context.selectionActor().ref().tell(new PrepareNextSelection(2));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSessionProbe(), PropagateSelectionTimer.class),
                () -> ActorTestUtils.expectMessages(context.inspectorSessionProbe(), PropagateSelectionTimer.class)
        );
    }

    @Test
    void 재접속한_밀수꾼_플레이어에게_후보_선정_상태를_동기화한다() {
        // given
        ContrabandSelectionTestContext context = contextWithFixedParticipants();

        // when
        context.selectionActor().ref().tell(new SyncReconnectedPlayer(1L));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.smugglerSessionProbe(),
                        PropagateRegisterSmugglerId.class,
                        PropagateFixedSmugglerId.class,
                        PropagateFixedInspectorIdForSmuggler.class,
                        PropagateSmugglerApprovalState.class
                )
        );
    }

    @Test
    void 재접속한_검사관_플레이어에게_후보_선정_상태를_동기화한다() {
        // given
        ContrabandSelectionTestContext context = contextWithFixedParticipants();

        // when
        context.selectionActor().ref().tell(new SyncReconnectedPlayer(2L));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(
                        context.inspectorSessionProbe(),
                        PropagateRegisterInspectorId.class,
                        PropagateFixedInspectorId.class,
                        PropagateFixedSmugglerIdForInspector.class,
                        PropagateInspectorApprovalState.class
                )
        );
    }

    private ContrabandSelectionTestContext createContext(SelectionParticipants participants) {
        ActorTestUtils.MonitoredActor<ClientSessionCommand> smugglerSession =
                ActorTestUtils.spawnMonitored(actorTestKit, ClientSessionCommand.class, Behaviors.empty());
        ActorTestUtils.MonitoredActor<ClientSessionCommand> inspectorSession =
                ActorTestUtils.spawnMonitored(actorTestKit, ClientSessionCommand.class, Behaviors.empty());

        ClientSessionRegistry registry = createRegistry(
                Map.of(
                        1L, smugglerSession.ref(),
                        2L, inspectorSession.ref()
                )
        );

        SelectionClientMessenger messenger = new SelectionClientMessenger(registry);

        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> gameChat =
                ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameChatCommand.class, Behaviors.empty());
        SelectionChatCoordinator chatCoordinator = new SelectionChatCoordinator(gameChat.ref());

        ActorTestUtils.MonitoredActor<ContrabandGameCommand> facade =
                ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameCommand.class, Behaviors.ignore());

        Behavior<ContrabandGameCommand> behavior = ContrabandSelectionActor.create(
                messenger,
                chatCoordinator,
                participants,
                facade.ref()
        );
        ActorTestUtils.MonitoredActor<ContrabandGameCommand> selectionActor =
                ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameCommand.class, behavior);

        return new ContrabandSelectionTestContext(
                selectionActor,
                smugglerSession.monitor(),
                inspectorSession.monitor(),
                gameChat.monitor(),
                facade.monitor()
        );
    }

    private ClientSessionRegistry createRegistry(Map<Long, ActorRef<ClientSessionCommand>> sessions) {
        TestInbox<ClientSessionRegistry> registryInbox = TestInbox.create();

        Behavior<ContrabandGameCommand> behavior = Behaviors.setup(
                context -> {
                    ClientSessionRegistry registry = ClientSessionRegistry.create(
                            createGame(),
                            sessions,
                            context,
                            1L,
                            "entity-1"
                    );
                    registryInbox.getRef().tell(registry);
                    return Behaviors.empty();
                }
        );

        BehaviorTestUtils.createHarness(behavior);
        return registryInbox.receiveMessage();
    }

    private ContrabandGame createGame() {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사팀", TeamRole.INSPECTOR, List.of(inspector));

        return ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);
    }

    private SelectionParticipants twoPlayerParticipants() {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        return new SelectionParticipants(
                List.of(smuggler),
                List.of(inspector),
                1,
                1
        );
    }

    private SelectionParticipants multiPlayerParticipants() {
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(3L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        return new SelectionParticipants(
                List.of(smuggler1, smuggler2),
                List.of(inspector1, inspector2),
                2,
                2
        );
    }

    private SelectionParticipants timeoutParticipants() {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);

        return new SelectionParticipants(
                List.of(smuggler),
                List.of(inspector),
                2,
                2
        );
    }

    private void drainInitialSelectionTimer(ContrabandSelectionTestContext context) {
        ActorTestUtils.expectMessages(
                context.smugglerSessionProbe(),
                PropagateStartGame.class,
                UpdateContrabandGame.class,
                PropagateSelectionTimer.class
        );
        ActorTestUtils.expectMessages(
                context.inspectorSessionProbe(),
                PropagateStartGame.class,
                UpdateContrabandGame.class,
                PropagateSelectionTimer.class
        );
    }

    private ContrabandSelectionTestContext contextWithRegisteredSmuggler() {
        ContrabandSelectionTestContext context = createContext(multiPlayerParticipants());
        drainInitialSelectionTimer(context);
        context.selectionActor().ref().tell(new RegisterSmugglerId(1L, 1));
        ActorTestUtils.expectMessages(
                context.smugglerSessionProbe(),
                PropagateRegisterSmugglerId.class,
                PropagateSmugglerApprovalState.class
        );
        return context;
    }

    private ContrabandSelectionTestContext contextWithFixedParticipants() {
        ContrabandSelectionTestContext context = createContext(multiPlayerParticipants());
        drainInitialSelectionTimer(context);

        context.selectionActor().ref().tell(new RegisterSmugglerId(1L, 1));
        ActorTestUtils.expectMessages(
                context.smugglerSessionProbe(),
                PropagateRegisterSmugglerId.class,
                PropagateSmugglerApprovalState.class
        );
        context.selectionActor().ref().tell(new FixSmugglerId(1L));
        ActorTestUtils.expectMessages(
                context.smugglerSessionProbe(),
                PropagateFixedSmugglerId.class,
                PropagateSmugglerApprovalState.class
        );
        ActorTestUtils.expectMessages(context.inspectorSessionProbe(), PropagateFixedSmugglerIdForInspector.class);

        context.selectionActor().ref().tell(new RegisterInspectorId(2L, 1));
        ActorTestUtils.expectMessages(
                context.inspectorSessionProbe(),
                PropagateRegisterInspectorId.class,
                PropagateInspectorApprovalState.class
        );
        context.selectionActor().ref().tell(new FixInspectorId(2L));
        ActorTestUtils.expectMessages(
                context.inspectorSessionProbe(),
                PropagateFixedInspectorId.class,
                PropagateInspectorApprovalState.class
        );
        ActorTestUtils.expectMessages(context.smugglerSessionProbe(), PropagateFixedInspectorIdForSmuggler.class);
        ActorTestUtils.expectMessages(context.gameChatProbe(), SyncRoundChatId.class);
        ActorTestUtils.expectMessages(context.facadeProbe(), RoundReady.class, StartNewRound.class);
        return context;
    }

    private ContrabandSelectionTestContext contextWithRegisteredInspector() {
        ContrabandSelectionTestContext context = createContext(multiPlayerParticipants());
        drainInitialSelectionTimer(context);
        context.selectionActor().ref().tell(new RegisterInspectorId(2L, 1));
        ActorTestUtils.expectMessages(
                context.inspectorSessionProbe(),
                PropagateRegisterInspectorId.class,
                PropagateInspectorApprovalState.class
        );
        return context;
    }

    private record ContrabandSelectionTestContext(
            ActorTestUtils.MonitoredActor<ContrabandGameCommand> selectionActor,
            TestProbe<ClientSessionCommand> smugglerSessionProbe,
            TestProbe<ClientSessionCommand> inspectorSessionProbe,
            TestProbe<ContrabandGameChatCommand> gameChatProbe,
            TestProbe<ContrabandGameCommand> facadeProbe
    ) { }
}
