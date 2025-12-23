package com.game.contraband.domain.game.round.vo;

import com.game.contraband.domain.game.vo.Money;
import lombok.Getter;

@Getter
public class SmuggleState {

    public static SmuggleState initial(Long smugglerId) {
        return new SmuggleState(smugglerId, Money.ZERO, false);
    }

    private SmuggleState(Long smugglerId, Money amount, boolean declared) {
        this.smugglerId = smugglerId;
        this.amount = amount;
        this.declared = declared;
    }

    private final Long smugglerId;
    private final Money amount;
    private final boolean declared;

    public SmuggleState declare(Money newAmount) {
        if (declared) {
            throw new IllegalStateException("이미 밀수 금액을 선언했습니다.");
        }
        return new SmuggleState(smugglerId, newAmount, true);
    }

    public boolean isNotDeclared() {
        return !this.declared;
    }
}
