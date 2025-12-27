package com.game.contraband.infrastructure.actor.game.engine.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClearActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateContrabandGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateRegisterSmugglerId;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import com.game.contraband.infrastructure.actor.utils.BehaviorTestUtils;
import java.time.Duration;
import java.util.LinkedHashMap;
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
class ClientSessionRegistryTest {

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
    void 플레이어_세션을_등록할_때_게임_시작_및_상태_동기화_메시지를_전달한다() {
        // given
        Long smugglerId = 1L;
        Long inspectorId = 2L;

        // when
        ClientSessionRegistryTestContext context = createTestContext(smugglerId, inspectorId, 1);

        // then
        TestProbe<ClientSessionCommand> smugglerSession = context.session(smugglerId);
        TestProbe<ClientSessionCommand> inspectorSession = context.session(inspectorId);

        assertAll(
                () -> assertThat(context.registry().findInTotalSessions(smugglerId))
                        .isEqualTo(smugglerSession.getRef()),
                () -> assertThat(context.registry().findInTeamSessions(TeamRole.SMUGGLER, smugglerId))
                        .isEqualTo(smugglerSession.getRef()),
                () -> assertThat(context.registry().findInTeamSessions(TeamRole.INSPECTOR, inspectorId))
                        .isEqualTo(inspectorSession.getRef()),

                () -> ActorTestUtils.expectMessages(
                        smugglerSession,
                        PropagateStartGame.class,
                        UpdateContrabandGame.class
                ),
                () -> ActorTestUtils.expectMessages(
                        inspectorSession,
                        PropagateStartGame.class,
                        UpdateContrabandGame.class
                )
        );
    }

    @Test
    void 등록된_전체_클라이언트_세션에_메시지를_전파한다() {
        // given
        Long smugglerId = 1L;
        Long inspectorId = 2L;
        ClientSessionRegistryTestContext context = createTestContext(smugglerId, inspectorId, 2);
        TestProbe<ClientSessionCommand> smugglerSession = context.session(smugglerId);
        TestProbe<ClientSessionCommand> inspectorSession = context.session(inspectorId);

        ActorTestUtils.waitUntilMessages(smugglerSession, PropagateStartGame.class, UpdateContrabandGame.class);
        ActorTestUtils.waitUntilMessages(inspectorSession, PropagateStartGame.class, UpdateContrabandGame.class);

        // when
        ClientSessionRegistry clientSessionRegistry = context.registry();

        clientSessionRegistry.tellAll(new ClearActiveGame());

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(smugglerSession, ClearActiveGame.class),
                () -> ActorTestUtils.expectMessages(inspectorSession, ClearActiveGame.class)
        );
    }

    @Test
    void 등록된_특정_팀_클라이언트_세션에_메시지를_전파한다() {
        // given
        Long smugglerId = 1L;
        Long inspectorId = 2L;
        ClientSessionRegistryTestContext context = createTestContext(smugglerId, inspectorId, 2);
        TestProbe<ClientSessionCommand> smugglerSession = context.session(smugglerId);
        TestProbe<ClientSessionCommand> inspectorSession = context.session(inspectorId);

        ActorTestUtils.waitUntilMessages(smugglerSession, PropagateStartGame.class, UpdateContrabandGame.class);
        ActorTestUtils.waitUntilMessages(inspectorSession, PropagateStartGame.class, UpdateContrabandGame.class);

        // when
        ClientSessionRegistry clientSessionregistry = context.registry();

        clientSessionregistry.tellTeam(TeamRole.SMUGGLER, new PropagateRegisterSmugglerId(smugglerId));

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(smugglerSession, PropagateRegisterSmugglerId.class),
                () -> ActorTestUtils.expectNoMessages(inspectorSession, Duration.ofMillis(300L))
        );
    }

    @Test
    void 게임_채팅_Actor를_모든_클라이언트_세션과_동기화한다() {
        // given
        Long smugglerId = 1L;
        Long inspectorId = 2L;
        ClientSessionRegistryTestContext context = createTestContext(smugglerId, inspectorId, 3);
        TestProbe<ClientSessionCommand> smugglerSession = context.session(smugglerId);
        TestProbe<ClientSessionCommand> inspectorSession = context.session(inspectorId);

        ActorTestUtils.expectMessages(smugglerSession, PropagateStartGame.class, UpdateContrabandGame.class);
        ActorTestUtils.expectMessages(inspectorSession, PropagateStartGame.class, UpdateContrabandGame.class);

        TestProbe<ContrabandGameChatCommand> gameChat = actorTestKit.createTestProbe(ContrabandGameChatCommand.class);

        // when
        ClientSessionRegistry clientSessionRegistry = context.registry();

        clientSessionRegistry.syncGameChatForAll(gameChat.getRef());

        // then
        assertAll(
                () -> ActorTestUtils.expectMessages(smugglerSession, SyncContrabandGameChat.class),
                () -> ActorTestUtils.expectMessages(inspectorSession, SyncContrabandGameChat.class)
        );
    }

    private ContrabandGame createGame(Long smugglerId, Long inspectorId) {
        PlayerProfile smuggler = PlayerProfile.create(smugglerId, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(inspectorId, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼 팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관 팀", TeamRole.INSPECTOR, List.of(inspector));

        return ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);
    }

    private ClientSessionRegistryTestContext createTestContext(
            Long smugglerId,
            Long inspectorId,
            int identityOrder
    ) {
        RoomIdentity identity = roomIdentity(identityOrder);

        return createTestContext(
                smugglerId,
                inspectorId,
                identity.roomId(),
                identity.entityId()
        );
    }

    private ClientSessionRegistryTestContext createTestContext(
            Long smugglerId,
            Long inspectorId,
            Long roomId,
            String entityId
    ) {
        ContrabandGame contrabandGame = createGame(smugglerId, inspectorId);
        Map<Long, TestProbe<ClientSessionCommand>> sessions = createSessionProbes(smugglerId, inspectorId);
        Map<Long, ActorRef<ClientSessionCommand>> sessionRefs = toSessionRefs(sessions);

        RegistryContext registryContext = createRegistry(contrabandGame, sessionRefs, roomId, entityId);

        return new ClientSessionRegistryTestContext(
                sessions,
                registryContext.registry(),
                registryContext.contrabandGameRef()
        );
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

    private RoomIdentity roomIdentity(int order) {
        Long roomId = 100L * order;
        String entityId = "entity-" + order;

        return new RoomIdentity(roomId, entityId);
    }

    private Map<Long, TestProbe<ClientSessionCommand>> createSessionProbes(Long smugglerId, Long inspectorId) {
        Map<Long, TestProbe<ClientSessionCommand>> probes = new LinkedHashMap<>();

        probes.put(smugglerId, actorTestKit.createTestProbe(ClientSessionCommand.class));
        probes.put(inspectorId, actorTestKit.createTestProbe(ClientSessionCommand.class));
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

    private record ClientSessionRegistryTestContext(
            Map<Long, TestProbe<ClientSessionCommand>> sessions,
            ClientSessionRegistry registry,
            ActorRef<ContrabandGameCommand> contrabandGameRef
    ) {
        TestProbe<ClientSessionCommand> session(Long playerId) {
            return sessions.get(playerId);
        }
    }

    private record RoomIdentity(Long roomId, String entityId) { }
}
