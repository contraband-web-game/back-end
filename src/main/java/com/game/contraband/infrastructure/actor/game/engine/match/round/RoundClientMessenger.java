package com.game.contraband.infrastructure.actor.game.engine.match.round;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateFinishedRound;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartNewRound;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.EndGame;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import java.time.Instant;
import java.util.List;
import org.apache.pekko.actor.typed.ActorRef;

public class RoundClientMessenger {

    private final ClientSessionRegistry registry;
    private final ActorRef<LobbyCommand> parent;
    private final ActorRef<ContrabandGameCommand> facade;
    private final GameLifecycleEventPublisher lifecyclePublisher;
    private final Long roomId;
    private final String entityId;

    public RoundClientMessenger(
            ClientSessionRegistry registry,
            ActorRef<LobbyCommand> parent,
            ActorRef<ContrabandGameCommand> facade,
            GameLifecycleEventPublisher lifecyclePublisher,
            Long roomId,
            String entityId
    ) {
        this.registry = registry;
        this.parent = parent;
        this.facade = facade;
        this.lifecyclePublisher = lifecyclePublisher;
        this.roomId = roomId;
        this.entityId = entityId;
    }

    public void broadcastStartRound(RoundFlowState roundState, Instant startedAt, long durationMillis, long serverNow, long endAt) {
        registry.tellAll(
                new PropagateStartNewRound(
                        roundState.currentRound(),
                        roundState.smugglerId(),
                        roundState.inspectorId(),
                        startedAt.toEpochMilli(),
                        durationMillis,
                        serverNow,
                        endAt
                )
        );
    }

    public void broadcastFinishedRound(PropagateFinishedRound finishedRound) {
        registry.tellAll(finishedRound);
    }

    public void broadcastFinishedGame(PropagateFinishedGame finishedGame) {
        registry.tellAll(finishedGame);
    }

    public void publishGameEnded() {
        if (lifecyclePublisher != null) {
            lifecyclePublisher.publishGameEnded(entityId, roomId);
        }
    }

    public void notifyParentEndGame() {
        parent.tell(new EndGame());
    }

    public void sendStartGameToSession(ActorRef<ClientSessionCommand> targetSession, List<GameStartPlayer> players) {
        targetSession.tell(new PropagateStartGame(facade, roomId, entityId, players));
    }

    public ActorRef<ClientSessionCommand> totalSession(Long playerId) {
        return registry.findInTotalSessions(playerId);
    }

    public ActorRef<ClientSessionCommand> teamSession(TeamRole teamRole, Long playerId) {
        return registry.findInTeamSessions(teamRole, playerId);
    }

    public void tellAll(ClientSessionCommand command) {
        registry.tellAll(command);
    }

    public ActorRef<ContrabandGameCommand> facade() {
        return facade;
    }
}
