package com.game.contraband.infrastructure.actor.game.chat.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.spy.SpyChatBlacklistRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyChatBlacklistListenerTest {

    @Test
    void 차단_여부를_조회한다() {
        // given
        SpyChatBlacklistRepository repository = new SpyChatBlacklistRepository();
        repository.setBlockedReturn(true);
        LobbyChatBlacklistListener listener = new LobbyChatBlacklistListener(repository, id -> { });

        // when
        boolean actual = listener.isBlocked(10L);

        // then
        assertAll(
                () -> assertThat(actual).isTrue(),
                () -> assertThat(repository.lastCheckedPlayerId()).isEqualTo(10L)
        );
    }

    @Test
    void 리스너를_해제한다() {
        // given
        List<Long> notified = new ArrayList<>();
        LongConsumer handler = notified::add;
        SpyChatBlacklistRepository repository = new SpyChatBlacklistRepository();
        LobbyChatBlacklistListener listener = new LobbyChatBlacklistListener(repository, handler);

        // when
        listener.close();

        // then
        assertAll(
                () -> assertThat(repository.registeredHandler()).isEqualTo(handler),
                () -> assertThat(repository.isUnsubscribeInvoked()).isTrue()
        );
    }
}
