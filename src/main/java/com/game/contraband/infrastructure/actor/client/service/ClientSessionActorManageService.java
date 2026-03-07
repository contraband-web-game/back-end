package com.game.contraband.infrastructure.actor.client.service;

import com.game.contraband.global.actor.GuardianActor.GuardianCommand;
import com.game.contraband.global.actor.GuardianActor.GetOrCreateSessionProxy;
import com.game.contraband.global.actor.GuardianActor.SpawnClientSession;
import com.game.contraband.global.actor.GuardianActor.SpawnedClientSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ReSyncClientSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.RegisterRuntimeSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.TerminateSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UnregisterRuntimeSession;
import com.game.contraband.infrastructure.websocket.ClientWebSocketMessageSender;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
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

    private static final Duration DEFAULT_ASK_TIMEOUT = Duration.ofSeconds(3);

    private final ActorSystem<GuardianCommand> actorSystem;
    private final Map<Long, ActorRef<ClientSessionCommand>> runtimeSessions = new ConcurrentHashMap<>();
    private final Map<Long, ActorRef<ClientSessionCommand>> sessionProxies = new ConcurrentHashMap<>();

    public CompletionStage<BoundClientSession> bindClientSession(
            Long playerId,
            ClientWebSocketMessageSender clientWebSocketMessageSender
    ) {
        return spawnClientSession(playerId, clientWebSocketMessageSender).thenApply(spawned -> {
            ActorRef<ClientSessionCommand> runtimeSession = spawned.clientSession();
            ActorRef<ClientSessionCommand> sessionProxy = spawned.clientSessionProxy();
            sessionProxies.put(playerId, sessionProxy);
            ActorRef<ClientSessionCommand> previous = runtimeSessions.put(playerId, runtimeSession);

            if (previous != null && !previous.equals(runtimeSession)) {
                previous.tell(new TerminateSession());
                sessionProxy.tell(new UnregisterRuntimeSession(previous));
            }

            sessionProxy.tell(new RegisterRuntimeSession(runtimeSession));
            sessionProxy.tell(new ReSyncClientSession(playerId));
            return new BoundClientSession(sessionProxy, runtimeSession);
        });
    }

    public void unbindClientSession(Long playerId, ActorRef<ClientSessionCommand> runtimeSession) {
        boolean removed = runtimeSessions.remove(playerId, runtimeSession);

        if (!removed) {
            return;
        }

        ActorRef<ClientSessionCommand> sessionProxy = sessionProxies.get(playerId);

        if (sessionProxy != null) {
            sessionProxy.tell(new UnregisterRuntimeSession(runtimeSession));
        }
        runtimeSession.tell(new TerminateSession());
    }

    public Optional<ActorRef<ClientSessionCommand>> findClientSession(Long userId) {
        if (!runtimeSessions.containsKey(userId)) {
            return Optional.empty();
        }

        return Optional.ofNullable(sessionProxies.get(userId));
    }

    public CompletionStage<ActorRef<ClientSessionCommand>> findOrCreateClientSession(Long userId) {
        ActorRef<ClientSessionCommand> cached = sessionProxies.get(userId);

        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return AskPattern.ask(
                actorSystem,
                (ActorRef<ActorRef<ClientSessionCommand>> replyTo) -> new GetOrCreateSessionProxy(userId, replyTo),
                DEFAULT_ASK_TIMEOUT,
                actorSystem.scheduler()
        ).thenApply(proxy -> {
            sessionProxies.putIfAbsent(userId, proxy);
            return proxy;
        });
    }

    private CompletionStage<SpawnedClientSession> spawnClientSession(
            Long playerId,
            ClientWebSocketMessageSender clientWebSocketMessageSender
    ) {
        return AskPattern.ask(
                actorSystem,
                (ActorRef<SpawnedClientSession> replyTo) -> new SpawnClientSession(
                        playerId,
                        clientWebSocketMessageSender,
                        replyTo
                ),
                DEFAULT_ASK_TIMEOUT,
                actorSystem.scheduler()
        );
    }

    public record BoundClientSession(
            ActorRef<ClientSessionCommand> sessionRef,
            ActorRef<ClientSessionCommand> runtimeSessionRef
    ) { }
}
