package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateSelectionTimer;
import com.game.contraband.infrastructure.actor.game.engine.match.ClientSessionRegistry;
import org.apache.pekko.actor.typed.ActorRef;

public class SelectionClientMessenger {

    private final ClientSessionRegistry registry;

    public SelectionClientMessenger(ClientSessionRegistry registry) {
        this.registry = registry;
    }

    ActorRef<ClientSessionCommand> teamSession(TeamRole teamRole, Long playerId) {
        return registry.findInTeamSessions(teamRole, playerId);
    }

    boolean hasTeamSession(TeamRole teamRole, Long playerId) {
        return registry.findInTeamSessions(teamRole, playerId) != null;
    }

    ActorRef<ClientSessionCommand> totalSession(Long playerId) {
        return registry.findInTotalSessions(playerId);
    }

    void tellTeam(TeamRole teamRole, ClientSessionCommand command) {
        registry.tellTeam(teamRole, command);
    }

    void tellAll(ClientSessionCommand command) {
        registry.tellAll(command);
    }

    void broadcastSelectionTimer(int round, long startedAt, long duration, long serverNow, long endAt) {
        tellAll(new PropagateSelectionTimer(round, startedAt, duration, serverNow, endAt));
    }
}
