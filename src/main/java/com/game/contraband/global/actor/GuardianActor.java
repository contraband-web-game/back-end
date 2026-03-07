package com.game.contraband.global.actor;

import static com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor.*;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.global.actor.GuardianActor.GuardianCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionGatewayActor;
import com.game.contraband.infrastructure.actor.client.ClientSessionProxyActor;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySubscriberActor;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.event.MonitorEventBroadcaster;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorRole;
import com.game.contraband.infrastructure.monitor.payload.MonitorActorState;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;

public class GuardianActor extends AbstractBehavior<GuardianCommand>  {

    public static Behavior<GuardianCommand> create(
            ActorRef<RoomDirectoryCommand> roomDirectory,
            ActorRef<GameRoomCoordinatorCommand> gameRoomsCoordinator,
            ClusterSharding clusterSharding,
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
                            clusterSharding,
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
            ClusterSharding clusterSharding,
            MonitorEventBroadcaster monitorEventBroadcaster,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        super(context);

        this.roomDirectory = roomDirectory;
        this.gameRoomsCoordinator = gameRoomsCoordinator;
        this.clusterSharding = clusterSharding;
        this.monitorEventBroadcaster = monitorEventBroadcaster;
        this.chatBlacklistRepository = chatBlacklistRepository;
    }

    private final ActorRef<RoomDirectoryCommand> roomDirectory;
    private final ActorRef<GameRoomCoordinatorCommand> gameRoomsCoordinator;
    private final ClusterSharding clusterSharding;
    private final MonitorEventBroadcaster monitorEventBroadcaster;
    private final ChatBlacklistRepository chatBlacklistRepository;
    private final Map<Long, ActorRef<LocalDirectoryCommand>> roomDirectoryCaches = new ConcurrentHashMap<>();
    private final Map<Long, ActorRef<ClientSessionCommand>> clientSessionProxies = new ConcurrentHashMap<>();

    @Override
    public Receive<GuardianCommand> createReceive() {
        return newReceiveBuilder().onMessage(SpawnClientSession.class, this::onSpawnClientSession)
                                  .onMessage(GetOrCreateSessionProxy.class, this::onGetOrCreateSessionProxy)
                                  .onMessage(GetGameRoomsCoordinator.class, this::onGetGameRoomsCoordinator)
                                  .build();
    }

    private Behavior<GuardianCommand> onSpawnClientSession(SpawnClientSession command) {
        ActorRef<LocalDirectoryCommand> localCache = roomDirectoryCaches.computeIfAbsent(
                command.playerId(),
                this::spawnLocalDirectoryCache
        );
        ActorRef<ClientSessionCommand> clientSessionProxy = clientSessionProxies.computeIfAbsent(
                command.playerId(),
                this::spawnSessionProxy
        );
        ActorRef<ClientSessionCommand> clientSession = getContext().spawn(
                ClientSessionActor.create(
                        command.playerId(),
                        command.clientWebSocketMessageSender(),
                        localCache,
                        chatBlacklistRepository,
                        clientSessionProxy
                ),
                "client-session-" + System.nanoTime() + "-" + command.playerId()
        );

        command.replyTo()
               .tell(new SpawnedClientSession(clientSession, clientSessionProxy));
        return this;
    }

    private Behavior<GuardianCommand> onGetOrCreateSessionProxy(GetOrCreateSessionProxy command) {
        ActorRef<ClientSessionCommand> clientSessionProxy = clientSessionProxies.computeIfAbsent(
                command.playerId(),
                this::spawnSessionProxy
        );
        command.replyTo().tell(clientSessionProxy);
        return this;
    }

    private ActorRef<LocalDirectoryCommand> spawnLocalDirectoryCache(Long playerId) {
        return getContext().spawn(
                RoomDirectorySubscriberActor.create(roomDirectory, monitorEventBroadcaster),
                "room-directory-cache-" + playerId
        );
    }

    private ActorRef<ClientSessionCommand> spawnSessionProxy(Long userId) {
        EntityRef<ClientSessionCommand> entityRef = clusterSharding.entityRefFor(
                ClientSessionGatewayActor.ENTITY_TYPE_KEY,
                String.valueOf(userId)
        );
        return getContext().spawn(
                ClientSessionProxyActor.create(entityRef),
                "client-session-proxy-" + userId
        );
    }

    private Behavior<GuardianCommand> onGetGameRoomsCoordinator(GetGameRoomsCoordinator command) {
        command.replyTo()
               .tell(gameRoomsCoordinator);

        return this;
    }

    public interface GuardianCommand extends CborSerializable { }

    public record SpawnClientSession(
            Long playerId,
            ClientWebSocketMessageSender clientWebSocketMessageSender,
            ActorRef<SpawnedClientSession> replyTo
    ) implements GuardianCommand { }

    public record SpawnedClientSession(
            ActorRef<ClientSessionCommand> clientSession,
            ActorRef<ClientSessionCommand> clientSessionProxy
    ) implements GuardianCommand { }

    public record GetOrCreateSessionProxy(
            Long playerId,
            ActorRef<ActorRef<ClientSessionCommand>> replyTo
    ) implements GuardianCommand { }

    public record GetGameRoomsCoordinator(ActorRef<ActorRef<GameRoomCoordinatorCommand>> replyTo) implements GuardianCommand { }
}
