package com.game.contraband.infrastructure.actor.game.chat.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.chat.ChatMuteReason;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlayerModerationStateTest {

    @Test
    void 제한_내_메시지는_허용된다() {
        // given
        RateLimitPolicy policy = new RateLimitPolicy(
                20,
                10_000L,
                new RateLimitPenalty(30_000L, ChatMuteReason.LEVEL1, false, 1)
        );
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 19; i++) {
            state.evaluate(now + i * 100, policy);
        }
        ModerationResult lastResult = state.evaluate(now + 19 * 100, policy);

        // then
        assertAll(
                () -> assertThat(lastResult.allowed()).isTrue(),
                () -> assertThat(lastResult.countInWindow()).isEqualTo(20)
        );
    }

    @Test
    void 제한_초과_시_차단된다() {
        // given
        RateLimitPolicy policy = new RateLimitPolicy(
                20,
                10_000L,
                new RateLimitPenalty(30_000L, ChatMuteReason.LEVEL1, false, 1)
        );
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        ModerationResult result = state.evaluate(now + 2000, policy);

        // then
        assertAll(
                () -> assertThat(result.allowed()).isFalse(),
                () -> assertThat(result.countInWindow()).isEqualTo(21),
                () -> assertThat(result.mutedUntil()).isEqualTo(now + 2000 + 30_000L),
                () -> assertThat(result.reason()).isEqualTo(ChatMuteReason.LEVEL1),
                () -> assertThat(result.level()).isEqualTo(1)
        );
    }

    @Test
    void 차단_1단계_30초_뮤트() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        long blockTime = now + 2000;
        ModerationResult blocked = state.evaluate(blockTime, policy);

        // then
        assertAll(
                () -> assertThat(blocked.allowed()).isFalse(),
                () -> assertThat(blocked.reason()).isEqualTo(ChatMuteReason.LEVEL1),
                () -> assertThat(blocked.level()).isEqualTo(1),
                () -> assertThat(blocked.mutedUntil()).isEqualTo(blockTime + 30_000L)
        );
    }

    @Test
    void 차단_2단계_60초_뮤트() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        state.evaluate(now + 2000, policy);

        long afterFirstMute = now + 32_000L;

        for (int i = 0; i < 20; i++) {
            state.evaluate(afterFirstMute + i * 100, policy);
        }
        long secondBlockTime = afterFirstMute + 2000;
        ModerationResult blocked = state.evaluate(secondBlockTime, policy);

        // then
        assertAll(
                () -> assertThat(blocked.allowed()).isFalse(),
                () -> assertThat(blocked.reason()).isEqualTo(ChatMuteReason.LEVEL2),
                () -> assertThat(blocked.level()).isEqualTo(2),
                () -> assertThat(blocked.mutedUntil()).isEqualTo(secondBlockTime + 60_000L)
        );
    }

    @Test
    void 차단_3단계_영구_차단() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        state.evaluate(now + 2000, policy);

        long afterFirstMute = now + 32_000L;

        for (int i = 0; i < 20; i++) {
            state.evaluate(afterFirstMute + i * 100, policy);
        }
        state.evaluate(afterFirstMute + 2000, policy);

        long afterSecondMute = afterFirstMute + 62_000L;

        for (int i = 0; i < 20; i++) {
            state.evaluate(afterSecondMute + i * 100, policy);
        }
        ModerationResult blocked = state.evaluate(afterSecondMute + 2000, policy);

        // then
        assertAll(
                () -> assertThat(blocked.allowed()).isFalse(),
                () -> assertThat(blocked.reason()).isEqualTo(ChatMuteReason.PERMANENT),
                () -> assertThat(blocked.level()).isEqualTo(3),
                () -> assertThat(blocked.mutedUntil()).isEqualTo(Long.MAX_VALUE)
        );
    }

    @Test
    void 뮤트_해제_시_releasedLevel_반환() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        state.evaluate(now + 2000, policy);

        long afterMute = now + 32_000L;
        ModerationResult released = state.evaluate(afterMute, policy);

        // then
        assertAll(
                () -> assertThat(released.allowed()).isTrue(),
                () -> assertThat(released.releasedLevel()).isEqualTo(1),
                () -> assertThat(released.countInWindow()).isEqualTo(1)
        );
    }

    @Test
    void 영구_차단은_해제되지_않음() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        state.evaluate(now + 2000, policy);

        long afterFirstMute = now + 32_000L;
        for (int i = 0; i < 20; i++) {
            state.evaluate(afterFirstMute + i * 100, policy);
        }
        state.evaluate(afterFirstMute + 2000, policy);

        long afterSecondMute = afterFirstMute + 62_000L;
        for (int i = 0; i < 20; i++) {
            state.evaluate(afterSecondMute + i * 100, policy);
        }
        state.evaluate(afterSecondMute + 2000, policy);

        long veryFuture = afterSecondMute + 100_000_000L;
        ModerationResult result = state.evaluate(veryFuture, policy);

        // then
        assertAll(
                () -> assertThat(result.allowed()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo(ChatMuteReason.PERMANENT),
                () -> assertThat(result.level()).isEqualTo(3)
        );
    }

    @Test
    void 뮤트_해제_후_첫_메시지는_카운트_1로_시작() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        state.evaluate(now + 2000, policy);

        long afterMute = now + 32_000L;
        ModerationResult first = state.evaluate(afterMute, policy);

        // then
        assertAll(
                () -> assertThat(first.allowed()).isTrue(),
                () -> assertThat(first.countInWindow()).isEqualTo(1),
                () -> assertThat(first.releasedLevel()).isEqualTo(1)
        );
    }

    @Test
    void 뮤트_중에는_계속_차단된다() {
        // given
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy();
        PlayerModerationState state = new PlayerModerationState(policy.windowMillis);
        long now = 0L;

        // when
        for (int i = 0; i < 20; i++) {
            state.evaluate(now + i * 100, policy);
        }
        state.evaluate(now + 2000, policy);

        long duringMute = now + 17_000L;
        ModerationResult result = state.evaluate(duringMute, policy);

        // then
        assertAll(
                () -> assertThat(result.allowed()).isFalse(),
                () -> assertThat(result.reason()).isEqualTo(ChatMuteReason.LEVEL1),
                () -> assertThat(result.level()).isEqualTo(1)
        );
    }
}
