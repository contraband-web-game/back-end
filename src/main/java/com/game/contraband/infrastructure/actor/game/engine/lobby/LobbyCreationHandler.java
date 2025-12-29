package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySync;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.manage.CoordinatorGateway;
import com.game.contraband.infrastructure.actor.manage.GameLifecycleNotifier;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.manage.RoomRegistry;
import com.game.contraband.infrastructure.actor.sequence.SnowflakeSequenceGenerator;
import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;

public class LobbyCreationHandler {

    private final String entityId;
    private final SnowflakeSequenceGenerator roomSequenceGenerator;
    private final ChatBlacklistRepository chatBlacklistRepository;

    public LobbyCreationHandler(
            String entityId,
            SnowflakeSequenceGenerator roomSequenceGenerator,
            ChatBlacklistRepository chatBlacklistRepository
    ) {
        this.entityId = entityId;
        this.roomSequenceGenerator = roomSequenceGenerator;
        this.chatBlacklistRepository = chatBlacklistRepository;
    }

    public LobbyCreationPlan prepare(
            ActorRef<ClientSessionCommand> hostSession,
            Long hostId,
            String hostName,
            int maxPlayerCount,
            String lobbyName,
            ChatMessageEventPublisher chatMessageEventPublisher
    ) {
        Long roomId = roomSequenceGenerator.nextSequence();
        String resolvedLobbyName = resolveLobbyName(lobbyName, roomId);
        PlayerProfile hostProfile = PlayerProfile.create(hostId, hostName, TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(roomId, resolvedLobbyName, hostProfile, maxPlayerCount);
        Map<Long, ActorRef<ClientSessionCommand>> players = new HashMap<>();
        players.put(hostId, hostSession);

        LobbyClientSessionRegistry sessionRegistry = new LobbyClientSessionRegistry(players);
        LobbyRuntimeState lobbyState = new LobbyRuntimeState(roomId, hostId, entityId, lobby);
        HostContext hostContext = new HostContext(
                hostId,
                hostSession,
                chatMessageEventPublisher
        );

        return new LobbyCreationPlan(
                roomId,
                resolvedLobbyName,
                maxPlayerCount,
                hostContext,
                sessionRegistry,
                lobbyState
        );
    }

    private String resolveLobbyName(String lobbyName, long roomId) {
        if (lobbyName == null || lobbyName.isBlank()) {
            return "게임방" + roomId;
        }
        return lobbyName;
    }

    public LobbyActorAssembly buildLobbyActor(
            LobbyCreationPlan plan,
            ActorRef<GameManagerCommand> parent,
            GameLifecycleNotifier lifecycleNotifier
    ) {
        Behavior<LobbyCommand> behavior = LobbyActor.create(
                plan.lobbyState(),
                plan.sessionRegistry(),
                parent,
                plan.host().chatMessageEventPublisher(),
                lifecycleNotifier.publisher(),
                chatBlacklistRepository,
                plan.lobbyChatActorName()
        );

        return new LobbyActorAssembly(behavior);
    }

    public void completeCreation(
            LobbyCreationPlan plan,
            ActorRef<LobbyCommand> lobbyActor,
            RoomRegistry roomRegistry,
            RoomDirectorySync roomDirectorySync,
            CoordinatorGateway coordinatorGateway,
            GameLifecycleNotifier lifecycleNotifier
    ) {
        roomRegistry.add(plan.roomId(), lobbyActor);
        roomDirectorySync.register(
                new RoomDirectorySnapshot(
                        plan.roomId(),
                        plan.lobbyName(),
                        plan.maxPlayerCount(),
                        plan.sessionRegistry().size(),
                        entityId,
                        false
                )
        );
        coordinatorGateway.registerRoom(plan.roomId(), entityId);
        lifecycleNotifier.roomCreated(plan.roomId());
    }

    public record LobbyCreationPlan(
            Long roomId,
            String lobbyName,
            int maxPlayerCount,
            HostContext host,
            LobbyClientSessionRegistry sessionRegistry,
            LobbyRuntimeState lobbyState
    ) {
        public String lobbyActorName() {
            return "lobby:" + roomId;
        }

        public String lobbyChatActorName() {
            return "lobby-chat-" + roomId;
        }
    }

    public record HostContext(
            Long id,
            ActorRef<ClientSessionCommand> session,
            ChatMessageEventPublisher chatMessageEventPublisher
    ) { }

    public record LobbyActorAssembly(Behavior<LobbyCommand> behavior) { }
}
