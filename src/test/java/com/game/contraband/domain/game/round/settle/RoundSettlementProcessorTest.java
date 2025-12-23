package com.game.contraband.domain.game.round.settle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.RoundOutcomeType;
import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundSettlementProcessorTest {

    private final RoundSettlementProcessor processor = RoundSettlementProcessor.create();

    @Test
    void 통과_선택시_라운드_정산을_처리한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();
        Player smuggler = Player.create(
                1L,
                "밀수꾼",
                TeamRole.SMUGGLER,
                Money.from(3_000)
        );
        Player inspector = Player.create(
                2L,
                "검사관",
                TeamRole.INSPECTOR,
                Money.from(3_000)
        );

        // when
        RoundSettlementProcessor.RoundSettlementResult actual = processor.process(
                round,
                smuggler,
                inspector
        );

        // then
        // 검사관이 통과를 선택하면 밀수꾼은 밀수 금액만큼 획득하고 검사관은 변동이 없다
        // 밀수꾼은 3_000 + 1_000 = 4_000원이 된다
        // 검사관은 3_000원을 그대로 유지한다
        assertAll(
                () -> assertThat(actual.updatedSmuggler().getBalance().getAmount()).isEqualTo(4_000), // 3_000 + 1_000
                () -> assertThat(actual.updatedInspector().getBalance().getAmount()).isEqualTo(3_000), // 3_000원 유지
                () -> assertThat(actual.settlement().outcomeType()).isEqualTo(RoundOutcomeType.PASS)
        );
    }

    @Test
    void 검문_선택시_밀수_적발_성공_정산을_처리한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decideInspection(Money.from(900));
        Player smuggler = Player.create(
                1L,
                "밀수꾼",
                TeamRole.SMUGGLER,
                Money.from(3_000)
        );
        Player inspector = Player.create(
                2L,
                "검사관",
                TeamRole.INSPECTOR,
                Money.from(3_000)
        );

        // when
        RoundSettlementProcessor.RoundSettlementResult actual = processor.process(
                round,
                smuggler,
                inspector
        );

        // then
        // 검문에 성공한 경우 검사관은 검문 금액을 얻으며 밀수꾼은 아무런 이득도, 손해도 얻지 않는다
        // 검사관은 검문 금액 900원을 얻는다
        // 밀수꾼은 잔액을 그대로 유지한다
        // 검사관의 소지 금액은 3_000 + 900 = 3_900원이 된다
        // 밀수꾼의 소지 금액은 3_000원을 유지한다
        assertAll(
                () -> assertThat(actual.updatedSmuggler().getBalance().getAmount()).isEqualTo(3_000), // 3_000원 유지
                () -> assertThat(actual.updatedInspector().getBalance().getAmount()).isEqualTo(3_900), // 3_000 + 900
                () -> assertThat(actual.settlement().outcomeType()).isEqualTo(RoundOutcomeType.INSPECTION_HIT)
        );
    }

    @Test
    void 검문_선택시_밀수_금액이_0원인_경우_정산을_처리한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.ZERO,
                                   Money.from(3_000)
                           )
                           .decideInspection(Money.from(1_000));
        Player smuggler = Player.create(
                1L,
                "밀수꾼",
                TeamRole.SMUGGLER,
                Money.from(3_000)
        );
        Player inspector = Player.create(
                2L,
                "검사관",
                TeamRole.INSPECTOR,
                Money.from(3_000)
        );

        // when
        RoundSettlementProcessor.RoundSettlementResult actual = processor.process(
                round,
                smuggler,
                inspector
        );

        // then
        // 검문에 실패해 빈 가방을 적중한 경우 검사관은 검문 금액의 절반을 잃고 밀수꾼은 그만큼 얻는다
        // 검사관은 검문 금액 1_000원의 절반인 500원을 잃는다
        // 밀수꾼은 검사관이 잃은 500원을 얻는다
        // 검사관의 소지 금액은 3_000 - 500 = 2_500원이 된다
        // 밀수꾼의 소지 금액은 3_000 + 500 = 3_500원이 된다
        assertAll(
                () -> assertThat(actual.updatedSmuggler().getBalance().getAmount()).isEqualTo(3_500), // 3_000 + 500
                () -> assertThat(actual.updatedInspector().getBalance().getAmount()).isEqualTo(2_500), // 3_000 - 500
                () -> assertThat(actual.settlement().outcomeType()).isEqualTo(RoundOutcomeType.INSPECTION_UNDER)
        );
    }

    @Test
    void 검문_선택시_밀수_금액이_기준보다_적은_경우_정산을_처리한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(500),
                                   Money.from(3_000)
                           )
                           .decideInspection(Money.from(800));
        Player smuggler = Player.create(
                1L,
                "밀수꾼",
                TeamRole.SMUGGLER,
                Money.from(3_000)
        );
        Player inspector = Player.create(
                2L,
                "검사관",
                TeamRole.INSPECTOR,
                Money.from(3_000)
        );

        // when
        RoundSettlementProcessor.RoundSettlementResult actual = processor.process(
                round,
                smuggler,
                inspector
        );

        // then
        // 검문 금액이 실제보다 큰 경우(검문 실패) 밀수꾼은 밀수 금액과 검문 금액 절반을 모두 얻고 검사관은 절반을 잃는다
        // 밀수꾼은 밀수 금액 500원과 보상금 800원의 절반인 400원을 얻는다
        // 검사관은 보상금 절반 400원을 잃는다
        // 밀수꾼의 소지 금액은 3_000 + 500 + 400 = 3_900원이 된다
        // 검사관의 소지 금액은 3_000 - 400 = 2_600원이 된다
        assertAll(
                () -> assertThat(actual.updatedSmuggler().getBalance().getAmount()).isEqualTo(3_900), // 3_000 + 500 + 400
                () -> assertThat(actual.updatedInspector().getBalance().getAmount()).isEqualTo(2_600), // 3_000 - 400
                () -> assertThat(actual.settlement().outcomeType()).isEqualTo(RoundOutcomeType.INSPECTION_UNDER)
        );
    }

    @Test
    void 정산_결과에서_업데이트된_밀수범_정보를_조회한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();
        Player smuggler = Player.create(
                1L,
                "밀수꾼",
                TeamRole.SMUGGLER,
                Money.from(3_000)
        );
        Player inspector = Player.create(
                2L,
                "검사관",
                TeamRole.INSPECTOR,
                Money.from(3_000)
        );

        // when
        RoundSettlementProcessor.RoundSettlementResult actual = processor.process(
                round,
                smuggler,
                inspector
        );

        // then
        assertAll(
                () -> assertThat(actual.updatedSmuggler().getId()).isEqualTo(1L),
                () -> assertThat(actual.updatedSmuggler().getName()).isEqualTo("밀수꾼"),
                () -> assertThat(actual.updatedSmuggler().getBalance().getAmount()).isEqualTo(4_000)
        );
    }

    @Test
    void 정산_결과에서_업데이트된_검문관_정보를_조회한다() {
        // given
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(
                                   Money.from(1_000),
                                   Money.from(3_000)
                           )
                           .decidePass();
        Player smuggler = Player.create(
                1L,
                "밀수꾼",
                TeamRole.SMUGGLER,
                Money.from(3_000)
        );
        Player inspector = Player.create(
                2L,
                "검사관",
                TeamRole.INSPECTOR,
                Money.from(3_000)
        );

        // when
        RoundSettlementProcessor.RoundSettlementResult actual = processor.process(
                round,
                smuggler,
                inspector
        );

        // then
        assertAll(
                () -> assertThat(actual.updatedInspector().getId()).isEqualTo(2L),
                () -> assertThat(actual.updatedInspector().getName()).isEqualTo("검사관"),
                () -> assertThat(actual.updatedInspector().getBalance().getAmount()).isEqualTo(3_000)
        );
    }
}
