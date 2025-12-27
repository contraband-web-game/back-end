package com.game.contraband.infrastructure.actor.game.engine.match;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateContrabandGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.PropagateStartGame;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.ContrabandGameProtocol.ContrabandGameCommand;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.ActorContext;

@Getter
public class ClientSessionRegistry {

    public static ClientSessionRegistry create(
            ContrabandGame contrabandGame,
            Map<Long, ActorRef<ClientSessionCommand>> clientSessions,
            ActorContext<ContrabandGameCommand> context,
            Long roomId,
            String entityId
    ) {
        Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> grouped = new EnumMap<>(TeamRole.class);

        grouped.put(TeamRole.SMUGGLER, new HashMap<>());
        grouped.put(TeamRole.INSPECTOR, new HashMap<>());

        PropagateStartGame propagateStartGame = new PropagateStartGame(
                context.getSelf(),
                roomId,
                entityId,
                getAllPlayers(contrabandGame)
        );

        contrabandGame.smugglerPlayers()
                      .forEach(
                              profile -> assignClientSession(
                                      grouped,
                                      TeamRole.SMUGGLER,
                                      profile.getPlayerId(),
                                      clientSessions,
                                      propagateStartGame,
                                      context.getSelf()
                              )
                      );
        contrabandGame.inspectorPlayers()
                      .forEach(
                              profile -> assignClientSession(
                                      grouped,
                                      TeamRole.INSPECTOR,
                                      profile.getPlayerId(),
                                      clientSessions,
                                      propagateStartGame,
                                      context.getSelf()
                              )
                      );

        return new ClientSessionRegistry(clientSessions, grouped);
    }

    private static void assignClientSession(
            Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> grouped,
            TeamRole teamRole,
            Long playerId,
            Map<Long, ActorRef<ClientSessionCommand>> clientSessions,
            PropagateStartGame propagateStartGame,
            ActorRef<ContrabandGameCommand> contrabandGameRef
    ) {
        ActorRef<ClientSessionCommand> clientSession = clientSessions.get(playerId);

        if (clientSession != null) {
            grouped.get(teamRole)
                   .put(playerId, clientSession);

            clientSession.tell(propagateStartGame);
            clientSession.tell(new UpdateContrabandGame(contrabandGameRef));
        }
    }

    private static List<GameStartPlayer> getAllPlayers(ContrabandGame contrabandGame) {
        return Stream.concat(
                             contrabandGame.smugglerPlayers().stream(),
                             contrabandGame.inspectorPlayers().stream()
                     )
                     .map(
                             profile -> {
                                 Player player = contrabandGame.getPlayer(profile.getPlayerId());

                                 return new GameStartPlayer(
                                         profile.getPlayerId(),
                                         profile.getName(),
                                         profile.getTeamRole(),
                                         player.getBalance().getAmount()
                                 );
                             }
                     )
                     .toList();
    }

    private ClientSessionRegistry(
            Map<Long, ActorRef<ClientSessionCommand>> totalSessions,
            Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> sessionsByTeam
    ) {
        this.totalSessions = totalSessions;
        this.sessionsByTeam = sessionsByTeam;
    }

    private final Map<Long, ActorRef<ClientSessionCommand>> totalSessions;
    private final Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> sessionsByTeam;

    public ActorRef<ClientSessionCommand> findInTotalSessions(Long playerId) {
        return totalSessions.get(playerId);
    }

    public ActorRef<ClientSessionCommand> findInTeamSessions(TeamRole teamRole, Long playerId) {
        Map<Long, ActorRef<ClientSessionCommand>> sessions = sessionsByTeam.get(teamRole);

        if (sessions == null) {
            return null;
        }

        return sessions.get(playerId);
    }

    public void tellAll(ClientSessionCommand command) {
        for (ActorRef<ClientSessionCommand> session : totalSessions.values()) {
            session.tell(command);
        }
    }

    public void tellTeam(TeamRole teamRole, ClientSessionCommand command) {
        Map<Long, ActorRef<ClientSessionCommand>> sessions = sessionsByTeam.get(teamRole);

        if (sessions == null) {
            return;
        }
        for (ActorRef<ClientSessionCommand> session : sessions.values()) {
            session.tell(command);
        }
    }

    public void syncGameChatForAll(ActorRef<ContrabandGameChatCommand> gameChat) {
        sessionsByTeam.entrySet().stream()
                      .flatMap(
                              entry -> entry.getValue()
                                            .values()
                                            .stream()
                                            .map(session -> Map.entry(entry.getKey(), session))
                      )
                      .forEach(
                              pair -> pair.getValue()
                                          .tell(new SyncContrabandGameChat(gameChat, pair.getKey()))
                      );
    }
}
