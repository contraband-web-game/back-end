package com.game.contraband.domain.game.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.settle.RoundSettlement;
import com.game.contraband.domain.game.round.settle.RoundSettlementRuleSelector;
import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundTest {

    @Test
    void 새_라운드를_생성한다() {
        // when
        Round actual = Round.newRound(1, 1L, 2L);

        // then
        assertAll(
                () -> assertThat(actual.getRoundNumber()).isEqualTo(1),
                () -> assertThat(actual.getSmugglerId()).isEqualTo(1L),
                () -> assertThat(actual.getInspectorId()).isEqualTo(2L),
                () -> assertThat(actual.getStatus()).isEqualTo(RoundStatus.NEW),
                () -> assertThat(actual.getSmuggleAmount()).isEqualTo(Money.ZERO),
                () -> assertThat(actual.getInspectionDecision()).isEqualTo(InspectionDecision.NONE),
                () -> assertThat(actual.getInspectionThreshold()).isEqualTo(Money.ZERO)
        );
    }

    @Test
    void 라운드_시작_직후에_밀수_금액을_선언한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);
        Player smuggler = Player.create(1L, "S1", TeamRole.SMUGGLER, Money.from(3_000));

        // when
        Round actual = round.declareSmuggleAmount(
                Money.from(1_000),
                smuggler.getBalance()
        );

        // then
        assertAll(
                () -> assertThat(actual.getStatus()).isEqualTo(RoundStatus.SMUGGLE_DECLARED),
                () -> assertThat(actual.getSmuggleAmount().getAmount()).isEqualTo(1_000)
        );
    }

    @Test
    void 라운드에_지정되지_않은_밀수꾼은_밀수_금액을_선언할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(
                () -> round.declareSmuggleAmount(
                        999L,
                        Money.from(1_000),
                        Money.from(3_000)
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("라운드에 지정된 밀수꾼만 밀수 금액을 선언할 수 있습니다.");
    }

    @Test
    void 라운드를_시작하지_않았다면_밀수_금액을_선언할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           );

        // when & then
        assertThatThrownBy(
                () -> round.declareSmuggleAmount(
                        Money.from(2_000),
                        Money.from(3_000)
                )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessage("이미 밀수 금액을 선언했습니다.");
    }

    @Test
    void 밀수_금액이_음수라면_밀수_금액을_선언할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(
                () -> round.declareSmuggleAmount(
                        Money.from(-1_000),
                        Money.from(3_000)
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("금액은 0원 이상이어야 합니다.");
    }

    @Test
    void 밀수_금액이_최대_금액인_1000_원을_초과하면_밀수_금액을_선언할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(
                () -> round.declareSmuggleAmount(
                        Money.from(1_100),
                        Money.from(3_000)
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("허용된 최대 밀수 금액을 초과할 수 없습니다.");
    }

    @Test
    void 밀수_금액이_보유_금액을_초과하면_밀수_금액을_선언할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(
                () -> round.declareSmuggleAmount(
                        Money.from(1_000),
                        Money.from(900)
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("보유 금액보다 많이 밀수할 수 없습니다.");
    }

    @Test
    void 밀수_금액이_100원_단위가_아니면_밀수_금액을_선언할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(
                () -> round.declareSmuggleAmount(
                        Money.from(950),
                        Money.from(3_000)
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("밀수 금액은 100원 단위여야 합니다.");
    }

    @Test
    void 이미_검문을_선택했다면_검사관이_다시_행동을_선택할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();

        // when & then
        assertThatThrownBy(() -> round.decidePass())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("검사관의 선택은 한 번만 할 수 있습니다.");
    }

    @Test
    void 라운드에_지정되지_않은_검사관은_검문을_하지_않고_통과를_선택할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(() -> round.decidePass(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("라운드에 지정된 검사관만 검문 결정을 내릴 수 있습니다.");
    }

    @Test
    void 검사관이_먼저_검문을_하지_않고_통과를_선택한_경우_밀수_금액을_선언하면_라운드_내_모든_참여자의_행동이_결정된_상태로_변경된다() {
        // given
        Round round = Round.newRound(1, 1L, 2L).decidePass();

        // when
        Round actual = round.declareSmuggleAmount(
                Money.from(1_000),
                Money.from(3_000)
        );

        // then
        assertThat(actual.getStatus()).isEqualTo(RoundStatus.INSPECTION_DECIDED);
    }

    @Test
    void 검사관이_먼저_검문을_선택하고_밀수_금액을_선언하면_라운드_내_모든_참여자의_행동이_결정된_상태로_변경된다() {
        // given
        Round round = Round.newRound(1, 1L, 2L).decideInspection(Money.from(500));

        // when
        Round actual = round.declareSmuggleAmount(Money.from(1_000), Money.from(3_000));

        // then
        assertThat(actual.getStatus()).isEqualTo(RoundStatus.INSPECTION_DECIDED);
    }

    @Test
    void 라운드에_지정되지_않은_검사관은_검문을_선택할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when & then
        assertThatThrownBy(() -> round.decideInspection(999L, Money.from(500)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("라운드에 지정된 검사관만 검문 결정을 내릴 수 있습니다.");
    }

    @Test
    void 검문_기준_금액이_0원이면_검문을_선택할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           );

        // when & then
        assertThatThrownBy(() -> round.decideInspection(Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검문 기준 금액은 0보다 커야 합니다.");
    }

    @Test
    void 검문_기준_금액이_100원_단위가_아니면_검문을_선택할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           );

        // when & then
        assertThatThrownBy(() -> round.decideInspection(Money.from(950)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검문 기준 금액은 100원 단위여야 합니다.");
    }

    @Test
    void 검문_기준_금액이_검문관_보유_금액의_2배를_초과하면_검문을_선택할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           );

        // when & then
        assertThatThrownBy(() -> round.decideInspection(Money.from(1_100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검문 기준 금액은 최대 1000원을 초과할 수 없습니다.");
    }

    @Test
    void 검사관의_선택이_끝나지_않은_상태에서_정산할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);
        Player smuggler = Player.create(1L, "S1", TeamRole.SMUGGLER, Money.from(3_000));
        Player inspector = Player.create(2L, "I1", TeamRole.INSPECTOR, Money.from(3_000));
        RoundSettlementRuleSelector ruleSelector = new RoundSettlementRuleSelector();

        // when & then
        assertThatThrownBy(() -> round.settle(smuggler, inspector, ruleSelector))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정산은 검사관의 선택이 끝난 후에만 수행할 수 있습니다.");
    }

    @Test
    void 라운드에_지정된_플레이어와_다른_플레이어로_정산할_수_없다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();
        Player wrongSmuggler = Player.create(3L, "참여하지 않은 플레이어", TeamRole.SMUGGLER, Money.from(3_000));
        Player inspector = Player.create(2L, "검사관", TeamRole.INSPECTOR, Money.from(3_000));
        RoundSettlementRuleSelector ruleSelector = new RoundSettlementRuleSelector();

        // when & then
        assertThatThrownBy(() -> round.settle(wrongSmuggler, inspector, ruleSelector))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("라운드에 지정된 플레이어와 정산 대상 플레이어가 일치하지 않습니다.");
    }

    @Test
    void 라운드에서_모든_참여자의_동작을_선언한_상태에서_정산을_진행한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();
        Player smuggler = Player.create(1L, "S1", TeamRole.SMUGGLER, Money.from(3_000));
        Player inspector = Player.create(2L, "I1", TeamRole.INSPECTOR, Money.from(3_000));
        RoundSettlementRuleSelector ruleSelector = new RoundSettlementRuleSelector();

        // when
        RoundSettlement settlement = round.settle(smuggler, inspector, ruleSelector);

        // then
        assertThat(settlement.outcomeType()).isEqualTo(RoundOutcomeType.PASS);
    }

    @Test
    void 새_라운드_상태인지_여부를_반환한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L);

        // when
        boolean actual = round.isNotNewStatus();

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 검문이_결정되지_않은_상태인지_여부를_반환한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           );

        // when
        boolean actual = round.isNotInspectionDecided();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 검문이_결정된_상태인지_여부를_반환한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();

        // when
        boolean actual = round.isNotInspectionDecided();

        // then
        assertThat(actual).isFalse();
    }
}

