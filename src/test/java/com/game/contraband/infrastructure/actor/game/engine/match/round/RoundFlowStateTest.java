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
class RoundFlowStateTest {

    @Test
    void 라운드와_플레이어를_설정하면_현재_라운드_정보가_반영되고_행동이_리셋된다() {
        // given
        RoundFlowState state = new RoundFlowState();

        state.markSmugglerActionDone();
        state.markInspectorActionDone();

        // when
        state.assignRound(10L, 20L, 3);

        // then
        assertAll(
                () -> assertThat(state.currentRound()).isEqualTo(3),
                () -> assertThat(state.smugglerId()).isEqualTo(10L),
                () -> assertThat(state.inspectorId()).isEqualTo(20L),
                () -> assertThat(state.isSmugglerActionNotDone()).isTrue(),
                () -> assertThat(state.isInspectorActionNotDone()).isTrue()
        );
    }

    @Test
    void 현재_라운드와_다른_라운드인지_확인한다() {
        // given
        RoundFlowState state = new RoundFlowState();

        state.assignRound(1L, 2L, 2);

        // when
        boolean actual = state.isDifferentRound(3);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 현재_라운드와_같은_라운드인지_확인한다() {
        // given
        RoundFlowState state = new RoundFlowState();

        state.assignRound(1L, 2L, 2);

        // when
        boolean actual = state.isDifferentRound(2);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 밀수꾼의_행동_완료_여부를_체크한다() {
        // given
        RoundFlowState state = new RoundFlowState();
        state.assignRound(1L, 2L, 1);

        // when
        state.markSmugglerActionDone();

        // then
        assertThat(state.isSmugglerActionNotDone()).isFalse();
    }

    @Test
    void 검사관의_행동_완료_여부를_체크한다() {
        // given
        RoundFlowState state = new RoundFlowState();
        state.assignRound(1L, 2L, 1);

        // when
        state.markInspectorActionDone();

        // then
        assertThat(state.isInspectorActionNotDone()).isFalse();
    }

    @Test
    void 라운드_타임아웃을_설정하고_스냅샷을_조회한다() {
        // given
        RoundFlowState state = new RoundFlowState();
        DummyCancellable cancellable = new DummyCancellable();
        Instant startedAt = Instant.EPOCH;
        Duration duration = Duration.ofSeconds(30);

        // when
        state.initRoundTimeout(cancellable, startedAt, duration);

        // then
        assertThat(state.currentRoundTimer())
                .isPresent()
                .get()
                .satisfies(
                        snapshot -> {
                            assertThat(snapshot.startedAt()).isEqualTo(startedAt);
                            assertThat(snapshot.duration()).isEqualTo(duration);
                        }
                );
    }

    @Test
    void 라운드_타임아웃을_재설정하면_이전_타임아웃이_취소되고_새_타이머가_저장된다() {
        // given
        RoundFlowState state = new RoundFlowState();
        DummyCancellable first = new DummyCancellable();
        DummyCancellable second = new DummyCancellable();

        state.initRoundTimeout(first, Instant.EPOCH, Duration.ofSeconds(10));

        // when
        Instant newStart = Instant.EPOCH.plusSeconds(1);
        Duration newDuration = Duration.ofSeconds(20);
        state.initRoundTimeout(second, newStart, newDuration);

        // then
        assertAll(
                () -> assertThat(first.isCancelled()).isTrue(),
                () -> assertThat(state.currentRoundTimer())
                        .isPresent()
                        .get()
                        .satisfies(
                                snapshot -> {
                                    assertThat(snapshot.startedAt()).isEqualTo(newStart);
                                    assertThat(snapshot.duration()).isEqualTo(newDuration);
                                }
                        )
        );
    }

    @Test
    void 라운드_타임아웃을_취소하면_스냅샷을_초기화하고_타이머가_취소된다() {
        // given
        RoundFlowState state = new RoundFlowState();
        DummyCancellable cancellable = new DummyCancellable();

        state.initRoundTimeout(cancellable, Instant.EPOCH, Duration.ofSeconds(10));

        // when
        state.cancelRoundTimeout();

        // then
        assertAll(
                () -> assertThat(cancellable.isCancelled()).isTrue(),
                () -> assertThat(state.currentRoundTimer()).isEmpty()
        );
    }

    @Test
    void 라운드_종료_시_타이머와_행동_상태를_초기화한다() {
        // given
        RoundFlowState state = new RoundFlowState();
        DummyCancellable cancellable = new DummyCancellable();

        state.assignRound(1L, 2L, 1);
        state.markSmugglerActionDone();
        state.markInspectorActionDone();
        state.initRoundTimeout(cancellable, Instant.EPOCH, Duration.ofSeconds(10));

        // when
        state.resetAfterFinish();

        // then
        assertAll(
                () -> assertThat(state.smugglerId()).isNull(),
                () -> assertThat(state.inspectorId()).isNull(),
                () -> assertThat(state.isSmugglerActionNotDone()).isTrue(),
                () -> assertThat(state.isInspectorActionNotDone()).isTrue(),
                () -> assertThat(state.currentRoundTimer()).isEmpty()
        );
    }
}
