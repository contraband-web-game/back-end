package com.game.contraband.infrastructure.actor.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RegisterRoom;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RoomRemovalNotification;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CoordinatorGatewayTest {

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
    void 게임방_등록_요청을_전송한다() {
        // given
        ActorTestUtils.MonitoredActor<GameRoomCoordinatorCommand> coordinator = ActorTestUtils.spawnMonitored(
                actorTestKit,
                GameRoomCoordinatorCommand.class,
                receiveAndStay()
        );
        CoordinatorGateway gateway = new CoordinatorGateway(coordinator.ref());

        // when
        gateway.registerRoom(1L, "entity");

        // then
        GameRoomCoordinatorCommand actual = coordinator.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual).isInstanceOf(RegisterRoom.class),
                () -> assertThat(((RegisterRoom) actual).roomId()).isEqualTo(1L),
                () -> assertThat(((RegisterRoom) actual).entityId()).isEqualTo("entity")
        );
    }

    @Test
    void 게임방_삭제_알림을_전송한다() {
        // given
        ActorTestUtils.MonitoredActor<GameRoomCoordinatorCommand> coordinator = ActorTestUtils.spawnMonitored(
                actorTestKit,
                GameRoomCoordinatorCommand.class,
                receiveAndStay()
        );
        CoordinatorGateway gateway = new CoordinatorGateway(coordinator.ref());

        // when
        gateway.notifyRoomRemoved(5L);

        // then
        GameRoomCoordinatorCommand actual = coordinator.monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual).isInstanceOf(RoomRemovalNotification.class),
                () -> assertThat(((RoomRemovalNotification) actual).roomId()).isEqualTo(5L)
        );
    }

    private Behavior<GameRoomCoordinatorCommand> receiveAndStay() {
        return Behaviors.receiveMessage(message -> Behaviors.same());
    }
}
