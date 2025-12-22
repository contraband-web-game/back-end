package com.game.contraband.domain.game.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "amount")
public final class Money {

    public static final Money ZERO = new Money(0);

    private static final int START_MONEY_AMOUNT = 3_000;

    public static Money startingAmount() {
        return new Money(START_MONEY_AMOUNT);
    }

    public static Money from(int amount) {
        validateAmount(amount);

        return new Money(amount);
    }

    private static void validateAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0원 이상이어야 합니다.");
        }
    }

    private final int amount;

    private Money(int amount) {
        this.amount = amount;
    }

    public boolean isGreaterThan(Money other) {
        return this.amount > other.amount;
    }

    public boolean isLessThan(Money other) {
        return this.amount < other.amount;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount >= other.amount;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.amount <= other.amount;
    }

    public boolean isZero() {
        return this.amount == 0;
    }

    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money minus(Money other) {
        int result = this.amount - other.amount;

        if (result < 0) {
            throw new IllegalArgumentException("결과 금액이 0원 미만이 될 수 없습니다.");
        }

        return new Money(result);
    }

    public Money multiply(int factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("배수는 양수여야 합니다.");
        }
        return new Money(this.amount * factor);
    }

    public Money half() {
        if (this.amount % 2 != 0) {
            throw new IllegalStateException("2로 나눌 수 없는 금액입니다.");
        }

        return new Money(this.amount / 2);
    }
}
