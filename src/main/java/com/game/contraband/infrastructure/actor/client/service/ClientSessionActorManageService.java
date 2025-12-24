package com.game.contraband.infrastructure.actor.client.service;

import com.game.contraband.global.actor.GuardianActor.GuardianCommand;
import com.game.contraband.global.actor.GuardianActor.SpawnClientSession;
import com.game.contraband.global.actor.GuardianActor.SpawnedClientSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientSessionActorManageService {

    private final ActorSystem<GuardianCommand> actorSystem;
    private final Map<Long, ClientSessionHolder> clientSessions = new ConcurrentHashMap<>();

    public CompletionStage<ActorRef<ClientSessionCommand>> createClientSessionActor(
            Long playerId,
            ClientWebSocketMessageSender clientWebSocketMessageSender
    ) {
        return AskPattern.ask(
                actorSystem,
                (ActorRef<GuardianCommand> replyTo) -> new SpawnClientSession(
                        playerId,
                        clientWebSocketMessageSender,
                        replyTo
                ),
                Duration.ofSeconds(3),
                actorSystem.scheduler()
        ).thenApply(response -> {
            SpawnedClientSession spawned = (SpawnedClientSession) response;
            ClientSessionHolder clientSessionHolder = new ClientSessionHolder();

            clientSessionHolder.clientSession = spawned.clientSession();
            clientSessions.put(playerId, clientSessionHolder);
            return spawned.clientSession();
        });
    }

    public Optional<ActorRef<ClientSessionCommand>> findClientSession(Long userId) {
        ClientSessionHolder clientSessionHolder = clientSessions.get(userId);

        if (clientSessionHolder == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(clientSessionHolder.clientSession);
    }

    private static class ClientSessionHolder {

        private ActorRef<ClientSessionCommand> clientSession;
    }
}
