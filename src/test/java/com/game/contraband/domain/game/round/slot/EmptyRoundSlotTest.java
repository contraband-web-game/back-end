package com.game.contraband.domain.game.round.slot;

import static org.assertj.core.api.Assertions.assertThat;

import com.game.contraband.domain.game.round.Round;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EmptyRoundSlotTest {

    @Test
    void 비어_있는_라운드_슬롯은_항상_라운드를_가지지_않는다() {
        // when
        boolean actual = EmptyRoundSlot.INSTANCE.hasRound();

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 라운드_조회_시_빈_Optional을_반환한다() {
        // when
        Optional<Round> actual = EmptyRoundSlot.INSTANCE.currentRound();

        // then
        assertThat(actual).isEmpty();
    }

    @Test
    void 비어_있는_슬롯의_상수는_싱글톤으로_제공된다() {
        // when
        EmptyRoundSlot instance1 = EmptyRoundSlot.INSTANCE;
        EmptyRoundSlot instance2 = EmptyRoundSlot.INSTANCE;

        // then
        assertThat(instance1).isSameAs(instance2);
    }
}

