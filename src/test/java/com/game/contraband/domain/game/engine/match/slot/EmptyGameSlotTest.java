package com.game.contraband.domain.game.engine.match.slot;

import static org.assertj.core.api.Assertions.assertThat;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EmptyGameSlotTest {

    @Test
    void 비어_있는_게임_슬롯은_항상_게임을_가지지_않는다() {
        // when
        boolean actual = EmptyGameSlot.INSTANCE.hasGame();

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 게임_조회_시_빈_Optional을_반환한다() {
        // when
        Optional<ContrabandGame> actual = EmptyGameSlot.INSTANCE.currentGame();

        // then
        assertThat(actual).isEmpty();
    }

    @Test
    void 비어_있는_게임_슬롯의_상수는_싱글톤으로_제공된다() {
        // when
        EmptyGameSlot actual1 = EmptyGameSlot.INSTANCE;
        EmptyGameSlot actual2 = EmptyGameSlot.INSTANCE;

        // then
        assertThat(actual1).isSameAs(actual2);
    }
}
