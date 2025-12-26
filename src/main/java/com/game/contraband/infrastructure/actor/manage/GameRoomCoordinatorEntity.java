package com.game.contraband.infrastructure.actor.manage;

import static com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.*;

import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.pattern.StatusReply;

public class GameRoomCoordinatorEntity extends AbstractBehavior<GameRoomCoordinatorCommand> {

    private static final String ENTITY_ID_PREFIX = "game-rooms-";

    public static Behavior<GameRoomCoordinatorCommand> create(
            int maxRoomsPerEntity,
            GameLifecycleEventPublisher gameLifecycleEventPublisher
    ) {
        return Behaviors.setup(
                context -> new GameRoomCoordinatorEntity(context, maxRoomsPerEntity, gameLifecycleEventPublisher)
        );
    }

    private GameRoomCoordinatorEntity(
            ActorContext<GameRoomCoordinatorCommand> context,
            int maxRoomsPerEntity,
            GameLifecycleEventPublisher gameLifecycleEventPublisher
    ) {
        super(context);

        this.maxRoomsPerEntity = maxRoomsPerEntity;
        this.gameLifecycleEventPublisher = gameLifecycleEventPublisher;
    }

    private final int maxRoomsPerEntity;
    private final Map<String, Integer> entityRoomCounts = new HashMap<>();
    private final Map<Long, String> roomToEntity = new HashMap<>();
    private final GameLifecycleEventPublisher gameLifecycleEventPublisher;
    private Long entityIdSequence = 1L;

    @Override
    public Receive<GameRoomCoordinatorCommand> createReceive() {
        return newReceiveBuilder().onMessage(AllocateEntity.class, this::onAllocateEntity)
                                  .onMessage(RegisterRoom.class, this::onRegisterRoom)
                                  .onMessage(SyncRoomRemoved.class, this::onSyncRoomRemoved)
                                  .onMessage(ResolveEntityId.class, this::onResolveEntityId)
                                  .onMessage(RoomRemovalNotification.class, this::onRoomRemovalNotification)
                                  .build();
    }

    private Behavior<GameRoomCoordinatorCommand> onAllocateEntity(AllocateEntity command) {
        try {
            String entityId = selectEntityId();
            command.replyTo().tell(StatusReply.success(new TargetEntity(entityId)));
        } catch (Exception e) {
            command.replyTo().tell(StatusReply.error(e));
        }
        return this;
    }

    private Behavior<GameRoomCoordinatorCommand> onSyncRoomRemoved(SyncRoomRemoved command) {
        String entityId = roomToEntity.remove(command.roomId());

        if (entityId != null && entityRoomCounts.containsKey(entityId)) {
            entityRoomCounts.compute(entityId, (k, v) -> v == null || v <= 1 ? 0 : v - 1);
            publishRoomRemovedEvent(command, entityId);
        }

        return this;
    }

    private void publishRoomRemovedEvent(SyncRoomRemoved command, String entityId) {
        if (entityRoomCounts.get(entityId) == 0) {
            gameLifecycleEventPublisher.publishEntityRemoved(entityId);
        }

        gameLifecycleEventPublisher.publishRoomRemoved(entityId, command.roomId());
    }

    private Behavior<GameRoomCoordinatorCommand> onResolveEntityId(ResolveEntityId command) {
        try {
            String entityId = roomToEntity.get(command.roomId());

            if (entityId == null) {
                command.replyTo()
                       .tell(
                               StatusReply.error(
                                       new IllegalStateException("해당 roomId에 매핑된 엔티티가 없습니다.")
                               )
                       );
                return this;
            }

            command.replyTo().tell(StatusReply.success(entityId));
        } catch (Exception e) {
            command.replyTo().tell(StatusReply.error(e));
        }

        return this;
    }

    private String selectEntityId() {
        String target = null;
        int minCount = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : entityRoomCounts.entrySet()) {
            int count = entry.getValue();

            if (count < minCount && count < maxRoomsPerEntity) {
                minCount = count;
                target = entry.getKey();
            }
        }

        if (target != null) {
            return target;
        }

        String newId = entityIdForSequence(entityIdSequence++);

        entityRoomCounts.put(newId, 0);
        gameLifecycleEventPublisher.publishEntityCreated(newId);
        return newId;
    }

    private String entityIdForSequence(Long sequence) {
        return ENTITY_ID_PREFIX + sequence;
    }

    private Behavior<GameRoomCoordinatorCommand> onRegisterRoom(RegisterRoom command) {
        if (!entityRoomCounts.containsKey(command.entityId())) {
            entityRoomCounts.put(command.entityId(), 0);
        }

        roomToEntity.put(command.roomId(), command.entityId());
        entityRoomCounts.compute(command.entityId(), (k, v) -> v == null ? 1 : v + 1);
        return this;
    }

    private Behavior<GameRoomCoordinatorCommand> onRoomRemovalNotification(RoomRemovalNotification command) {
        String entityId = roomToEntity.remove(command.roomId());

        if (entityId != null && entityRoomCounts.containsKey(entityId)) {
            entityRoomCounts.compute(entityId, (k, v) -> v == null || v <= 1 ? 0 : v - 1);
            if (entityRoomCounts.get(entityId) == 0) {
                gameLifecycleEventPublisher.publishEntityRemoved(entityId);
            }
            gameLifecycleEventPublisher.publishRoomRemoved(entityId, command.roomId());
        }

        return this;
    }

    public interface GameRoomCoordinatorCommand extends CborSerializable { }

    public record AllocateEntity(ActorRef<StatusReply<TargetEntity>> replyTo) implements GameRoomCoordinatorCommand { }

    public record TargetEntity(String entityId) implements CborSerializable { }

    public record RegisterRoom(long roomId, String entityId) implements GameRoomCoordinatorCommand { }

    public record SyncRoomRemoved(long roomId) implements GameRoomCoordinatorCommand { }

    public record ResolveEntityId(long roomId, ActorRef<StatusReply<String>> replyTo) implements GameRoomCoordinatorCommand { }

    public record RoomRemovalNotification(long roomId) implements GameRoomCoordinatorCommand { }
}
