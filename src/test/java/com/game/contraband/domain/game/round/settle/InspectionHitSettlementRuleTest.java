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
class InspectionHitSettlementRuleTest {

    @Test
    void 검문_성공시_검사관은_검문_금액을_획득하고_밀수범_잔액은_변하지_않는다() {
        // given
        int smugglerAmount = 3_000;
        int inspectorAmount = 5_000;

        Player smuggler = Player.create(1L, "밀수꾼", TeamRole.SMUGGLER, Money.from(smugglerAmount));
        Player inspector = Player.create(2L, "검사관", TeamRole.INSPECTOR, Money.from(inspectorAmount));
        InspectionHitSettlementRule rule = new InspectionHitSettlementRule();

        // when
        int smuggleAmount = 1_000;
        int claimAmount = 1_000;

        RoundSettlement actual = rule.settle(smuggler, inspector, Money.from(smuggleAmount), Money.from(claimAmount));

        // then
        // 검문에 성공한 경우 검사관은 검문 금액을 얻으며 밀수꾼은 아무런 이득도, 손해도 얻지 않는다
        // 검사관은 검문 금액 1_000원을 얻는다
        // 밀수꾼은 잔액을 그대로 유지한다
        // 검사관의 소지 금액은 5_000 + 1_000 = 6_000원이 된다
        // 밀수꾼의 소지 금액은 3_000원을 유지한다
        assertAll(
                () -> assertThat(actual.outcomeType()).isEqualTo(RoundOutcomeType.INSPECTION_HIT),
                () -> assertThat(actual.smuggler().getBalance()).isEqualTo(Money.from(3_000)), // 3_000원 그대로 유지
                () -> assertThat(actual.inspector().getBalance()).isEqualTo(Money.from(6_000)) // 5_000 + 1_000
        );
    }
}
