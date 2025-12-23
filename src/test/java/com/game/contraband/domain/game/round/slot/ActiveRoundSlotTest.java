package com.game.contraband.domain.game.round.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.vo.Money;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ActiveRoundSlotTest {

    @Test
    void 유효한_라운드로_진행_중인_라운드_슬롯을_생성한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when
        ActiveRoundSlot actual = ActiveRoundSlot.create(round);

        // then
        assertAll(
                () -> assertThat(actual.hasRound()).isTrue(),
                () -> assertThat(actual.currentRound()).contains(round)
        );
    }

    @Test
    void 비어_있는_라운드로는_진행_중인_라운드_슬롯을_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> ActiveRoundSlot.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("라운드는 비어 있을 수 없습니다.");
    }

    @Test
    void 진행_중인_라운드_슬롯은_항상_라운드를_가지고_있다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when
        ActiveRoundSlot actual = ActiveRoundSlot.create(round);

        // then
        assertAll(
                () -> assertThat(actual.hasRound()).isTrue(),
                () -> assertThat(actual.currentRound()).hasValue(round)
        );
    }

    @Test
    void 가지고_있는_라운드를_Optional로_감싸서_반환한다() {
        // given
        Round round = Round.newRound(2, 3L, 4L);
        ActiveRoundSlot slot = ActiveRoundSlot.create(round);

        // when
        Optional<Round> actual = slot.currentRound();

        // then
        assertAll(
                () -> assertThat(actual).isPresent(),
                () -> assertThat(actual.get().getRoundNumber()).isEqualTo(2),
                () -> assertThat(actual.get().getSmuggleAmount()).isEqualTo(Money.ZERO)
        );
    }
}

