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
class InspectionUnderSettlementRuleTest {

    @Test
    void 검문_금액이_밀수_금액보다_높아_검문에_실패한_경우_검사관이_검문_금액의_절반을_배상금을_지급한다() {
        // given
        int smugglerAmount = 1_000;
        int inspectorAmount = 2_000;

        Player smuggler = Player.create(1L, "밀수꾼", TeamRole.SMUGGLER, Money.from(smugglerAmount));
        Player inspector = Player.create(2L, "검사관", TeamRole.INSPECTOR, Money.from(inspectorAmount));
        InspectionUnderSettlementRule rule = new InspectionUnderSettlementRule();

        // when
        int smuggleAmount = 500;
        int claimAmount = 1_000;

        RoundSettlement actual = rule.settle(smuggler, inspector, Money.from(smuggleAmount), Money.from(claimAmount));

        // then
        // 검문에 실패한 경우 검사관은 검문 금액의 절반을 배상금으로 지급해야 한다
        // 검사관은 검문 금액 1_000원의 절반인 500원을 밀수꾼에게 배상금으로 지급한다
        // 밀수꾼은 밀수 금액 500원에 더해 검사관이 지불한 배상금 500원을 받는다
        // 검사관의 소지 금액은 2_000 - 500 = 1_500원이 된다
        // 밀수꾼의 소지 금액은 1_000 + 500 + 500 = 2_000원이 된다
        assertAll(
                () -> assertThat(actual.outcomeType()).isEqualTo(RoundOutcomeType.INSPECTION_UNDER),
                () -> assertThat(actual.smuggler().getBalance()).isEqualTo(Money.from(2_000)), // 1_000 + 500 + 500
                () -> assertThat(actual.inspector().getBalance()).isEqualTo(Money.from(1_500)) // 2_000 - 500
        );
    }
}
