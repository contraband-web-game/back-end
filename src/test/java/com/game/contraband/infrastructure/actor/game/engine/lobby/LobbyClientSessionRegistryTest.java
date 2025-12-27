package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.util.Map;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyClientSessionRegistryTest {

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
    void 클라이언트_세션을_조회한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, session.ref())
        );

        // when
        ActorRef<ClientSessionCommand> actual = registry.get(1L);

        // then
        assertThat(actual).isEqualTo(session.ref());
    }

    @Test
    void 클라이언트_세션을_추가한다() {
        // given
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(null);
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );

        // when
        registry.add(1L, session.ref());

        // then
        assertThat(registry.get(1L)).isEqualTo(session.ref());
    }

    @Test
    void 클라이언트_세션을_제거한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, session.ref())
        );

        // when
        registry.remove(1L);

        // then
        assertThat(registry.get(1L)).isNull();
    }

    @Test
    void 플레이어_인원_수를_조회한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, session.ref())
        );

        // when
        int actual = registry.size();

        // then
        assertThat(actual).isEqualTo(1);
    }

    @Test
    void 클라이언트_세션_목록을_반환한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, session.ref())
        );

        // when
        Iterable<ActorRef<ClientSessionCommand>> actual = registry.values();

        // then
        assertThat(actual).containsExactly(session.ref());
    }

    @Test
    void 클라이언트_세션을_순회한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, session.ref())
        );

        // when
        registry.forEachSession(target -> target.tell(new ClientSessionCommand() { }));

        // then
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void 클라이언트_세션_맵을_조회한다() {
        // given
        ActorTestUtils.MonitoredActor<ClientSessionCommand> session = ActorTestUtils.spawnMonitored(
                actorTestKit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyClientSessionRegistry registry = new LobbyClientSessionRegistry(
                Map.of(1L, session.ref())
        );

        // when
        Map<Long, ActorRef<ClientSessionCommand>> actual = registry.asMapView();

        // then
        assertAll(
                () -> assertThat(actual).containsEntry(1L, session.ref()),
                () -> assertThat(actual).hasSize(1)
        );
    }
}
