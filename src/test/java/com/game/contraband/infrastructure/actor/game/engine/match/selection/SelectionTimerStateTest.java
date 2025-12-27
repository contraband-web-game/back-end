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
    void 선택_타이머를_설정하면_현재_타이머_스냅샷을_제공한다() {
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
    void 기존_타이머가_있으면_새_타이머로_바꾸기_전에_취소한다() {
        // given
        SelectionTimerState state = new SelectionTimerState();
        SpyCancellable oldCancellable = new SpyCancellable();
        state.initSelectionTimeoutCancellable(
                oldCancellable,
                Instant.parse("2024-01-01T00:00:00Z"),
                Duration.ofSeconds(10)
        );

        SpyCancellable newCancellable = new SpyCancellable();
        Instant newStartedAt = Instant.parse("2024-01-01T00:00:30Z");
        Duration newDuration = Duration.ofSeconds(15);

        // when
        state.initSelectionTimeoutCancellable(newCancellable, newStartedAt, newDuration);

        // then
        assertAll(
                () -> assertThat(state.currentSelectionTimer())
                        .isPresent()
                        .get()
                        .satisfies(
                                snapshot -> assertAll(
                                        () -> assertThat(snapshot.startedAt()).isEqualTo(newStartedAt),
                                        () -> assertThat(snapshot.duration()).isEqualTo(newDuration)
                                )
                        ),
                () -> assertThat(oldCancellable.isCancelled()).isTrue(),
                () -> assertThat(newCancellable.isCancelled()).isFalse()
        );
    }

    @Test
    void 선택_타이머를_취소하면_스냅샷이_비워지고_타이머도_취소된다() {
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
