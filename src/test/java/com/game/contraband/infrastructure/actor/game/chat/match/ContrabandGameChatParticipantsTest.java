package com.game.contraband.infrastructure.actor.game.chat.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandGameChatParticipantsTest {

    @Test
    void 팀원_목록을_조회한다() {
        // given
        TestContext context = createContext();

        // when
        Map<Long, ActorRef<ClientSessionCommand>> actual = context.participants().teamMembers(TeamRole.SMUGGLER);

        // then
        assertAll(
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(1L)).isEqualTo(context.smuggler().getRef())
        );
    }

    @Test
    void 클라이언트_세션을_조회한다() {
        // given
        TestContext context = createContext();

        // when
        ActorRef<ClientSessionCommand> actual = context.participants().session(TeamRole.INSPECTOR, 2L);

        // then
        assertThat(actual).isEqualTo(context.inspector().getRef());
    }

    @Test
    void 클라이언트_세션이_존재하는지_확인한다() {
        // given
        TestContext context = createContext();

        // when
        boolean actual = context.participants().hasSession(TeamRole.INSPECTOR, 3L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 특정_팀_클라이언트_세션에게_특정_동작을_수행한다() {
        // given
        TestContext context = createContext();
        List<ActorRef<ClientSessionCommand>> collected = new ArrayList<>();

        // when
        context.participants().forEachTeamMember(TeamRole.INSPECTOR, collected::add);

        // then
        assertThat(collected)
                .containsExactlyInAnyOrder(context.inspector().getRef(), context.inspector2().getRef());
    }

    private TestContext createContext() {
        TestInbox<ClientSessionCommand> smuggler = TestInbox.create();
        TestInbox<ClientSessionCommand> inspector = TestInbox.create();
        TestInbox<ClientSessionCommand> inspector2 = TestInbox.create();

        Map<TeamRole, Map<Long, ActorRef<ClientSessionCommand>>> sessionsByTeam = Map.of(
                TeamRole.SMUGGLER, Map.of(1L, smuggler.getRef()),
                TeamRole.INSPECTOR, Map.of(
                        2L, inspector.getRef(),
                        3L, inspector2.getRef()
                )
        );

        ContrabandGameChatParticipants participants = new ContrabandGameChatParticipants(sessionsByTeam);

        return new TestContext(participants, smuggler, inspector, inspector2);
    }

    private record TestContext(
            ContrabandGameChatParticipants participants,
            TestInbox<ClientSessionCommand> smuggler,
            TestInbox<ClientSessionCommand> inspector,
            TestInbox<ClientSessionCommand> inspector2
    ) { }
}
