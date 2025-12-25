package com.game.contraband.global.actor;

import static com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.*;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.global.actor.GuardianActor.GuardianCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.event.MonitorEventBroadcaster;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorRole;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorState;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class GuardianActor extends AbstractBehavior<GuardianCommand>  {

    public static Behavior<GuardianCommand> create(
            ActorRef<RoomDirectoryCommand> roomDirectory,
            ActorRef<GameRoomCoordinatorCommand> gameRoomsCoordinator,
            MonitorEventBroadcaster monitorEventBroadcaster,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(
                context -> {
                    publishCreatedActorEvent(context, monitorEventBroadcaster);

                    return new GuardianActor(
                            context,
                            roomDirectory,
                            gameRoomsCoordinator,
                            monitorEventBroadcaster,
                            chatBlacklistRepository
                    );
                }
        );
    }

    private static void publishCreatedActorEvent(
            ActorContext<GuardianCommand> context,
            MonitorEventBroadcaster monitorEventBroadcaster
    ) {
        if (monitorEventBroadcaster != null) {
            monitorEventBroadcaster.publishActorEvent(
                    context.getSelf().path().toString(),
                    context.getSelf().path().parent().toString(),
                    MonitorActorRole.GUARDIAN_ACTOR,
                    MonitorActorState.CREATED
            );
        }
    }

    private GuardianActor(
            ActorContext<GuardianCommand> context,
            ActorRef<RoomDirectoryCommand> roomDirectory,
            ActorRef<GameRoomCoordinatorCommand> gameRoomsCoordinator,
            MonitorEventBroadcaster monitorEventBroadcaster,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        super(context);

        this.roomDirectory = roomDirectory;
        this.gameRoomsCoordinator = gameRoomsCoordinator;
        this.monitorEventBroadcaster = monitorEventBroadcaster;
        this.chatBlacklistRepository = chatBlacklistRepository;
    }

    private final ActorRef<RoomDirectoryCommand> roomDirectory;
    private final ActorRef<GameRoomCoordinatorCommand> gameRoomsCoordinator;
    private final MonitorEventBroadcaster monitorEventBroadcaster;
    private final ChatBlacklistRepository chatBlacklistRepository;

    @Override
    public Receive<GuardianCommand> createReceive() {
        return newReceiveBuilder().onMessage(SpawnClientSession.class, this::onSpawnClientSession)
                                  .onMessage(GetGameRoomsCoordinator.class, this::onGetGameRoomsCoordinator)
                                  .build();
    }

    private Behavior<GuardianCommand> onSpawnClientSession(SpawnClientSession command) {
        ActorRef<LocalDirectoryCommand> localCache = getContext().spawn(
                RoomDirectorySubscriberActor.create(roomDirectory, monitorEventBroadcaster),
                "room-directory-cache-" + command.playerId()
        );
        ActorRef<ClientSessionCommand> clientSession = getContext().spawn(
                ClientSessionActor.create(
                        command.playerId(),
                        command.clientWebSocketMessageSender(),
                        localCache, chatBlacklistRepository
                ),
                "client-session-" + System.nanoTime() + "-" + command.playerId()
        );

        command.replyTo()
               .tell(new SpawnedClientSession(clientSession));
        return this;
    }

    private Behavior<GuardianCommand> onGetGameRoomsCoordinator(GetGameRoomsCoordinator command) {
        command.replyTo()
               .tell(gameRoomsCoordinator);

        return this;
    }

    public interface GuardianCommand extends CborSerializable { }

    public record SpawnClientSession(Long playerId, ClientWebSocketMessageSender clientWebSocketMessageSender, ActorRef<GuardianCommand> replyTo) implements GuardianCommand { }

    public record SpawnedClientSession(ActorRef<ClientSessionCommand> clientSession) implements GuardianCommand { }

    public record GetGameRoomsCoordinator(ActorRef<ActorRef<GameRoomCoordinatorCommand>> replyTo) implements GuardianCommand { }
}
