package com.game.contraband.infrastructure.actor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.application.game.dto.ActiveGameView;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.QueryActiveGame;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ReSyncClientSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.RegisterRuntimeSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.TerminateSession;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.UpdateActiveGame;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.SyncContrabandGameChat;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.UpdateLobby;
import com.game.contraband.infrastructure.actor.client.SessionInboundActor.RequestToggleReady;
import com.game.contraband.infrastructure.actor.game.chat.GameChatProtocol.GameChatCommand;
import com.game.contraband.infrastructure.actor.game.engine.lobby.LobbyActor.LobbyCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.cluster.typed.Cluster;
import org.apache.pekko.cluster.typed.Join;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;
import org.apache.pekko.pattern.StatusReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientSessionGatewayActorIntegrationTest {

    private ActorTestKit actorTestKit;
    private ClusterSharding clusterSharding;

    @BeforeEach
    void setUp() {
        Config config = ConfigFactory.parseString(
                "pekko.actor.provider = cluster\n"
                        + "pekko.remote.artery.canonical.hostname = \"127.0.0.1\"\n"
                        + "pekko.remote.artery.canonical.port = 0\n"
                        + "pekko.cluster.seed-nodes = []\n"
                        + "pekko.cluster.jmx.multi-mbeans-in-same-jvm = on\n"
        );
        actorTestKit = ActorTestKit.create(config);

        Cluster cluster = Cluster.get(actorTestKit.system());
        cluster.manager().tell(Join.create(cluster.selfMember().address()));

        clusterSharding = ClusterSharding.get(actorTestKit.system());
        clusterSharding.init(
                Entity.of(
                        ClientSessionGatewayActor.ENTITY_TYPE_KEY,
                        entityContext -> ClientSessionGatewayActor.create()
                )
        );
    }

    @AfterEach
    void tearDown() {
        actorTestKit.shutdownTestKit();
    }

    @Test
    void 샤딩_게이트웨이가_런타임_세션으로_메시지를_중계하고_진행중_게임을_응답한다() {
        // given
        TestProbe<ClientSessionCommand> runtimeSession = actorTestKit.createTestProbe();
        TestProbe<StatusReply<ActiveGameView>> reply = actorTestKit.createTestProbe();
        EntityRef<ClientSessionCommand> entityRef =
                clusterSharding.entityRefFor(ClientSessionGatewayActor.ENTITY_TYPE_KEY, "100");
        ActorRef<ClientSessionCommand> proxy = actorTestKit.spawn(ClientSessionProxyActor.create(entityRef));

        // when
        proxy.tell(new RegisterRuntimeSession(runtimeSession.ref()));
        proxy.tell(new UpdateActiveGame(33L, "entity-33"));
        proxy.tell(new RequestToggleReady(100L));
        proxy.tell(new QueryActiveGame(reply.ref()));

        // then
        UpdateActiveGame update = (UpdateActiveGame) runtimeSession.receiveMessage();
        RequestToggleReady forwarded = (RequestToggleReady) runtimeSession.receiveMessage();
        StatusReply<ActiveGameView> actual = reply.receiveMessage();

        assertAll(
                () -> assertThat(update.roomId()).isEqualTo(33L),
                () -> assertThat(update.entityId()).isEqualTo("entity-33"),
                () -> assertThat(forwarded.playerId()).isEqualTo(100L),
                () -> assertThat(actual.getValue().roomId()).isEqualTo(33L),
                () -> assertThat(actual.getValue().entityId()).isEqualTo("entity-33")
        );
    }

    @Test
    void 새_런타임_세션이_등록되면_기존_세션을_종료하고_최신_상태를_동기화한다() {
        // given
        TestProbe<ClientSessionCommand> oldRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<ClientSessionCommand> newRuntimeSession = actorTestKit.createTestProbe();
        EntityRef<ClientSessionCommand> entityRef =
                clusterSharding.entityRefFor(ClientSessionGatewayActor.ENTITY_TYPE_KEY, "200");
        ActorRef<ClientSessionCommand> proxy = actorTestKit.spawn(ClientSessionProxyActor.create(entityRef));

        proxy.tell(new RegisterRuntimeSession(oldRuntimeSession.ref()));
        proxy.tell(new UpdateActiveGame(99L, "entity-99"));
        oldRuntimeSession.receiveMessage();

        // when
        proxy.tell(new RegisterRuntimeSession(newRuntimeSession.ref()));

        // then
        TerminateSession terminateSession = (TerminateSession) oldRuntimeSession.receiveMessage();
        UpdateActiveGame synchronizedState = (UpdateActiveGame) newRuntimeSession.receiveMessage();

        assertAll(
                () -> assertThat(terminateSession).isNotNull(),
                () -> assertThat(synchronizedState.roomId()).isEqualTo(99L),
                () -> assertThat(synchronizedState.entityId()).isEqualTo("entity-99")
        );
    }

    @Test
    void 새_런타임_세션_등록시_로비_참조를_복구해_나가기_요청을_처리할_수_있다() {
        // given
        TestProbe<ClientSessionCommand> oldRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<ClientSessionCommand> newRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<LobbyCommand> lobby = actorTestKit.createTestProbe();
        EntityRef<ClientSessionCommand> entityRef =
                clusterSharding.entityRefFor(ClientSessionGatewayActor.ENTITY_TYPE_KEY, "300");
        ActorRef<ClientSessionCommand> proxy = actorTestKit.spawn(ClientSessionProxyActor.create(entityRef));

        proxy.tell(new RegisterRuntimeSession(oldRuntimeSession.ref()));
        proxy.tell(new UpdateLobby(lobby.ref()));

        UpdateLobby oldRuntimeUpdate = (UpdateLobby) oldRuntimeSession.receiveMessage();

        // when
        proxy.tell(new RegisterRuntimeSession(newRuntimeSession.ref()));

        // then
        TerminateSession terminateSession = (TerminateSession) oldRuntimeSession.receiveMessage();
        UpdateLobby newRuntimeUpdate = (UpdateLobby) newRuntimeSession.receiveMessage();

        assertAll(
                () -> assertThat(terminateSession).isNotNull(),
                () -> assertThat(oldRuntimeUpdate.lobby()).isEqualTo(lobby.ref()),
                () -> assertThat(newRuntimeUpdate.lobby()).isEqualTo(lobby.ref())
        );
    }

    @Test
    void 새_런타임_세션_등록시_게임_채팅_참조를_복구해_게임중_채팅을_전송할_수_있다() {
        // given
        TestProbe<ClientSessionCommand> oldRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<ClientSessionCommand> newRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<GameChatCommand> gameChat = actorTestKit.createTestProbe();
        EntityRef<ClientSessionCommand> entityRef =
                clusterSharding.entityRefFor(ClientSessionGatewayActor.ENTITY_TYPE_KEY, "400");
        ActorRef<ClientSessionCommand> proxy = actorTestKit.spawn(ClientSessionProxyActor.create(entityRef));

        proxy.tell(new RegisterRuntimeSession(oldRuntimeSession.ref()));
        proxy.tell(new SyncContrabandGameChat(gameChat.ref(), TeamRole.SMUGGLER));

        SyncContrabandGameChat oldRuntimeChatSync = (SyncContrabandGameChat) oldRuntimeSession.receiveMessage();

        // when
        proxy.tell(new RegisterRuntimeSession(newRuntimeSession.ref()));

        // then
        TerminateSession terminateSession = (TerminateSession) oldRuntimeSession.receiveMessage();
        SyncContrabandGameChat newRuntimeChatSync = (SyncContrabandGameChat) newRuntimeSession.receiveMessage();

        assertAll(
                () -> assertThat(terminateSession).isNotNull(),
                () -> assertThat(oldRuntimeChatSync.chat()).isEqualTo(gameChat.ref()),
                () -> assertThat(oldRuntimeChatSync.teamRole()).isEqualTo(TeamRole.SMUGGLER),
                () -> assertThat(newRuntimeChatSync.chat()).isEqualTo(gameChat.ref()),
                () -> assertThat(newRuntimeChatSync.teamRole()).isEqualTo(TeamRole.SMUGGLER)
        );
    }

    @Test
    void 런타임_재등록_직후_재동기화_요청을_보내면_복구된_상태_이후에_처리된다() {
        // given
        TestProbe<ClientSessionCommand> oldRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<ClientSessionCommand> newRuntimeSession = actorTestKit.createTestProbe();
        TestProbe<LobbyCommand> lobby = actorTestKit.createTestProbe();
        EntityRef<ClientSessionCommand> entityRef =
                clusterSharding.entityRefFor(ClientSessionGatewayActor.ENTITY_TYPE_KEY, "500");
        ActorRef<ClientSessionCommand> proxy = actorTestKit.spawn(ClientSessionProxyActor.create(entityRef));

        proxy.tell(new RegisterRuntimeSession(oldRuntimeSession.ref()));
        proxy.tell(new UpdateLobby(lobby.ref()));
        oldRuntimeSession.receiveMessage();

        // when
        proxy.tell(new RegisterRuntimeSession(newRuntimeSession.ref()));
        proxy.tell(new ReSyncClientSession(500L));

        // then
        TerminateSession terminateSession = (TerminateSession) oldRuntimeSession.receiveMessage();
        UpdateLobby restoredLobby = (UpdateLobby) newRuntimeSession.receiveMessage();
        ReSyncClientSession reSyncClientSession = (ReSyncClientSession) newRuntimeSession.receiveMessage();

        assertAll(
                () -> assertThat(terminateSession).isNotNull(),
                () -> assertThat(restoredLobby.lobby()).isEqualTo(lobby.ref()),
                () -> assertThat(reSyncClientSession.playerId()).isEqualTo(500L)
        );
    }
}
