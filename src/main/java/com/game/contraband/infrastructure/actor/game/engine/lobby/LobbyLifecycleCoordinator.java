package com.game.contraband.infrastructure.actor.game.engine.lobby;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateWelcomeMessage;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateCreateLobby;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateCreatedLobby;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncDeleteLobby;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncEndGame;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.SyncRoomPlayerCount;
import java.util.List;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.ActorContext;

public class LobbyLifecycleCoordinator {

    private final LobbyRuntimeState lobbyState;
    private final LobbyClientSessionRegistry sessionRegistry;
    private final LobbyExternalGateway externalGateway;

    public LobbyLifecycleCoordinator(
            LobbyRuntimeState lobbyState,
            LobbyClientSessionRegistry sessionRegistry,
            LobbyExternalGateway externalGateway
    ) {
        this.lobbyState = lobbyState;
        this.sessionRegistry = sessionRegistry;
        this.externalGateway = externalGateway;
    }

    public void initializeHost(ActorContext<LobbyActor.LobbyCommand> context, LobbyChatRelay chatRelay) {
        ActorRef<ClientSessionCommand> hostSession = sessionRegistry.get(lobbyState.getHostId());

        if (hostSession == null) {
            return;
        }

        HostInfo hostInfo = resolveHostInfo(lobbyState.findPlayerProfile(lobbyState.getHostId()));

        hostSession.tell(
                new PropagateCreateLobby(
                        context.getSelf(),
                        lobbyState.lobbyMaxPlayerCount(),
                        lobbyState.lobbyName(),
                        hostInfo.role()
                )
        );
        hostSession.tell(new UpdateLobby(context.getSelf()));
        List<LobbyParticipant> participants = lobbyState.lobbyParticipants();
        hostSession.tell(
                new PropagateCreatedLobby(
                        context.getSelf(),
                        lobbyState.getRoomId(),
                        lobbyState.getHostId(),
                        lobbyState.lobbyMaxPlayerCount(),
                        sessionRegistry.size(),
                        lobbyState.lobbyName(),
                        participants
                )
        );
        chatRelay.syncLobbyChat(hostSession);
        hostSession.tell(new PropagateWelcomeMessage(hostInfo.name()));
    }

    public void notifyRoomPlayerCount(boolean gameStarted) {
        externalGateway.notifyParent(
                new SyncRoomPlayerCount(
                        lobbyState.getRoomId(),
                        lobbyState.lobbyName(),
                        lobbyState.lobbyMaxPlayerCount(),
                        sessionRegistry.size(),
                        gameStarted
                )
        );
    }

    public void notifyDeleteLobby() {
        externalGateway.notifyParent(new SyncDeleteLobby(lobbyState.getRoomId()));
    }

    public void notifyEndGame() {
        externalGateway.notifyParent(new SyncEndGame(lobbyState.getRoomId()));
    }

    private HostInfo resolveHostInfo(PlayerProfile hostProfile) {
        if (hostProfile == null) {
            return new HostInfo("", TeamRole.INSPECTOR);
        }
        return new HostInfo(hostProfile.getName(), hostProfile.getTeamRole());
    }

    private record HostInfo(String name, TeamRole role) { }
}
