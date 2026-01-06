package com.game.contraband.infrastructure.actor.game.chat.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SlidingWindowCounterTest {

    @Test
    void 시간_내_메시지_수를_증가시킨다() {
        // given
        SlidingWindowCounter counter = new SlidingWindowCounter(1_000L);

        // when
        int first = counter.incrementAndGet(100L);
        int second = counter.incrementAndGet(200L);

        // then
        assertAll(
                () -> assertThat(first).isEqualTo(1),
                () -> assertThat(second).isEqualTo(2),
                () -> assertThat(counter.currentCount(500L)).isEqualTo(2)
        );
    }

    @Test
    void 윈도우를_벗어난_메시지는_카운팅에서_제외된다() {
        // given
        SlidingWindowCounter counter = new SlidingWindowCounter(1_000L);

        counter.incrementAndGet(0L);
        counter.incrementAndGet(500L);

        // when
        int countAfterEviction = counter.currentCount(1_600L);
        int afterNewMessage = counter.incrementAndGet(1_600L);

        // then
        assertAll(
                () -> assertThat(countAfterEviction).isZero(),
                () -> assertThat(afterNewMessage).isEqualTo(1),
                () -> assertThat(counter.currentCount(1_700L)).isEqualTo(1)
        );
    }

    @Test
    void 경계_시간에_있는_메시지는_카운팅에_포함된다() {
        // given
        SlidingWindowCounter counter = new SlidingWindowCounter(1_000L);

        counter.incrementAndGet(0L);
        counter.incrementAndGet(1_000L);

        // when
        int actual = counter.currentCount(2_000L);

        // then
        assertThat(actual).isEqualTo(1);
    }

    @Test
    void 모든_기록을_비운다() {
        // given
        SlidingWindowCounter counter = new SlidingWindowCounter(1_000L);

        counter.incrementAndGet(100L);
        counter.incrementAndGet(200L);

        // when
        counter.reset();

        // then
        assertThat(counter.currentCount(500L)).isZero();
    }
}

