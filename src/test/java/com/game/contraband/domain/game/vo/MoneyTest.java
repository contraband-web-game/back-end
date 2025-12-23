package com.game.contraband.domain.game.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MoneyTest {

    @Test
    void 유효한_금액을_초기화_한다() {
        // when
        Money actual = Money.from(1_000);

        // then
        assertThat(actual.getAmount()).isEqualTo(1_000);
    }

    @Test
    void 음수_금액은_초기화_할_수_없다() {
        // when & then
        assertThatThrownBy(() -> Money.from(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 0원 이상이어야 합니다.");
    }

    @Test
    void 게임_초기_금액으로_3천원을_초기화_한다() {
        // when
        Money actual = Money.startingAmount();

        // then
        assertThat(actual.getAmount()).isEqualTo(3_000);
    }

    @Test
    void 금액이_0원인_정적_변수를_조회한다() {
        // when
        Money actual = Money.ZERO;

        // then
        assertThat(actual.getAmount()).isZero();
    }

    @Test
    void 금액이_더_큰지_비교한다() {
        // given
        Money money1 = Money.from(2_000);
        Money money2 = Money.from(1_000);

        // when
        boolean actual = money1.isGreaterThan(money2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 금액이_더_크지_않은지_비교한다() {
        // given
        Money money1 = Money.from(1_000);
        Money money2 = Money.from(2_000);

        // when
        boolean actual = money1.isGreaterThan(money2);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 금액이_더_작은지_비교한다() {
        // given
        Money money1 = Money.from(1_000);
        Money money2 = Money.from(2_000);

        // when
        boolean actual = money1.isLessThan(money2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 금액이_더_작지_않은지_비교한다() {
        // given
        Money money1 = Money.from(2_000);
        Money money2 = Money.from(1_000);

        // when
        boolean actual = money1.isLessThan(money2);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 금액이_크거나_같은지_비교한다() {
        // given
        Money money1 = Money.from(2_000);
        Money money2 = Money.from(1_000);

        // when
        boolean actual = money1.isGreaterThanOrEqual(money2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 금액이_크거나_같지_않은지_비교한다() {
        // given
        Money money1 = Money.from(500);
        Money money2 = Money.from(1_000);

        // when
        boolean actual = money1.isGreaterThanOrEqual(money2);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 금액이_작거나_같은지_비교한다() {
        // given
        Money money1 = Money.from(500);
        Money money2 = Money.from(1_000);

        // when
        boolean actual = money1.isLessThanOrEqual(money2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 금액이_작거나_같지_않은지_비교한다() {
        // given
        Money money1 = Money.from(2_000);
        Money money2 = Money.from(1_000);

        // when
        boolean actual = money1.isLessThanOrEqual(money2);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 금액이_0원인지_확인한다() {
        // given
        Money money = Money.ZERO;

        // when
        boolean actual = money.isZero();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 금액이_0원이_아닌지_확인한다() {
        // given
        Money money = Money.from(1_000);

        // when
        boolean actual = money.isZero();

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 두_금액을_더한다() {
        // given
        Money money1 = Money.from(1_000);
        Money money2 = Money.from(2_000);

        // when
        Money actual = money1.plus(money2);

        // then
        assertThat(actual.getAmount()).isEqualTo(3_000);
    }

    @Test
    void 두_금액을_뺀다() {
        // given
        Money money1 = Money.from(5_000);
        Money money2 = Money.from(2_000);

        // when
        Money actual = money1.minus(money2);

        // then
        assertThat(actual.getAmount()).isEqualTo(3_000);
    }

    @Test
    void 뺄셈_결과가_음수가_되는_금액_계산은_할_수_없다() {
        // given
        Money money1 = Money.from(1_000);
        Money money2 = Money.from(2_000);

        // when & then
        assertThatThrownBy(() -> money1.minus(money2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결과 금액이 0원 미만이 될 수 없습니다.");
    }

    @Test
    void 금액에_양수를_곱한다() {
        // given
        Money money = Money.from(1_000);

        // when
        Money actual = money.multiply(3);

        // then
        assertThat(actual.getAmount()).isEqualTo(3_000);
    }

    @Test
    void 금액에_0을_곱할_수_없다() {
        // given
        Money money = Money.from(1_000);

        // when & then
        assertThatThrownBy(() -> money.multiply(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("배수는 양수여야 합니다.");
    }

    @Test
    void 금액에_음수를_곱할_수_없다() {
        // given
        Money money = Money.from(1_000);

        // when & then
        assertThatThrownBy(() -> money.multiply(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("배수는 양수여야 합니다.");
    }

    @Test
    void 짝수_금액을_반으로_나눈다() {
        // given
        Money money = Money.from(2_000);

        // when
        Money actual = money.half();

        // then
        assertThat(actual.getAmount()).isEqualTo(1_000);
    }

    @Test
    void 홀수_금액을_반으로_나눌_수_없다() {
        // given
        Money money = Money.from(1_001);

        // when & then
        assertThatThrownBy(() -> money.half())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("2로 나눌 수 없는 금액입니다.");
    }
}
