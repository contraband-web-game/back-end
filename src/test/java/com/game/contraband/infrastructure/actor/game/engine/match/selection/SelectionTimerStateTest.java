package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.spy.SpyCancellable;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SelectionTimerStateTest {

    @Test
    void 후보_선정_타이머를_설정하면_현재_타이머_스냅샷을_제공한다() {
        // given
        SelectionTimerState state = new SelectionTimerState();
        Instant startedAt = Instant.parse("2024-01-01T00:00:00Z");
        Duration duration = Duration.ofSeconds(30);
        SpyCancellable cancellable = new SpyCancellable();

        // when
        state.initSelectionTimeoutCancellable(cancellable, startedAt, duration);

        // then
        assertAll(
                () -> assertThat(state.currentSelectionTimer())
                        .isPresent()
                        .get()
                        .satisfies(
                                snapshot ->
                                        assertAll(
                                                () -> assertThat(snapshot.startedAt()).isEqualTo(startedAt),
                                                () -> assertThat(snapshot.duration()).isEqualTo(duration)
                                        )
                        ),
                () -> assertThat(cancellable.isCancelled()).isFalse()
        );
    }

    @Test
    void 후보_선정_타이머를_취소하면_스냅샷이_비워지고_해당_후보_선정_타이머도_취소된다() {
        // given
        SelectionTimerState state = new SelectionTimerState();
        SpyCancellable cancellable = new SpyCancellable();
        state.initSelectionTimeoutCancellable(
                cancellable,
                Instant.parse("2024-01-01T00:01:00Z"),
                Duration.ofSeconds(20)
        );

        // when
        state.cancelSelectionTimeout();

        // then
        assertAll(
                () -> assertThat(cancellable.isCancelled()).isTrue(),
                () -> assertThat(state.currentSelectionTimer()).isEmpty()
        );
    }
}
