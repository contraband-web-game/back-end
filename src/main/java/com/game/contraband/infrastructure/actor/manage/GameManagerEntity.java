package com.game.contraband.infrastructure.actor.manage;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectoryCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySync;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.SyncPlayerJoined;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyCreationHandler;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyCreationHandler.LobbyActorAssembly;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.actor.sequence.SnowflakeSequenceGenerator;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;

public class GameManagerEntity extends AbstractBehavior<GameManagerCommand> {

    public static final EntityTypeKey<GameManagerCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(GameManagerCommand.class, "game-managers");

    public static Behavior<GameManagerCommand> create(
            String entityId,
            Long numericEntityId,
            ActorRef<GameRoomCoordinatorCommand> coordinator,
            ActorRef<RoomDirectoryCommand> roomDirectory,
            GameLifecycleEventPublisher gameLifecycleEventPublisher,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        return Behaviors.setup(
                context -> {
                    RoomRegistry roomRegistry = new RoomRegistry();
                    SnowflakeSequenceGenerator roomSequenceGenerator = new SnowflakeSequenceGenerator(numericEntityId);
                    CoordinatorGateway coordinatorGateway = new CoordinatorGateway(coordinator);
                    RoomDirectorySync roomDirectorySync = new RoomDirectorySync(roomDirectory);
                    GameLifecycleNotifier gameLifecycleNotifier = new GameLifecycleNotifier(
                            gameLifecycleEventPublisher,
                            entityId
                    );
                    LobbyCreationHandler lobbyCreationHandler = new LobbyCreationHandler(
                            entityId,
                            roomSequenceGenerator,
                            chatBlacklistRepository
                    );

                    return new GameManagerEntity(
                            context,
                            entityId,
                            roomRegistry,
                            coordinatorGateway,
                            roomDirectorySync,
                            gameLifecycleNotifier,
                            lobbyCreationHandler
                    );
                }
        );
    }

    private GameManagerEntity(
            ActorContext<GameManagerCommand> context,
            String entityId,
            RoomRegistry roomRegistry,
            CoordinatorGateway coordinatorGateway,
            RoomDirectorySync roomDirectorySync,
            GameLifecycleNotifier gameLifecycleNotifier,
            LobbyCreationHandler lobbyCreationHandler
    ) {
        super(context);

        this.entityId = entityId;
        this.roomRegistry = roomRegistry;
        this.coordinatorGateway = coordinatorGateway;
        this.roomDirectorySync = roomDirectorySync;
        this.gameLifecycleNotifier = gameLifecycleNotifier;
        this.lobbyCreationHandler = lobbyCreationHandler;
    }

    private final String entityId;
    private final RoomRegistry roomRegistry;
    private final CoordinatorGateway coordinatorGateway;
    private final RoomDirectorySync roomDirectorySync;
    private final GameLifecycleNotifier gameLifecycleNotifier;
    private final LobbyCreationHandler lobbyCreationHandler;

    @Override
    public Receive<GameManagerCommand> createReceive() {
        return newReceiveBuilder().onMessage(CreateLobby.class, this::onCreateLobby)
                                  .onMessage(JoinLobby.class, this::onJoinLobby)
                                  .onMessage(SyncRoomPlayerCount.class, this::onSyncRoomPlayerCount)
                                  .onMessage(SyncRoomStarted.class, this::onSyncRoomStarted)
                                  .onMessage(SyncDeleteLobby.class, this::onSyncDeleteLobby)
                                  .onMessage(SyncEndGame.class, this::onSyncEndGame)
                                  .build();
    }

    private Behavior<GameManagerCommand> onCreateLobby(CreateLobby command) {
        LobbyCreationHandler.LobbyCreationPlan plan = lobbyCreationHandler.prepare(
                command.hostClientSession(),
                command.hostId(),
                command.hostName(),
                command.maxPlayerCount(),
                command.lobbyName(),
                command.chatMessageEventPublisher()
        );
        LobbyActorAssembly lobbyActorAssembly = lobbyCreationHandler.buildLobbyActor(
                plan,
                getContext().getSelf(),
                gameLifecycleNotifier
        );
        ActorRef<LobbyCommand> lobbyActor = getContext().spawn(
                lobbyActorAssembly.behavior(),
                plan.lobbyActorName()
        );

        lobbyCreationHandler.completeCreation(
                plan,
                lobbyActor,
                roomRegistry,
                roomDirectorySync,
                coordinatorGateway,
                gameLifecycleNotifier
        );
        return this;
    }

    private Behavior<GameManagerCommand> onJoinLobby(JoinLobby command) {
        ActorRef<LobbyCommand> lobbyActor = roomRegistry.get(command.roomId());

        if (lobbyActor == null) {
            command.clientSession()
                   .tell(new HandleExceptionMessage(ExceptionCode.GAME_ROOM_NOT_FOUND));
            return this;
        }

        lobbyActor.tell(new SyncPlayerJoined(command.clientSession(), command.playerName(), command.playerId()));
        return this;
    }

    private Behavior<GameManagerCommand> onSyncRoomStarted(SyncRoomStarted command) {
        roomDirectorySync.register(
                new RoomDirectorySnapshot(
                        command.roomId(),
                        command.lobbyName(),
                        command.maxPlayerCount(),
                        command.currentPlayerCount(),
                        entityId,
                        true
                )
        );
        gameLifecycleNotifier.gameStarted(command.roomId());
        return this;
    }

    private Behavior<GameManagerCommand> onSyncEndGame(SyncEndGame command) {
        roomRegistry.remove(command.roomId());

        coordinatorGateway.notifyRoomRemoved(command.roomId());
        roomDirectorySync.remove(command.roomId());
        gameLifecycleNotifier.gameEnded(command.roomId());
        gameLifecycleNotifier.roomRemoved(command.roomId());

        return this;
    }

    private Behavior<GameManagerCommand> onSyncDeleteLobby(SyncDeleteLobby command) {
        roomRegistry.remove(command.roomId());

        coordinatorGateway.notifyRoomRemoved(command.roomId());
        roomDirectorySync.remove(command.roomId());
        gameLifecycleNotifier.roomRemoved(command.roomId());

        return this;
    }

    private Behavior<GameManagerCommand> onSyncRoomPlayerCount(SyncRoomPlayerCount command) {
        RoomDirectorySnapshot summary = new RoomDirectorySnapshot(command.roomId(), command.lobbyName(), command.maxPlayerCount(), command.currentPlayerCount(), entityId, command.gameStarted());
        roomDirectorySync.register(summary);
        return this;
    }

    public interface GameManagerCommand extends CborSerializable { }

    public record CreateLobby(ActorRef<ClientSessionCommand> hostClientSession, Long hostId, String hostName, int maxPlayerCount, String lobbyName, ChatMessageEventPublisher chatMessageEventPublisher) implements GameManagerCommand { }

    public record JoinLobby(ActorRef<ClientSessionCommand> clientSession, Long roomId, Long playerId, String playerName) implements GameManagerCommand { }

    public record SyncDeleteLobby(Long roomId) implements GameManagerCommand { }

    public record SyncEndGame(Long roomId) implements GameManagerCommand { }

    public record SyncRoomPlayerCount(Long roomId, String lobbyName, int maxPlayerCount, int currentPlayerCount, boolean gameStarted) implements GameManagerCommand { }

    public record SyncRoomStarted(Long roomId, String lobbyName, int maxPlayerCount, int currentPlayerCount) implements GameManagerCommand { }
}
