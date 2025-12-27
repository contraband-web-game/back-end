package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClearActiveGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSelectionTimer;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
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
class SelectionClientMessengerTest {

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
    void 팀_클라이언트_세션을_조회한다() {
        // given
        TestContext context = createContext();
        SelectionClientMessenger messenger = context.messenger();

        // when
        ActorRef<ClientSessionCommand> actual = messenger.teamSession(TeamRole.SMUGGLER, 1L);

        // then
        assertThat(actual).isEqualTo(context.smugglerSessionRef());
    }

    @Test
    void 팀_클라이언트_세션에서_특정_플레이어가_존재하는지_확인한다() {
        // given
        TestContext context = createContext();
        SelectionClientMessenger messenger = context.messenger();

        // when
        boolean actual = messenger.hasTeamSession(TeamRole.INSPECTOR, 2L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 전체_클라이언트_세션을_조회한다() {
        // given
        TestContext context = createContext();
        SelectionClientMessenger messenger = context.messenger();

        // when
        ActorRef<ClientSessionCommand> actual = messenger.totalSession(1L);

        // then
        assertThat(actual).isEqualTo(context.smugglerSessionRef());
    }

    @Test
    void 팀_클라이언트_세션에게_메시지를_전파한다() {
        // given
        TestContext context = createContext();
        SelectionClientMessenger messenger = context.messenger();
        ClearActiveGame command = new ClearActiveGame();

        // when
        messenger.tellTeam(TeamRole.SMUGGLER, command);

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSessionProbe(), ClearActiveGame.class),
                () -> ActorTestUtils.expectNoMessages(context.inspectorSessionProbe(), Duration.ofMillis(300L))
        );
    }

    @Test
    void 전체_클라이언트_세션에게_메시지를_전파한다() {
        // given
        TestContext context = createContext();
        SelectionClientMessenger messenger = context.messenger();
        ClearActiveGame command = new ClearActiveGame();

        // when
        messenger.tellAll(command);

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSessionProbe(), ClearActiveGame.class),
                () -> ActorTestUtils.expectMessages(context.inspectorSessionProbe(), ClearActiveGame.class)
        );
    }

    @Test
    void 후보_선정_타이머를_모든_클라이언트_세션에_전파한다() {
        // given
        TestContext context = createContext();
        SelectionClientMessenger messenger = context.messenger();

        int round = 1;
        long startedAt = 1000L;
        long duration = 30000L;
        long serverNow = 2000L;
        long endAt = 31000L;

        // when
        messenger.broadcastSelectionTimer(round, startedAt, duration, serverNow, endAt);

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.smugglerSessionProbe(), PropagateSelectionTimer.class),
                () -> ActorTestUtils.expectMessages(context.inspectorSessionProbe(), PropagateSelectionTimer.class)
        );
    }

    private TestContext createContext() {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사팀", TeamRole.INSPECTOR, List.of(inspector));
        ContrabandGame contrabandGame = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);

        ActorTestUtils.MonitoredActor<ClientSessionCommand> smugglerSession =
                ActorTestUtils.spawnMonitored(actorTestKit, ClientSessionCommand.class, Behaviors.empty());
        ActorTestUtils.MonitoredActor<ClientSessionCommand> inspectorSession =
                ActorTestUtils.spawnMonitored(actorTestKit, ClientSessionCommand.class, Behaviors.empty());

        ClientSessionRegistry registry = createRegistry(
                Map.of(
                        1L, smugglerSession.ref(),
                        2L, inspectorSession.ref()
                ),
                contrabandGame
        );

        SelectionClientMessenger messenger = new SelectionClientMessenger(registry);
        return new TestContext(messenger, smugglerSession, inspectorSession);
    }

    private ClientSessionRegistry createRegistry(
            Map<Long, ActorRef<ClientSessionCommand>> sessions,
            ContrabandGame contrabandGame
    ) {
        TestInbox<ClientSessionRegistry> registryInbox = TestInbox.create();

        Behavior<ContrabandGameCommand> behavior = Behaviors.setup(
                context -> {
                    ClientSessionRegistry registry = ClientSessionRegistry.create(
                            contrabandGame,
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

    private record TestContext(
            SelectionClientMessenger messenger,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> smugglerSession,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> inspectorSession
    ) {
        ActorRef<ClientSessionCommand> smugglerSessionRef() {
            return smugglerSession.ref();
        }

        TestProbe<ClientSessionCommand> smugglerSessionProbe() {
            return smugglerSession.monitor();
        }

        TestProbe<ClientSessionCommand> inspectorSessionProbe() {
            return inspectorSession.monitor();
        }
    }
}
