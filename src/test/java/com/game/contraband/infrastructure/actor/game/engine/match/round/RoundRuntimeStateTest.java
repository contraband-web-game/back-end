package com.game.contraband.infrastructure.actor.game.engine.match.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.dummy.DummyCancellable;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundRuntimeStateTest {

    @Test
    void 밀수꾼과_검사관_행동이_미완료_상태인_라운드_상태_추적기를_초기화한다() {
        // when
        RoundRuntimeState state = new RoundRuntimeState();

        // then
        assertAll(
                () -> assertThat(state.isInspectorActionNotDone()).isTrue(),
                () -> assertThat(state.isSmugglerActionNotDone()).isTrue()
        );
    }

    @Test
    void 밀수꾼과_검사관의_행동이_결정된_후_라운드_상태_추적기를_초기화한다() {
        // given
        RoundRuntimeState state = new RoundRuntimeState();

        state.markSmugglerActionDone();
        state.markInspectorActionDone();

        // when
        state.resetActions();

        // then
        assertAll(
                () -> assertThat(state.isSmugglerActionNotDone()).isTrue(),
                () -> assertThat(state.isInspectorActionNotDone()).isTrue()
        );
    }

    @Test
    void 라운드_타이머를_설정하면_스냅샷을_조회할_수_있다() {
        // given
        RoundRuntimeState state = new RoundRuntimeState();
        Instant startedAt = Instant.EPOCH;
        Duration duration = Duration.ofSeconds(30);
        DummyCancellable cancellable = new DummyCancellable();

        // when
        state.initRoundTimeoutCancellable(cancellable, startedAt, duration);

        // then
        assertThat(state.currentRoundTimer())
                .isPresent()
                .get()
                .satisfies(snapshot -> {
                    assertThat(snapshot.startedAt()).isEqualTo(startedAt);
                    assertThat(snapshot.duration()).isEqualTo(duration);
                });
    }

    @Test
    void 라운드_타이머를_다시_설정하면_이전_타이머를_취소한다() {
        // given
        RoundRuntimeState state = new RoundRuntimeState();
        DummyCancellable first = new DummyCancellable();
        DummyCancellable second = new DummyCancellable();

        state.initRoundTimeoutCancellable(first, Instant.EPOCH, Duration.ofSeconds(10));

        // when
        Instant newStartedAt = Instant.EPOCH.plusSeconds(1);
        Duration newDuration = Duration.ofSeconds(20);
        state.initRoundTimeoutCancellable(second, newStartedAt, newDuration);

        // then
        assertAll(
                () -> assertThat(first.isCancelled()).isTrue(),
                () -> assertThat(state.currentRoundTimer())
                        .isPresent()
                        .get()
                        .satisfies(
                                snapshot -> {
                                    assertThat(snapshot.startedAt()).isEqualTo(newStartedAt);
                                    assertThat(snapshot.duration()).isEqualTo(newDuration);
                                }
                        )
        );
    }

    @Test
    void 라운드_타이머를_취소하면_스냅샷이_존재하지_않고_타이머도_취소된다() {
        // given
        RoundRuntimeState state = new RoundRuntimeState();
        DummyCancellable cancellable = new DummyCancellable();
        state.initRoundTimeoutCancellable(cancellable, Instant.EPOCH, Duration.ofSeconds(10));

        // when
        state.cancelRoundTimeout();

        // then
        assertAll(
                () -> assertThat(cancellable.isCancelled()).isTrue(),
                () -> assertThat(state.currentRoundTimer()).isEmpty()
        );
    }
}
