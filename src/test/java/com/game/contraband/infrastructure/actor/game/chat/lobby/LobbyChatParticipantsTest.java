package com.game.contraband.infrastructure.actor.game.chat.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import com.game.contraband.infrastructure.actor.client.SessionChatActor.PropagateMaskedChatBatch;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyChatParticipantsTest {

    @Test
    void 참여한_플레이어를_추가한다() {
        // given
        TestContext context = createContext();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> newSession = ActorTestUtils.spawnMonitored(
                context.actorTestKit(),
                ClientSessionCommand.class,
                Behaviors.ignore()
        );

        // when
        context.participants().add(2L, newSession.ref());

        // then
        ActorRef<ClientSessionCommand> actual = context.participants().get(2L);

        assertThat(actual).isEqualTo(newSession.ref());
    }

    @Test
    void 참여한_플레이어를_제거한다() {
        // given
        TestContext context = createContext();

        // when
        ActorRef<ClientSessionCommand> actual = context.participants().remove(1L);

        // then
        assertAll(
                () -> assertThat(actual).isEqualTo(context.host().ref()),
                () -> assertThat(context.participants().get(1L)).isNull()
        );
    }

    @Test
    void 참여한_플레이어에게_특정_동작을_수행한다() {
        // given
        TestContext context = createContext();
        List<ActorRef<ClientSessionCommand>> collected = new ArrayList<>();

        // when
        context.participants().forEach(collected::add);

        // then
        assertThat(collected).containsExactly(context.host().ref());
    }

    @Test
    void 마스킹_메시지를_브로드캐스트한다() {
        // given
        TestContext context = createContext();
        List<Long> messageIds = List.of(1L, 2L);

        // when
        context.participants().broadcastMasked(messageIds, ChatEventType.LOBBY_CHAT);

        // then
        PropagateMaskedChatBatch actual = (PropagateMaskedChatBatch) context.host().monitor().receiveMessage();
        assertAll(
                () -> assertThat(actual.messageIds()).containsExactlyElementsOf(messageIds),
                () -> assertThat(actual.chatEvent()).isEqualTo(ChatEventType.LOBBY_CHAT)
        );
    }

    private TestContext createContext() {
        ActorTestKit kit = ActorTestKit.create();
        ActorTestUtils.MonitoredActor<ClientSessionCommand> host = ActorTestUtils.spawnMonitored(
                kit,
                ClientSessionCommand.class,
                Behaviors.ignore()
        );
        LobbyChatParticipants participants = new LobbyChatParticipants(1L, host.ref());
        return new TestContext(kit, participants, host);
    }

    private record TestContext(
            ActorTestKit kit,
            LobbyChatParticipants participants,
            ActorTestUtils.MonitoredActor<ClientSessionCommand> host
    ) {
        public ActorTestKit actorTestKit() {
            return kit;
        }

        public LobbyChatParticipants participants() {
            return participants;
        }

        public ActorTestUtils.MonitoredActor<ClientSessionCommand> host() {
            return host;
        }
    }
}
