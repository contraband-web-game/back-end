package com.game.contraband.domain.game.round.settle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PassSettlementRuleTest {

    @Test
    void 검사관이_검문을_하지_않은_경우_밀수_금액이_0원이면_검사관과_밀수꾼_모두의_잔액이_변경되지_않는다() {
        // given
        int smugglerAmount = 3_000;
        int inspectorAmount = 3_000;

        Player smuggler = Player.create(1L, "밀수꾼", TeamRole.SMUGGLER, Money.from(smugglerAmount));
        Player inspector = Player.create(2L, "검문관", TeamRole.INSPECTOR, Money.from(inspectorAmount));
        PassSettlementRule rule = new PassSettlementRule();

        // when
        Money smuggleAmount = Money.ZERO;
        Money claimAmount = Money.ZERO;

        RoundSettlement actual = rule.settle(smuggler, inspector, smuggleAmount, claimAmount);

        // then
        // 검문을 하지 않은 경우 검사관은 아무런 이득도, 손해도 얻지 않는다
        // 밀수꾼은 0원을 밀수했으므로 아무런 이득도, 손해도 얻지 않는다
        // 검사관의 잔액은 3_000원을 그대로 유지한다
        // 밀수꾼의 잔액은 3_000원을 그대로 유지한다
        assertAll(
                () -> assertThat(actual.smuggler().getBalance()).isEqualTo(Money.from(3_000)),
                () -> assertThat(actual.inspector().getBalance()).isEqualTo(Money.from(3_000)),
                () -> assertThat(actual.outcomeType()).isEqualTo(RoundOutcomeType.PASS)
        );
    }

    @Test
    void 검사관이_검문을_하지_않은_경우_밀수_금액이_1_000원이면_밀수꾼이_밀수_금액만큼_획득한다() {
        // given
        int smugglerAmount = 3_000;
        int inspectorAmount = 3_000;

        Player smuggler = Player.create(1L, "밀수꾼", TeamRole.SMUGGLER, Money.from(smugglerAmount));
        Player inspector = Player.create(2L, "검문관", TeamRole.INSPECTOR, Money.from(inspectorAmount));
        PassSettlementRule rule = new PassSettlementRule();

        // when
        int smuggleAmount = 1_000;

        RoundSettlement actual = rule.settle(smuggler, inspector, Money.from(smuggleAmount), Money.ZERO);

        // then
        // 검문을 하지 않은 경우 검사관은 아무런 이득도, 손해도 얻지 않는다
        // 밀수꾼은 검문을 하지 않았으므로 밀수 금액을 그대로 얻는다
        // 검사관의 잔액은 3_000원을 그대로 유지한다
        // 밀수꾼의 잔액은 3_000 + 1_000 = 4_000원이 된다
        assertAll(
                () -> assertThat(actual.smuggler().getBalance()).isEqualTo(Money.from(4_000)), // 3_000 + 1_000
                () -> assertThat(actual.inspector().getBalance()).isEqualTo(Money.from(3_000)), // 3_000원 그대로 유지
                () -> assertThat(actual.outcomeType()).isEqualTo(RoundOutcomeType.PASS)
        );
    }
}
