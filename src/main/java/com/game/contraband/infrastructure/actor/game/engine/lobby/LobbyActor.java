package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.ClearLobbyChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.ClearLobby;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateHostDeletedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateJoinedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateKicked;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateLeftLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateLobbyDeleted;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateOtherPlayerJoinedLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateOtherPlayerKicked;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateOtherPlayerLeftLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateToggleReady;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateToggleTeam;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.JoinMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.KickedMessage;
import com.game.contraband.infrastructure.actor.game.chat.lobby.LobbyChatActor.LeftMessage;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameActor;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncRoomStarted;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.util.Optional;
import java.util.List;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class LobbyActor extends AbstractBehavior<LobbyCommand> {

    public static Behavior<LobbyCommand> create(
            LobbyRuntimeState lobbyState,
            LobbyClientSessionRegistry sessionRegistry,
            LobbyExternalGateway messageEndpoints,
            LobbyLifecycleCoordinator lifecycleCoordinator,
            LobbyChatRelay chatRelay
    ) {
        return Behaviors.setup(
                context -> {
                    lifecycleCoordinator.initializeHost(context, chatRelay);

                    return new LobbyActor(
                            context,
                            lobbyState,
                            sessionRegistry,
                            messageEndpoints,
                            chatRelay,
                            lifecycleCoordinator
                    );
                }
        );
    }

    private LobbyActor(
            ActorContext<LobbyCommand> context,
            LobbyRuntimeState lobbyState,
            LobbyClientSessionRegistry sessionRegistry,
            LobbyExternalGateway messageEndpoints,
            LobbyChatRelay chatRelay,
            LobbyLifecycleCoordinator lifecycleCoordinator
    ) {
        super(context);

        this.lobbyState = lobbyState;
        this.sessionRegistry = sessionRegistry;
        this.messageEndpoints = messageEndpoints;
        this.chatRelay = chatRelay;
        this.lifecycleCoordinator = lifecycleCoordinator;
    }

    private final LobbyRuntimeState lobbyState;
    private final LobbyClientSessionRegistry sessionRegistry;
    private final LobbyExternalGateway messageEndpoints;
    private final LobbyChatRelay chatRelay;
    private final LobbyLifecycleCoordinator lifecycleCoordinator;
    private boolean gameStarted = false;

    @Override
    public Receive<LobbyCommand> createReceive() {
        return newReceiveBuilder().onMessage(SyncPlayerJoined.class, this::onSyncPlayerJoined)
                                  .onMessage(ChangeMaxPlayerCount.class, this::onChangeMaxPlayerCount)
                                  .onMessage(ToggleReady.class, this::onToggleReady)
                                  .onMessage(ToggleTeam.class, this::onToggleTeam)
                                  .onMessage(LeaveLobby.class, this::onLeaveLobby)
                                  .onMessage(KickPlayer.class, this::onKickPlayer)
                                  .onMessage(RequestDeleteLobby.class, this::onDeleteLobby)
                                  .onMessage(StartGame.class, this::onStartGame)
                                  .onMessage(EndGame.class, this::onEndGame)
                                  .onMessage(ReSyncPlayer.class, this::onReSyncPlayer)
                                  .build();
    }

    private Behavior<LobbyCommand> onSyncPlayerJoined(SyncPlayerJoined command) {
        if (lobbyState.cannotAddToLobby()) {
            command.clientSession()
                   .tell(
                           new HandleExceptionMessage(
                                   ExceptionCode.LOBBY_FULL,
                                   "정원이 모두 찼습니다."
                           )
                   );
            return this;
        }

        PlayerProfile playerProfile = addToTeam(command.playerId(), command.playerName());

        if (playerProfile == null) {
            command.clientSession()
                   .tell(
                           new HandleExceptionMessage(
                                   ExceptionCode.LOBBY_FULL,
                                   "정원이 모두 찼습니다."
                           )
                   );

            return this;
        }

        int currentPlayersIncludingJoiner = sessionRegistry.size() + 1;

        sessionRegistry.forEachSession(
                targetClientSession -> targetClientSession.tell(
                        new PropagateOtherPlayerJoinedLobby(
                                playerProfile.getPlayerId(),
                                playerProfile.getName(),
                                playerProfile.getTeamRole(),
                                currentPlayersIncludingJoiner
                        )
                )
        );
        List<LobbyParticipant> lobbyParticipants = getLobbyParticipants();
        command.clientSession()
               .tell(
                       new PropagateJoinedLobby(
                                getContext().getSelf(),
                                lobbyState.getRoomId(),
                                lobbyState.getHostId(),
                                lobbyState.lobbyMaxPlayerCount(),
                                currentPlayersIncludingJoiner,
                                lobbyState.lobbyName(),
                                lobbyParticipants
                       )
               );
        command.clientSession().tell(new UpdateLobby(getContext().getSelf()));
        sessionRegistry.add(command.playerId(), command.clientSession());
        chatRelay.syncLobbyChat(command.clientSession());
        chatRelay.sendToChat(new JoinMessage(command.clientSession(), command.playerId(), command.playerName()));
        return this;
    }

    private List<LobbyParticipant> getLobbyParticipants() {
        return lobbyState.lobbyParticipants();
    }

    private PlayerProfile addToTeam(Long playerId, String playerName) {
        Lobby lobby = lobbyState.getLobby();
        if (lobby.canAddInspector(playerId)) {
            PlayerProfile playerProfile = PlayerProfile.create(playerId, playerName, TeamRole.INSPECTOR);

            lobby.addInspector(playerProfile);
            return playerProfile;
        }
        if (lobby.canAddSmuggler(playerId)) {
            PlayerProfile playerProfile = PlayerProfile.create(playerId, playerName, TeamRole.SMUGGLER);

            lobby.addSmuggler(playerProfile);
            return playerProfile;
        }

        return null;
    }

    private Behavior<LobbyCommand> onChangeMaxPlayerCount(ChangeMaxPlayerCount command) {
        ActorRef<ClientSessionCommand> clientSession = sessionRegistry.get(command.executorId());

        if (clientSession == null) {
            return this;
        }

        if (lobbyState.isNotHost(command.executorId())) {
            clientSession.tell(
                    new HandleExceptionMessage(
                            ExceptionCode.LOBBY_MAX_PLAYER_CHANGE_FORBIDDEN,
                            "정원을 변경할 권한이 없습니다."
                    )
            );
            return this;
        }

        try {
            lobbyState.changeMaxPlayerCount(command.maxPlayerCount(), command.executorId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            clientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(e), e.getMessage()));
            return this;
        }

        List<LobbyParticipant> lobbyParticipants = getLobbyParticipants();
        int currentCount = sessionRegistry.size();

        sessionRegistry.forEachSession(
                target -> target.tell(
                        new PropagateJoinedLobby(
                                getContext().getSelf(),
                                lobbyState.getRoomId(),
                                lobbyState.getHostId(),
                                lobbyState.lobbyMaxPlayerCount(),
                                currentCount,
                                lobbyState.lobbyName(),
                                lobbyParticipants
                        )
                )
        );

        return this;
    }

    private Behavior<LobbyCommand> onToggleReady(ToggleReady command) {
        ActorRef<ClientSessionCommand> clientSession = sessionRegistry.get(command.playerId());

        if (clientSession == null) {
            return this;
        }

        try {
            lobbyState.toggleReady(command.playerId());
        } catch (IllegalArgumentException ex) {
            clientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(ex), ex.getMessage()));
        }

        boolean toggleReadyState = lobbyState.readyStateOf(command.playerId());

        sessionRegistry.forEachSession(
                targetClientSession -> targetClientSession.tell(
                        new PropagateToggleReady(command.playerId(), toggleReadyState)
                )
        );
        return this;
    }

    private Behavior<LobbyCommand> onToggleTeam(ToggleTeam command) {
        ActorRef<ClientSessionCommand> clientSession = sessionRegistry.get(command.playerId());

        if (clientSession == null) {
            return this;
        }

        try {
            lobbyState.toggleTeam(command.playerId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            clientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(ex), ex.getMessage()));
        }

        PlayerProfile playerProfile = lobbyState.findPlayerProfile(command.playerId());

        sessionRegistry.forEachSession(
                targetClientSession -> targetClientSession.tell(
                        new PropagateToggleTeam(
                                command.playerId(),
                                playerProfile.getName(),
                                playerProfile.getTeamRole()
                        )
                )
        );
        return this;
    }

    private Behavior<LobbyCommand> onLeaveLobby(LeaveLobby command) {
        ActorRef<ClientSessionCommand> clientSession = sessionRegistry.get(command.playerId());

        if (clientSession == null) {
            return this;
        }

        try {
            PlayerProfile profile = lobbyState.findPlayerProfile(command.playerId());
            String playerName = profile != null ? profile.getName() : "";

            lobbyState.removePlayer(command.playerId());
            sessionRegistry.remove(command.playerId());
            clientSession.tell(new PropagateLeftLobby());
            clientSession.tell(new ClearLobby());
            clientSession.tell(new ClearLobbyChat());
            messageEndpoints.sendToLobbyChat(new LeftMessage(command.playerId(), playerName));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            clientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(ex), ex.getMessage()));
        }

        sessionRegistry.forEachSession(
                targetClientSession ->
                        targetClientSession.tell(
                                new PropagateOtherPlayerLeftLobby(command.playerId())
                        )
        );
        lifecycleCoordinator.notifyRoomPlayerCount(gameStarted);
        return this;
    }

    private Behavior<LobbyCommand> onKickPlayer(KickPlayer command) {
        ActorRef<ClientSessionCommand> hostClientSession = sessionRegistry.get(command.executorId());

        if (hostClientSession == null) {
            return this;
        }

        attemptKick(command, hostClientSession)
                .flatMap(profile -> Optional.ofNullable(sessionRegistry.remove(command.targetPlayerId()))
                                            .map(session -> new KickContext(profile, session)))
                .ifPresent(
                        kickContext -> {
                            kickContext.session().tell(new PropagateKicked());
                            kickContext.session().tell(new ClearLobby());
                            kickContext.session().tell(new ClearLobbyChat());
                            messageEndpoints.sendToLobbyChat(
                                    new KickedMessage(command.targetPlayerId(), kickContext.profile().getName())
                            );
                            sessionRegistry.forEachSession(
                                    targetClientSession ->
                                            targetClientSession.tell(
                                                    new PropagateOtherPlayerKicked(command.targetPlayerId())
                                            )
                            );
                            lifecycleCoordinator.notifyRoomPlayerCount(gameStarted);
                        }
                );
        return this;
    }

    private Behavior<LobbyCommand> onDeleteLobby(RequestDeleteLobby command) {
        ActorRef<ClientSessionCommand> executorClientSession = sessionRegistry.get(command.executorId());

        if (executorClientSession == null) {
            return this;
        }

        try {
            lobbyState.deleteLobby(command.executorId());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            executorClientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(ex), ex.getMessage()));
        }

        PlayerProfile profile = lobbyState.findPlayerProfile(command.executorId());
        String hostName = profile != null ? profile.getName() : "";

        lifecycleCoordinator.notifyDeleteLobby();
        sessionRegistry.remove(command.executorId());
        executorClientSession.tell(new PropagateHostDeletedLobby());
        executorClientSession.tell(new ClearLobby());
        executorClientSession.tell(new ClearLobbyChat());
        sessionRegistry.forEachSession(
                targetClientSession -> {
                    targetClientSession.tell(new PropagateLobbyDeleted());
                    targetClientSession.tell(new ClearLobby());
                    targetClientSession.tell(new ClearLobbyChat());
                }
        );
        messageEndpoints.sendToLobbyChat(new LeftMessage(command.executorId(), hostName));
        return this;
    }

    private Behavior<LobbyCommand> onStartGame(StartGame command) {
        ActorRef<ClientSessionCommand> executorClientSession = sessionRegistry.get(command.executorId());
        if (executorClientSession == null) {
            return this;
        }

        try {
            ContrabandGame contrabandGame = lobbyState.startGame(command.totalRounds(), command.executorId());

            getContext().spawn(
                    ContrabandGameActor.create(
                            lobbyState.getRoomId(),
                            lobbyState.getEntityId(),
                            contrabandGame,
                            getContext().getSelf(),
                            sessionRegistry.asMapView(),
                            messageEndpoints.chatMessageEventPublisher(),
                            messageEndpoints.gameLifecycleEventPublisher(),
                            messageEndpoints.chatBlacklistRepository()
                    ),
                    "contrabandGame:" + lobbyState.getRoomId()
            );

            gameStarted = true;

            messageEndpoints.publishGameStarted(lobbyState.getEntityId(), lobbyState.getRoomId());

            messageEndpoints.notifyParent(new SyncRoomStarted(lobbyState.getRoomId(), lobbyState.lobbyName(), lobbyState.lobbyMaxPlayerCount(), sessionRegistry.size()));
            sessionRegistry.forEachSession(target -> target.tell(new ClearLobbyChat()));
            chatRelay.stopChat();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            executorClientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(ex), ex.getMessage()));
        }

        return this;
    }

    private ExceptionCode resolveLobbyExceptionCode(Exception ex) {
        String message = ex.getMessage();

        if (message == null) {
            return ExceptionCode.LOBBY_INVALID_OPERATION;
        }
        if (message.contains("두 팀의 인원 수가 같아야")) {
            return ExceptionCode.LOBBY_TEAM_BALANCE_REQUIRED;
        }
        if (message.contains("ready 상태에서는 팀을 변경할 수 없습니다")) {
            return ExceptionCode.LOBBY_READY_STATE_TEAM_CHANGE_FORBIDDEN;
        }

        return ExceptionCode.LOBBY_INVALID_OPERATION;
    }

    public Behavior<LobbyCommand> onEndGame(EndGame command) {
        lifecycleCoordinator.notifyEndGame();

        return Behaviors.stopped();
    }

    private Behavior<LobbyCommand> onReSyncPlayer(ReSyncPlayer command) {
        ActorRef<ClientSessionCommand> targetSession = sessionRegistry.get(command.playerId());

        if (targetSession == null && command.clientSession() != null) {
            targetSession = command.clientSession();
            sessionRegistry.add(command.playerId(), targetSession);
        }

        if (targetSession == null) {
            return this;
        }

        List<LobbyParticipant> lobbyParticipants = getLobbyParticipants();
        targetSession.tell(
                new PropagateJoinedLobby(
                        getContext().getSelf(),
                        lobbyState.getRoomId(),
                        lobbyState.getHostId(),
                        lobbyState.lobbyMaxPlayerCount(),
                        sessionRegistry.size(),
                        lobbyState.lobbyName(),
                        lobbyParticipants
                )
        );
        targetSession.tell(new UpdateLobby(getContext().getSelf()));
        chatRelay.syncLobbyChat(targetSession);
        return this;
    }

    private Optional<PlayerProfile> attemptKick(KickPlayer command, ActorRef<ClientSessionCommand> hostClientSession) {
        try {
            return Optional.ofNullable(lobbyState.kick(command.executorId(), command.targetPlayerId()));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            hostClientSession.tell(new HandleExceptionMessage(resolveLobbyExceptionCode(ex), ex.getMessage()));
            return Optional.empty();
        }
    }

    private record KickContext(PlayerProfile profile, ActorRef<ClientSessionCommand> session) { }

    public interface LobbyCommand extends CborSerializable { }

    public record SyncPlayerJoined(ActorRef<ClientSessionCommand> clientSession, String playerName, Long playerId) implements LobbyCommand { }

    public record ChangeMaxPlayerCount(int maxPlayerCount, Long executorId) implements LobbyCommand { }

    public record ToggleReady(Long playerId) implements LobbyCommand { }

    public record ToggleTeam(Long playerId) implements LobbyCommand { }

    public record LeaveLobby(Long playerId) implements LobbyCommand { }

    public record KickPlayer(Long executorId, Long targetPlayerId) implements LobbyCommand { }

    public record RequestDeleteLobby(Long executorId) implements LobbyCommand { }

    public record StartGame(Long executorId, int totalRounds) implements LobbyCommand { }

    public record ReSyncPlayer(Long playerId, ActorRef<ClientSessionCommand> clientSession) implements LobbyCommand { }

    public record EndGame() implements LobbyCommand { }
}
