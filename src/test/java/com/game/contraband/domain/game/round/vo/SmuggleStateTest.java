package com.game.contraband.domain.game.round.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SmuggleStateTest {

    @Test
    void 밀수꾼_라운드_정보를_초기화한다() {
        // when
        SmuggleState actual = SmuggleState.initial(1L);

        // then
        assertAll(
                () -> assertThat(actual.getSmugglerId()).isEqualTo(1L),
                () -> assertThat(actual.getAmount()).isEqualTo(Money.ZERO),
                () -> assertThat(actual.isDeclared()).isFalse()
        );
    }

    @Test
    void 밀수꾼이_밀수_금액을_선언한다() {
        // given
        SmuggleState smuggleState = SmuggleState.initial(1L);

        // when
        SmuggleState actual = smuggleState.declare(Money.from(1_000));

        // then
        assertAll(
                () -> assertThat(actual.getSmugglerId()).isEqualTo(1L),
                () -> assertThat(actual.getAmount()).isEqualTo(Money.from(1_000)),
                () -> assertThat(actual.isDeclared()).isTrue()
        );
    }

    @Test
    void 이미_밀수_금액을_선언한_후에는_다시_선언할_수_없다() {
        // given
        SmuggleState smuggleState = SmuggleState.initial(1L)
                                                .declare(Money.from(500));

        // when & then
        assertThatThrownBy(() -> smuggleState.declare(Money.from(300)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 밀수 금액을 선언했습니다.");
    }
}
