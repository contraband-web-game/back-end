package com.game.contraband.domain.game.round.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.round.InspectionDecision;
import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InspectionStateTest {

    @Test
    void 검사관_라운드_정보를_초기화한다() {
        // when
        InspectionState actual = InspectionState.initial(2L);

        // then
        assertAll(
                () -> assertThat(actual.getDecision()).isEqualTo(InspectionDecision.NONE),
                () -> assertThat(actual.getThreshold()).isEqualTo(Money.ZERO),
                () -> assertThat(actual.isProvided()).isFalse(),
                () -> assertThat(actual.getInspectorId()).isEqualTo(2L)
        );
    }

    @Test
    void 검사관이_검문을_하지_않고_통과했음을_기록한다() {
        // given
        InspectionState inspectionState = InspectionState.initial(2L);

        // when
        InspectionState actual = inspectionState.decidePass(2L);

        // then
        assertAll(
                () -> assertThat(actual.getDecision()).isEqualTo(InspectionDecision.PASS),
                () -> assertThat(actual.getThreshold()).isEqualTo(Money.ZERO),
                () -> assertThat(actual.isProvided()).isTrue()
        );
    }

    @Test
    void 검사관이_검문을_했음을_기록한다() {
        // given
        InspectionState inspectionState = InspectionState.initial(2L);

        // when
        InspectionState actual = inspectionState.decideInspection(2L, Money.from(500));

        // then
        assertAll(
                () -> assertThat(actual.getDecision()).isEqualTo(InspectionDecision.INSPECTION),
                () -> assertThat(actual.getThreshold()).isEqualTo(Money.from(500)),
                () -> assertThat(actual.isProvided()).isTrue()
        );
    }

    @Test
    void 이미_해당_라운드의_동작을_결정한_후에는_다시_검문을_하지_않고_통과를_선언할_수_없다() {
        // given
        InspectionState inspectionState = InspectionState.initial(2L)
                                                         .decidePass(2L);

        // when & then
        assertThatThrownBy(() -> inspectionState.decidePass(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("검사관의 선택은 한 번만 할 수 있습니다.");
    }

    @Test
    void 이미_해당_라운드의_동작을_결정한_후에는_다시_검문을_선언할_수_없다() {
        // given
        InspectionState inspectionState = InspectionState.initial(2L)
                                                         .decideInspection(2L, Money.from(400));

        assertThatThrownBy(() -> inspectionState.decideInspection(2L, Money.from(200)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("검사관의 선택은 한 번만 할 수 있습니다.");
    }
}

