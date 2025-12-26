package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.directory.RoomDirectoryActor.RoomDirectorySnapshot;
import com.game.contraband.infrastructure.actor.directory.RoomDirectorySync;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LobbyChatCommand;
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
        long roomId = roomSequenceGenerator.nextSequence();
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

    public Behavior<LobbyChatCommand> lobbyChatBehavior(LobbyCreationPlan plan) {
        return LobbyChatActor.create(
                plan.roomId(),
                entityId,
                plan.host().session(),
                plan.host().id(),
                plan.host().chatMessageEventPublisher(),
                chatBlacklistRepository
        );
    }

    public LobbyActorAssembly buildLobbyActor(
            LobbyCreationPlan plan,
            ActorRef<LobbyChatCommand> lobbyChat,
            ActorRef<GameManagerCommand> parent,
            GameLifecycleNotifier lifecycleNotifier
    ) {
        LobbyExternalGateway gateway = new LobbyExternalGateway(
                lobbyChat,
                parent,
                plan.host().chatMessageEventPublisher(),
                lifecycleNotifier.publisher(),
                chatBlacklistRepository
        );
        LobbyChatRelay chatRelay = new LobbyChatRelay(gateway);
        LobbyLifecycleCoordinator lifecycleCoordinator = new LobbyLifecycleCoordinator(plan.lobbyState(), plan.sessionRegistry(), gateway);

        Behavior<LobbyCommand> behavior = LobbyActor.create(
                plan.lobbyState(),
                plan.sessionRegistry(),
                gateway,
                lifecycleCoordinator,
                chatRelay
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

    public static class LobbyCreationPlan {
        private final long roomId;
        private final String lobbyName;
        private final int maxPlayerCount;
        private final HostContext host;
        private final LobbyClientSessionRegistry sessionRegistry;
        private final LobbyRuntimeState lobbyState;

        private LobbyCreationPlan(
                long roomId,
                String lobbyName,
                int maxPlayerCount,
                HostContext host,
                LobbyClientSessionRegistry sessionRegistry,
                LobbyRuntimeState lobbyState
        ) {
            this.roomId = roomId;
            this.lobbyName = lobbyName;
            this.maxPlayerCount = maxPlayerCount;
            this.host = host;
            this.sessionRegistry = sessionRegistry;
            this.lobbyState = lobbyState;
        }

        public long roomId() {
            return roomId;
        }

        public String lobbyName() {
            return lobbyName;
        }

        public int maxPlayerCount() {
            return maxPlayerCount;
        }

        public HostContext host() {
            return host;
        }

        public LobbyClientSessionRegistry sessionRegistry() {
            return sessionRegistry;
        }

        public LobbyRuntimeState lobbyState() {
            return lobbyState;
        }

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
