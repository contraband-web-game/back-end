package com.game.contraband.domain.game.round.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.RoundStatus;
import com.game.contraband.domain.game.vo.Money;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundSlotManagerTest {

    @Test
    void 초기_상태에서는_진행_중인_라운드가_존재하지_않는다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();

        // when
        boolean actual = manager.hasRound();

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 게임을_시작하면_슬롯을_진행_중인_라운드_슬롯으로_변경한다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();
        Round round = Round.newRound(1, 1L, 2L);

        // when
        manager.start(round);

        // then
        assertAll(
                () -> assertThat(manager.hasRound()).isTrue(),
                () -> assertThat(manager.currentRound()).contains(round)
        );
    }

    @Test
    void 게임을_시작할_때_비어_있는_라운드를_전달할_수_없다() {
        // given
        RoundSlotManager actual = RoundSlotManager.create();

        // when & then
        assertThatThrownBy(() -> actual.start(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("라운드는 비어 있을 수 없습니다.");
    }

    @Test
    void 이미_시작된_라운드가_있다면_게임을_시작할_수_없다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();
        Round round = Round.newRound(1, 1L, 2L);
        manager.start(round);

        // when & then
        assertThatThrownBy(() -> manager.start(Round.newRound(2, 3L, 4L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이전 라운드가 아직 완료되지 않았습니다.");
    }

    @Test
    void 라운드를_업데이트_한다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();
        Round initialRound = Round.newRound(1, 1L, 2L);
        Round updatedRound = initialRound.declareSmuggleAmount(
                Money.from(1_000),
                Money.from(5_000)
        );

        manager.start(initialRound);

        // when
        manager.update(updatedRound);

        // then
        Optional<Round> actual = manager.currentRound();

        assertAll(
                () -> assertThat(actual).contains(updatedRound),
                () -> assertThat(actual.get().getStatus()).isEqualTo(RoundStatus.SMUGGLE_DECLARED)
        );
    }

    @Test
    void 라운드를_업데이트_할_때_비어_있는_라운드를_전달할_수_없다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();

        manager.start(Round.newRound(1, 1L, 2L));

        // when & then
        assertThatThrownBy(() -> manager.update(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("라운드는 비어 있을 수 없습니다.");
    }

    @Test
    void 게임이_시작되지_않아_라운드가_비어_있다면_라운드를_업데이트를_할_수_없다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();

        // when & then
        assertThatThrownBy(() -> manager.update(Round.newRound(1, 1L, 2L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중인 라운드가 없습니다.");
    }

    @Test
    void 라운드_정보를_초기화_한다() {
        // given
        RoundSlotManager manager = RoundSlotManager.create();
        Round round = Round.newRound(1, 1L, 2L);
        manager.start(round);

        // when
        manager.clear();

        // then
        assertAll(
                () -> assertThat(manager.hasRound()).isFalse(),
                () -> assertThat(manager.currentRound()).isEmpty()
        );
    }
}
