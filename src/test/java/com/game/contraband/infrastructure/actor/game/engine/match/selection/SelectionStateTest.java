package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.engine.match.selection.dto.SelectionTimerSnapshot;
import com.game.contraband.infrastructure.actor.spy.SpyCancellable;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SelectionStateTest {

    @Test
    void 라운드_상태를_초기화한다() {
        // given
        SelectionState state = new SelectionState();

        // then
        assertAll(
                () -> assertThat(state.currentRound()).isEqualTo(1),
                () -> assertThat(state.smugglerId()).isNull(),
                () -> assertThat(state.inspectorId()).isNull(),
                () -> assertThat(state.isReady()).isFalse(),
                () -> assertThat(state.isRoundNotReady()).isTrue()
        );
    }

    @Test
    void 후보를_등록한다() {
        // given
        SelectionState state = new SelectionState();

        // when
        state.registerSmugglerId(1, 10L);
        state.registerInspectorId(1, 20L);

        // then
        assertAll(
                () -> assertThat(state.smugglerId()).isEqualTo(10L),
                () -> assertThat(state.inspectorId()).isEqualTo(20L),
                () -> assertThat(state.smugglerCandidateId()).isEqualTo(10L),
                () -> assertThat(state.inspectorCandidateId()).isEqualTo(20L),
                () -> assertThat(state.isSmugglerReady()).isTrue(),
                () -> assertThat(state.isInspectorReady()).isTrue(),
                () -> assertThat(state.isReady()).isFalse()
        );
    }

    @Test
    void 후보를_확정한다() {
        // given
        SelectionState state = new SelectionState();
        state.registerSmugglerId(1, 10L);
        state.registerInspectorId(1, 20L);

        // when
        state.fixSmugglerId();
        state.fixInspectorId();

        // then
        assertAll(
                () -> assertThat(state.isSmugglerFixed()).isTrue(),
                () -> assertThat(state.isInspectorFixed()).isTrue(),
                () -> assertThat(state.isReady()).isTrue(),
                () -> assertThat(state.isRoundNotReady()).isFalse()
        );
    }

    @Test
    void 라운드가_1라운드를_진행하는_상황에서_2라운드의_밀수꾼을_미리_등록할_수_없다() {
        // given
        SelectionState state = new SelectionState();

        // when then
        assertThatThrownBy(() -> state.registerSmugglerId(2, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 진행중인 라운드가 아닙니다.");
    }

    @Test
    void 라운드가_1라운드를_진행하는_상황에서_2라운드의_검사관을_미리_등록할_수_없다() {
        // given
        SelectionState state = new SelectionState();

        // when then
        assertThatThrownBy(() -> state.registerInspectorId(2, 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 진행중인 라운드가 아닙니다.");
    }

    @Test
    void 확정된_밀수꾼은_변경할_수_없다() {
        // given
        SelectionState state = new SelectionState();

        state.registerSmugglerId(1, 10L);
        state.fixSmugglerId();

        // when & then
        assertThatThrownBy(() -> state.registerSmugglerId(1, 11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 이번 라운드에 참여할 밀수꾼이 확정되었습니다.");
    }

    @Test
    void 확정된_검사관은_변경할_수_없다() {
        // given
        SelectionState state = new SelectionState();

        state.registerInspectorId(1, 20L);
        state.fixInspectorId();

        // when & then
        assertThatThrownBy(() -> state.registerInspectorId(1, 21L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 이번 라운드에 참여할 검사관이 확정되었습니다.");
    }

    @Test
    void 선발_정보를_확정하면_찬성_정보를_초기화한다() {
        // given
        SelectionState state = new SelectionState();

        state.registerSmugglerId(1, 10L);
        state.registerInspectorId(1, 20L);
        state.toggleSmugglerApproval(1L);
        state.toggleInspectorApproval(2L);

        // when
        state.seed(100L, 200L);

        // then
        assertAll(
                () -> assertThat(state.smugglerId()).isEqualTo(100L),
                () -> assertThat(state.inspectorId()).isEqualTo(200L),
                () -> assertThat(state.isReady()).isTrue(),
                () -> assertThat(state.smugglerApprovalsSnapshot()).isEmpty(),
                () -> assertThat(state.inspectorApprovalsSnapshot()).isEmpty()
        );
    }

    @Test
    void 다음_라운드를_준비하면_상태와_타이머를_초기화한다() {
        // given
        SelectionState state = new SelectionState();

        state.registerSmugglerId(1, 10L);
        state.registerInspectorId(1, 20L);

        SpyCancellable cancellable = new SpyCancellable();
        Instant startedAt = Instant.parse("2024-01-01T00:00:00Z");

        state.initSelectionTimeout(cancellable, startedAt, Duration.ofSeconds(30));
        state.toggleSmugglerApproval(1L);

        // when
        state.prepareNextRound();

        // then
        assertAll(
                () -> assertThat(state.currentRound()).isEqualTo(2),
                () -> assertThat(state.smugglerId()).isNull(),
                () -> assertThat(state.inspectorId()).isNull(),
                () -> assertThat(state.isSmugglerFixed()).isFalse(),
                () -> assertThat(state.isInspectorFixed()).isFalse(),
                () -> assertThat(state.smugglerApprovalsSnapshot()).isEmpty(),
                () -> assertThat(state.inspectorApprovalsSnapshot()).isEmpty(),
                () -> assertThat(state.currentSelectionTimer()).isEmpty(),
                () -> assertThat(cancellable.isCancelled()).isTrue()
        );
    }

    @Test
    void 해당_밀수꾼_후보에_대해_라운드_참여를_찬성한다() {
        // given
        SelectionState state = new SelectionState();
        state.registerSmugglerId(1, 10L);

        // when
        boolean actual = state.toggleSmugglerApproval(1L);

        // then
        assertAll(
                () -> assertThat(actual).isTrue(),
                () -> assertThat(state.smugglerApprovalsSnapshot()).containsExactly(1L),
                () -> assertThat(state.hasEnoughSmugglerApprovals(1)).isTrue()
        );
    }

    @Test
    void 해당_검사관_후보에_대해_라운드_참여를_찬성한다() {
        // given
        SelectionState state = new SelectionState();

        state.registerInspectorId(1, 20L);

        // when
        boolean actual = state.toggleInspectorApproval(2L);

        // then
        assertAll(
                () -> assertThat(actual).isTrue(),
                () -> assertThat(state.inspectorApprovalsSnapshot()).containsExactly(2L),
                () -> assertThat(state.hasEnoughInspectorApprovals(1)).isTrue()
        );
    }

    @Test
    void 해당_밀수꾼_후보에_대해_라운드_참여_찬성을_취소한다() {
        // given
        SelectionState state = new SelectionState();

        state.registerSmugglerId(1, 10L);
        state.toggleSmugglerApproval(1L);

        // when
        boolean removedSmuggler = state.toggleSmugglerApproval(1L);

        // then
        assertAll(
                () -> assertThat(removedSmuggler).isFalse(),
                () -> assertThat(state.smugglerApprovalsSnapshot()).isEmpty(),
                () -> assertThat(state.hasEnoughSmugglerApprovals(1)).isFalse()
        );
    }

    @Test
    void 해당_검사관_후보에_대해_라운드_참여_찬성을_취소한다() {
        // given
        SelectionState state = new SelectionState();

        state.registerInspectorId(1, 20L);
        state.toggleInspectorApproval(2L);

        // when
        boolean removedInspector = state.toggleInspectorApproval(2L);

        // then
        assertAll(
                () -> assertThat(removedInspector).isFalse(),
                () -> assertThat(state.inspectorApprovalsSnapshot()).isEmpty(),
                () -> assertThat(state.hasEnoughInspectorApprovals(1)).isFalse()
        );
    }

    @Test
    void 후보_선정_타이머를_초기화하면_스냅샷을_조회할_수_있다() {
        // given
        SelectionState state = new SelectionState();
        SpyCancellable cancellable = new SpyCancellable();
        Instant startedAt = Instant.parse("2024-01-01T00:00:00Z");
        Duration duration = Duration.ofSeconds(15);

        // when
        state.initSelectionTimeout(cancellable, startedAt, duration);

        // then
        assertThat(state.currentSelectionTimer())
                .isPresent()
                .get()
                .isEqualTo(new SelectionTimerSnapshot(startedAt, duration));
    }

    @Test
    void 후보_선정_타이머를_취소하면_스냅샷이_없어진다() {
        // given
        SelectionState state = new SelectionState();
        SpyCancellable cancellable = new SpyCancellable();
        Instant startedAt = Instant.parse("2024-01-01T00:00:00Z");
        Duration duration = Duration.ofSeconds(15);

        state.initSelectionTimeout(cancellable, startedAt, duration);

        // when
        state.cancelSelectionTimeout();

        // then
        assertAll(
                () -> assertThat(state.currentSelectionTimer()).isEmpty(),
                () -> assertThat(cancellable.isCancelled()).isTrue()
        );
    }
}
