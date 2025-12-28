package com.game.contraband.infrastructure.actor.game.chat.match;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.pekko.actor.typed.ActorRef;

public class ContrabandGameChatParticipants {

    private final Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> sessionsByTeam;

    public ContrabandGameChatParticipants(Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> sessionsByTeam) {
        Objects.requireNonNull(sessionsByTeam, "sessionsByTeam");
        this.sessionsByTeam = new EnumMap<>(TeamRole.class);
        sessionsByTeam.forEach((teamRole, sessions) -> this.sessionsByTeam.put(teamRole, Map.copyOf(sessions)));
    }

    public Map<Long, ActorRef<ClientSessionCommand>> teamMembers(TeamRole teamRole) {
        return sessionsByTeam.getOrDefault(teamRole, Collections.emptyMap());
    }

    public ActorRef<ClientSessionCommand> session(TeamRole teamRole, Long playerId) {
        return teamMembers(teamRole).get(playerId);
    }

    public boolean hasSession(TeamRole teamRole, Long playerId) {
        return session(teamRole, playerId) != null;
    }

    public void forEachTeamMember(TeamRole teamRole, Consumer<ActorRef<ClientSessionCommand>> action) {
        teamMembers(teamRole).values()
                             .forEach(action);
    }
}
