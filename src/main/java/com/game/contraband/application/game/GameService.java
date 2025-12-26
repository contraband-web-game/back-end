package com.game.contraband.application.game;

import com.game.contraband.application.game.dto.ActiveGameView;
import com.game.contraband.global.actor.GuardianActor.GetGameRoomsCoordinator;
import com.game.contraband.global.actor.GuardianActor.GuardianCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.QueryActiveGame;
import com.game.contraband.infrastructure.actor.client.SessionOutboundActor.HandleExceptionMessage;
import com.game.contraband.infrastructure.actor.client.service.ClientSessionActorManageService;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.CreateLobby;
import com.game.contraband.infrastructure.actor.manage.GameManagerEntity.GameManagerCommand;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.AllocateEntity;
import com.game.contraband.infrastructure.actor.manage.GameRoomCoordinatorEntity.GameRoomCoordinatorCommand;
import com.game.contraband.infrastructure.websocket.message.ExceptionCode;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final Duration DEFAULT_ASK_TIMEOUT = Duration.ofSeconds(3L);
    private static final Duration DEFAULT_RESUME_TIMEOUT = Duration.ofSeconds(3L);

    private final ClientSessionActorManageService clientSessionActorManageService;
    private final ClusterSharding clusterSharding;
    private final ActorSystem<GuardianCommand> actorSystem;
    private final ChatMessageEventPublisher chatMessageEventPublisher;

    public CompletionStage<Void> createLobby(Long userId, String hostName, int maxPlayerCount, String lobbyName) {
        ActorRef<ClientSessionCommand> hostSession =
                clientSessionActorManageService.findClientSession(userId)
                                               .orElseThrow(
                                                       ClientSessionNotFoundException::new
                                               );
        CompletionStage<ActorRef<GameRoomCoordinatorCommand>> coordinator =
                AskPattern.ask(
                        actorSystem,
                        GetGameRoomsCoordinator::new,
                        DEFAULT_ASK_TIMEOUT,
                        actorSystem.scheduler()
                );

        return coordinator.thenCompose(ref ->
                AskPattern.ask(
                        ref,
                        AllocateEntity::new,
                        DEFAULT_ASK_TIMEOUT,
                        actorSystem.scheduler()
                ).handle((reply, ex) -> {
                    if (ex != null) {
                        hostSession.tell(
                                new HandleExceptionMessage(
                                        ExceptionCode.LOBBY_CREATE_FAILED,
                                        "로비 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
                                )
                        );
                        throw new IllegalStateException("게임 매니저 엔티티 할당 중 오류가 발생했습니다.", ex);
                    }

                    if (reply.isError()) {
                        hostSession.tell(
                                new HandleExceptionMessage(
                                        ExceptionCode.LOBBY_CREATE_FAILED,
                                        "로비 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
                                )
                        );
                        throw new IllegalStateException("게임 매니저 엔티티 할당에 실패했습니다.", reply.getError());
                    }

                    String entityId = reply.getValue().entityId();
                    EntityRef<GameManagerCommand> gameManager = clusterSharding.entityRefFor(
                            GameManagerEntity.ENTITY_TYPE_KEY,
                            entityId
                    );

                    gameManager.tell(
                            new CreateLobby(
                                    hostSession,
                                    userId,
                                    hostName,
                                    maxPlayerCount,
                                    lobbyName,
                                    chatMessageEventPublisher
                            )
                    );
                    return null;
                })
        );
    }

    public CompletionStage<Void> joinLobby(Long userId, String playerName, Long roomId, String entityId) {
        ActorRef<ClientSessionCommand> session =
                clientSessionActorManageService.findClientSession(userId)
                                               .orElseThrow(ClientSessionNotFoundException::new);
        EntityRef<GameManagerCommand> gameManager = clusterSharding.entityRefFor(
                GameManagerEntity.ENTITY_TYPE_KEY,
                entityId
        );

        gameManager.tell(new GameManagerEntity.JoinLobby(session, roomId, userId, playerName));
        return CompletableFuture.completedFuture(null);
    }

    public CompletionStage<ActiveGameView> findActiveGame(Long userId) {
        ActorRef<ClientSessionCommand> session = clientSessionActorManageService.findClientSession(userId)
                                                                                .orElse(null);

        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        return AskPattern.ask(
                session,
                QueryActiveGame::new,
                DEFAULT_RESUME_TIMEOUT,
                actorSystem.scheduler()
        ).thenApply(reply -> reply.isSuccess() ? reply.getValue() : null);
    }

    public static class ClientSessionNotFoundException extends IllegalStateException {

        public ClientSessionNotFoundException() {
            super("지정한 ID의 클라이언트 세션을 찾을 수 없습니다.");
        }
    }
}
