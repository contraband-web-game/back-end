package com.game.contraband.infrastructure.actor.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.LifecycleType;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.AllocateEntity;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RegisterRoom;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.ResolveEntityId;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.RoomRemovalNotification;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.SyncRoomRemoved;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.TargetEntity;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.pattern.StatusReply;

@SuppressWarnings({"NonAsciiCharacters", "rawtypes", "unchecked"})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GameRoomCoordinatorEntityTest {

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
    void 엔티티_할당시_새로운_엔티티를_생성하고_응답한다() {
        // given
        TestContext context = createContext(2);
        ActorTestUtils.MonitoredActor<StatusReply> reply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );

        // when
        ActorRef<StatusReply<TargetEntity>> allocateReplyRef = castReply(reply.ref());
        context.actor().tell(new AllocateEntity(allocateReplyRef));

        // then
        StatusReply actual = reply.monitor().receiveMessage();

        assertAll(
                () -> assertThat(actual.isSuccess()).isTrue(),
                () -> assertThat(((TargetEntity) actual.getValue()).entityId()).isEqualTo("game-rooms-1"),
                () -> assertThat(context.publisher().events()).hasSize(1),
                () -> assertThat(context.publisher().events().get(0).type()).isEqualTo(LifecycleType.ENTITY_CREATED)
        );
    }

    @Test
    void 엔티티_할당시_여유가_있으면_기존_엔티티를_재사용한다() {
        // given
        TestContext context = createContext(2);
        ActorTestUtils.MonitoredActor<StatusReply> reply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );
        ActorRef<StatusReply<TargetEntity>> firstAllocateRef = castReply(reply.ref());

        context.actor().tell(new AllocateEntity(firstAllocateRef));

        StatusReply firstAllocation = reply.monitor().receiveMessage();

        context.actor().tell(new RegisterRoom(1L, ((TargetEntity) firstAllocation.getValue()).entityId()));

        // when
        ActorRef<StatusReply<TargetEntity>> allocateReplyRef = castReply(reply.ref());

        context.actor().tell(new AllocateEntity(allocateReplyRef));

        // then
        StatusReply actual = reply.monitor().receiveMessage();

        assertAll(
                () -> assertThat(((TargetEntity) actual.getValue()).entityId()).isEqualTo(
                        ((TargetEntity) firstAllocation.getValue()).entityId()
                ),
                () -> assertThat(context.publisher().events()).hasSize(1)
        );
    }

    @Test
    void 게임방을_등록하면_매핑해서_저장한다() {
        // given
        TestContext context = createContext(2);
        ActorTestUtils.MonitoredActor<StatusReply> allocationReply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<StatusReply> resolveReply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );
        ActorRef<StatusReply<TargetEntity>> allocateReplyRef = castReply(allocationReply.ref());

        context.actor().tell(new AllocateEntity(allocateReplyRef));

        StatusReply allocated = allocationReply.monitor().receiveMessage();

        // when
        context.actor().tell(new RegisterRoom(5L, ((TargetEntity) allocated.getValue()).entityId()));

        ActorRef<StatusReply<String>> resolveRef = castResolve(resolveReply.ref());

        context.actor().tell(new ResolveEntityId(5L, resolveRef));

        // then
        StatusReply actual = resolveReply.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.isSuccess()).isTrue(),
                () -> assertThat(actual.getValue()).isEqualTo(((TargetEntity) allocated.getValue()).entityId())
        );
    }

    @Test
    void 지정한_ID의_게임방이_없으면_엔티티_ID_조회에_실패한다() {
        // given
        TestContext context = createContext(1);
        ActorTestUtils.MonitoredActor<StatusReply> resolveReply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );

        // when
        ActorRef<StatusReply<String>> resolveRef = castResolve(resolveReply.ref());

        context.actor().tell(new ResolveEntityId(99L, resolveRef));

        // then
        StatusReply actual = resolveReply.monitor().receiveMessage();

        assertThat(actual.isError()).isTrue();
    }

    @Test
    void 게임방_삭제_동기화시_매핑을_제거하고_이벤트를_발행한다() {
        // given
        TestContext context = createContext(2);
        ActorTestUtils.MonitoredActor<StatusReply> allocationReply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );
        ActorTestUtils.MonitoredActor<StatusReply> resolveReply = ActorTestUtils.spawnMonitored(
                actorTestKit,
                StatusReply.class,
                Behaviors.ignore()
        );
        ActorRef<StatusReply<TargetEntity>> allocateReplyRef = castReply(allocationReply.ref());

        context.actor().tell(new AllocateEntity(allocateReplyRef));

        StatusReply allocation = allocationReply.monitor().receiveMessage();

        context.actor().tell(new RegisterRoom(7L, ((TargetEntity) allocation.getValue()).entityId()));

        // when
        context.actor().tell(new SyncRoomRemoved(7L));
        ActorRef<StatusReply<String>> resolveRef = castResolve(resolveReply.ref());
        context.actor().tell(new ResolveEntityId(7L, resolveRef));

        // then
        StatusReply actual = resolveReply.monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.isError()).isTrue(),
                () -> assertThat(context.publisher().events()).hasSize(3),
                () -> assertThat(context.publisher().events().get(1).type()).isEqualTo(LifecycleType.ENTITY_REMOVED),
                () -> assertThat(context.publisher().events().get(2).type()).isEqualTo(LifecycleType.ROOM_REMOVED)
        );
    }

    @Test
    void 엔티티에_매핑이_없을때_게임방_삭제_알림을_무시한다() {
        // given
        TestContext context = createContext(2);

        // when
        context.actor().tell(new RoomRemovalNotification(15L));

        // then
        assertThat(context.publisher().events()).isEmpty();
    }

    private TestContext createContext(int maxRoomsPerEntity) {
        SpyPublisher publisher = new SpyPublisher();
        Behavior<GameRoomCoordinatorCommand> behavior = GameRoomCoordinatorEntity.create(maxRoomsPerEntity, publisher);
        ActorRef<GameRoomCoordinatorCommand> actor = actorTestKit.spawn(behavior);
        return new TestContext(actor, publisher);
    }

    private ActorRef<StatusReply<TargetEntity>> castReply(ActorRef<StatusReply> ref) {
        return (ActorRef<StatusReply<TargetEntity>>) (ActorRef<?>) ref;
    }

    private ActorRef<StatusReply<String>> castResolve(ActorRef<StatusReply> ref) {
        return (ActorRef<StatusReply<String>>) (ActorRef<?>) ref;
    }

    private record TestContext(
            ActorRef<GameRoomCoordinatorCommand> actor,
            SpyPublisher publisher
    ) {
        public ActorRef<GameRoomCoordinatorCommand> actor() {
            return actor;
        }

        public SpyPublisher publisher() {
            return publisher;
        }
    }

    private static final class SpyPublisher implements GameLifecycleEventPublisher {

        private final List<GameLifecycleEvent> events = new ArrayList<>();

        @Override
        public void publish(GameLifecycleEvent event) {
            events.add(event);
        }

        List<GameLifecycleEvent> events() {
            return events;
        }
    }
}
