package com.game.contraband.infrastructure.actor.game.engine.match.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearContrabandGameChat;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
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
class RoundChatCoordinatorTest {

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
    void 라운드_채팅_ID를_초기화한다() {
        // given
        RoundChatCoordinatorTestContext context = createTestContext();
        RoundChatCoordinator coordinator = context.coordinator();

        // when
        coordinator.clearRoundChatId();

        // then
        ActorTestUtils.expectMessages(context.gameChat(), ContrabandGameChatActor.ClearRoundChatId.class);
    }

    @Test
    void 모든_플레이어의_게임_채팅을_초기화한다() {
        // given
        RoundChatCoordinatorTestContext context = createTestContext(1L, 2L);
        RoundChatCoordinator coordinator = context.coordinator();

        // when
        coordinator.clearGameChatForAll();

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(context.session(1L), ClearContrabandGameChat.class),
                () -> ActorTestUtils.expectMessages(context.session(2L), ClearContrabandGameChat.class)
        );
    }

    @Test
    void 게임_채팅_Actor를_반환한다() {
        // given
        RoundChatCoordinatorTestContext context = createTestContext();
        RoundChatCoordinator coordinator = context.coordinator();

        // when
        ActorRef<ContrabandGameChatCommand> gameChat = coordinator.gameChat();

        // then
        assertThat(gameChat).isEqualTo(context.gameChat().getRef());
    }

    private RoundChatCoordinatorTestContext createTestContext(Long... playerIds) {
        Map<Long, TestProbe<ClientSessionCommand>> sessions = createSessionProbes(playerIds);
        Map<Long, ActorRef<ClientSessionCommand>> sessionRefs = toSessionRefs(sessions);

        return createTestContext(sessions, sessionRefs);
    }

    private RoundChatCoordinatorTestContext createTestContext(
            Map<Long, TestProbe<ClientSessionCommand>> sessions,
            Map<Long, ActorRef<ClientSessionCommand>> sessionRefs
    ) {
        ClientSessionRegistry registry = createRegistry(sessionRefs);
        TestProbe<ContrabandGameChatCommand> gameChat = actorTestKit.createTestProbe(ContrabandGameChatCommand.class);

        RoundChatCoordinator coordinator = new RoundChatCoordinator(gameChat.getRef(), registry);

        return new RoundChatCoordinatorTestContext(coordinator, gameChat, sessions);
    }

    private ClientSessionRegistry createRegistry(Map<Long, ActorRef<ClientSessionCommand>> totalSessions) {
        TestProbe<ClientSessionRegistry> registryProbe = actorTestKit.createTestProbe(ClientSessionRegistry.class);

        Behavior<ContrabandGameCommand> behavior = Behaviors.setup(
                context -> {
                    ClientSessionRegistry registry = ClientSessionRegistry.create(
                            emptyContrabandGame(),
                            totalSessions,
                            context,
                            1L,
                            "entity-1"
                    );
                    registryProbe.getRef().tell(registry);
                    return Behaviors.empty();
                }
        );

        ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameCommand.class, behavior);

        return registryProbe.receiveMessage();
    }

    private ContrabandGame emptyContrabandGame() {
        TeamRoster smugglerRoster = TeamRoster.create("smuggler", TeamRole.SMUGGLER, Collections.emptyList());
        TeamRoster inspectorRoster = TeamRoster.create("inspector", TeamRole.INSPECTOR, Collections.emptyList());

        return ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);
    }

    private Map<Long, TestProbe<ClientSessionCommand>> createSessionProbes(Long... playerIds) {
        Map<Long, TestProbe<ClientSessionCommand>> sessions = new LinkedHashMap<>();

        for (Long playerId : playerIds) {
            sessions.put(playerId, actorTestKit.createTestProbe());
        }

        return sessions;
    }

    private Map<Long, ActorRef<ClientSessionCommand>> toSessionRefs(
            Map<Long, TestProbe<ClientSessionCommand>> sessions
    ) {
        Map<Long, ActorRef<ClientSessionCommand>> sessionRefs = new LinkedHashMap<>();

        for (Map.Entry<Long, TestProbe<ClientSessionCommand>> entry : sessions.entrySet()) {
            sessionRefs.put(entry.getKey(), entry.getValue().getRef());
        }

        return sessionRefs;
    }

    private record RoundChatCoordinatorTestContext(
            RoundChatCoordinator coordinator,
            TestProbe<ContrabandGameChatCommand> gameChat,
            Map<Long, TestProbe<ClientSessionCommand>> sessions
    ) {
        TestProbe<ClientSessionCommand> session(Long playerId) {
            return sessions.get(playerId);
        }
    }
}
