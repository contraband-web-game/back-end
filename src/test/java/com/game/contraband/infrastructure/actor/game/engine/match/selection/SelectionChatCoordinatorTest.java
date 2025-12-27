package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.ContrabandGameChatCommand;
import com.game.contraband.infrastructure.actor.game.chat.match.ContrabandGameChatActor.SyncRoundChatId;
import com.game.contraband.infrastructure.actor.utils.ActorTestUtils;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SelectionChatCoordinatorTest {

    private ActorTestKit actorTestKit;

    @BeforeEach
    void setUp() {
        actorTestKit = ActorTestKit.create();
    }

    @AfterEach
    void tearDown() {
        actorTestKit.shutdownTestKit();
    }

    @Test
    void 라운드_채팅_ID를_동기화한다() {
        // given
        Behavior<ContrabandGameChatCommand> emptyChat = Behaviors.empty();
        ActorTestUtils.MonitoredActor<ContrabandGameChatCommand> gameChat =
                ActorTestUtils.spawnMonitored(actorTestKit, ContrabandGameChatCommand.class, emptyChat);
        SelectionChatCoordinator coordinator = new SelectionChatCoordinator(gameChat.ref());

        Long smugglerId = 1L;
        Long inspectorId = 2L;

        // when
        coordinator.syncRoundChatId(smugglerId, inspectorId);

        // then
        SyncRoundChatId actual = gameChat.monitor()
                                         .expectMessageClass(SyncRoundChatId.class);

        assertAll(
                () -> assertThat(actual.smugglerId()).isEqualTo(smugglerId),
                () -> assertThat(actual.inspectorId()).isEqualTo(inspectorId)
        );
    }
}
