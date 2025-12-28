package com.game.contraband.infrastructure.actor.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoomDirectorySyncTest {

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
    void 로비_생성_동기화를_전파한다() {
        // given
        ActorTestUtils.MonitoredActor<RoomDirectoryCommand> directory = ActorTestUtils.spawnMonitored(
                actorTestKit,
                RoomDirectoryCommand.class,
                Behaviors.ignore()
        );
        RoomDirectorySync sync = new RoomDirectorySync(directory.ref());
        RoomDirectorySnapshot snapshot = new RoomDirectorySnapshot(
                1L,
                "방",
                4,
                2,
                "entity",
                false
        );

        // when
        sync.register(snapshot);

        // then
        RoomDirectoryActor.SyncRoomRegistered actual =
                (RoomDirectoryActor.SyncRoomRegistered) directory.monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.roomSummary().roomId()).isEqualTo(1L),
                () -> assertThat(actual.roomSummary().lobbyName()).isEqualTo("방"),
                () -> assertThat(actual.roomSummary().entityId()).isEqualTo("entity")
        );
    }

    @Test
    void 로비_삭제_동기화를_전파한다() {
        // given
        ActorTestUtils.MonitoredActor<RoomDirectoryCommand> directory = ActorTestUtils.spawnMonitored(
                actorTestKit,
                RoomDirectoryCommand.class,
                Behaviors.ignore()
        );
        RoomDirectorySync sync = new RoomDirectorySync(directory.ref());

        // when
        sync.remove(5L);

        // then
        RoomDirectoryActor.RemoveRoom actual = (RoomDirectoryActor.RemoveRoom) directory.monitor().receiveMessage();

        assertThat(actual.roomId()).isEqualTo(5L);
    }
}
