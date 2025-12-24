package com.game.contraband.domain.game.engine.match.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ActiveGameSlotTest {

    @Test
    void 유효한_게임으로_진행_중인_게임_슬롯을_초기화_한다() {
        // given
        ContrabandGame game = mock(ContrabandGame.class);

        // when
        ActiveGameSlot actual = ActiveGameSlot.create(game);

        // then
        assertAll(
                () -> assertThat(actual.hasGame()).isTrue(),
                () -> assertThat(actual.currentGame()).contains(game)
        );
    }

    @Test
    void 비어_있는_게임으로는_게임_슬롯을_초기화할_수_없다() {
        // when & then
        assertThatThrownBy(() -> ActiveGameSlot.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게임은 비어 있을 수 없습니다.");
    }

    @Test
    void 진행_중인_게임_슬롯은_항상_게임을_가지고_있다() {
        // given
        ContrabandGame game = mock(ContrabandGame.class);

        // when
        ActiveGameSlot actual = ActiveGameSlot.create(game);

        // then
        assertAll(
                () -> assertThat(actual.hasGame()).isTrue(),
                () -> assertThat(actual.currentGame()).hasValue(game)
        );
    }

    @Test
    void 진행_중인_게임을_Optional로_감싸_반환한다() {
        // given
        ContrabandGame game = mock(ContrabandGame.class);

        // when
        ActiveGameSlot actual = ActiveGameSlot.create(game);

        // then
        assertAll(
                () -> assertThat(actual.currentGame()).isPresent(),
                () -> assertThat(actual.currentGame()).contains(game)
        );
    }
}
