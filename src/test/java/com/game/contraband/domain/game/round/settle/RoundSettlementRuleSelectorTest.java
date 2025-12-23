package com.game.contraband.domain.game.round.settle;

import static org.assertj.core.api.Assertions.assertThat;

import com.game.contraband.domain.game.round.InspectionDecision;
import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundSettlementRuleSelectorTest {

    @Test
    void 검사관이_검문을_하지_않고_통과시킨_경우_경우_밀수_금액과_무관하게_통과로_처리한다() {
        // given
        RoundSettlementRuleSelector rules = new RoundSettlementRuleSelector();
        InspectionDecision passDecision = InspectionDecision.PASS;
        Money smuggleAmount = Money.from(1_000);
        Money claimedAmount = Money.from(2_000);

        // when
        RoundSettlementRule actual = rules.selectRule(passDecision, smuggleAmount, claimedAmount);

        // then
        assertThat(actual).isInstanceOf(PassSettlementRule.class);
    }

    @Test
    void 밀수_금액이_0원이고_검문_금액이_양수면_검문_실패로_처리한다() {
        // given
        RoundSettlementRuleSelector rules = new RoundSettlementRuleSelector();
        InspectionDecision inspectionDecision = InspectionDecision.INSPECTION;
        Money smuggleAmount = Money.ZERO;
        Money claimedAmount = Money.from(900);

        // when
        RoundSettlementRule actual = rules.selectRule(inspectionDecision, smuggleAmount, claimedAmount);

        // then
        assertThat(actual).isInstanceOf(InspectionUnderSettlementRule.class);
    }

    @Test
    void 검문_금액이_밀수_금액보다_큰_경우_검문_실패로_처리한다() {
        // given
        RoundSettlementRuleSelector rules = new RoundSettlementRuleSelector();
        InspectionDecision inspectionDecision = InspectionDecision.INSPECTION;
        Money smuggleAmount = Money.from(500);
        Money claimedAmount = Money.from(1_000);

        // when
        RoundSettlementRule actual = rules.selectRule(inspectionDecision, smuggleAmount, claimedAmount);

        // then
        assertThat(actual).isInstanceOf(InspectionUnderSettlementRule.class);
    }

    @Test
    void 밀수_금액이_검문_금액보다_크거나_같으면_검문_성공으로_처리한다() {
        // given
        RoundSettlementRuleSelector rules = new RoundSettlementRuleSelector();
        InspectionDecision inspectionDecision = InspectionDecision.INSPECTION;
        Money smuggleAmount = Money.from(3_000);
        Money claimedAmount = Money.from(2_000);

        // when
        RoundSettlementRule actual = rules.selectRule(inspectionDecision, smuggleAmount, claimedAmount);

        // then
        assertThat(actual).isInstanceOf(InspectionHitSettlementRule.class);
    }
}
