package com.game.contraband.infrastructure.actor.game.chat.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.chat.ChatMuteReason;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RateLimitPolicyTest {

    @Test
    void 채팅_도배_차단_설정_값과_패널티를_생성한다() {
        // given
        RateLimitPenalty first = new RateLimitPenalty(
                10_000L,
                ChatMuteReason.LEVEL1,
                false,
                1
        );
        RateLimitPenalty second = RateLimitPenalty.permanent(
                ChatMuteReason.PERMANENT,
                2
        );

        // when
        RateLimitPolicy actual = new RateLimitPolicy(3, 5_000L, first, second);

        // then
        assertAll(
                () -> assertThat(actual.maxMessagesInWindow).isEqualTo(3),
                () -> assertThat(actual.windowMillis).isEqualTo(5_000L),
                () -> assertThat(actual.penalties).containsExactly(first, second)
        );
    }

    @Test
    void 윈도우_크기_당_입력_가능한_채팅_메시지_개수가_양수가_아니면_생성할_수_없다() {
        // given
        RateLimitPenalty penalty = RateLimitPenalty.permanent(ChatMuteReason.PERMANENT);

        // when & then
        assertThatThrownBy(() -> new RateLimitPolicy(0, 1_000L, penalty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxMessagesInWindow는 양수여야 합니다.");
    }

    @Test
    void 윈도우_크기가_양수가_아니면_생성할_수_없다() {
        // given
        RateLimitPenalty penalty = RateLimitPenalty.permanent(ChatMuteReason.PERMANENT);

        // when & then
        assertThatThrownBy(() -> new RateLimitPolicy(1, 0L, penalty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("windowMillis는 양수여야 합니다.");
    }

    @ParameterizedTest(name = "패널티 배열이 {0}일 때 생성할 수 없다")
    @NullAndEmptySource
    void 패널티_배열이_비어_있으면_생성할_수_없다(RateLimitPenalty[] penalties) {
        // when & then
        assertThatThrownBy(() -> new RateLimitPolicy(1, 1_000L, penalties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("penalties는 비어 있을 수 없습니다.");
    }

    @Test
    void 기본_정책을_제공한다() {
        // when
        RateLimitPolicy actual = RateLimitPolicy.defaultPolicy();

        // then
        assertAll(
                () -> assertThat(actual.maxMessagesInWindow).isEqualTo(20),
                () -> assertThat(actual.windowMillis).isEqualTo(10_000L),
                () -> assertThat(actual.penalties).hasSize(3),
                () -> assertThat(actual.penalties[0].muteMillis()).isEqualTo(30_000L),
                () -> assertThat(actual.penalties[0].reason()).isEqualTo(ChatMuteReason.LEVEL1),
                () -> assertThat(actual.penalties[0].level()).isEqualTo(1),
                () -> assertThat(actual.penalties[0].permanent()).isFalse(),
                () -> assertThat(actual.penalties[1].muteMillis()).isEqualTo(60_000L),
                () -> assertThat(actual.penalties[1].reason()).isEqualTo(ChatMuteReason.LEVEL2),
                () -> assertThat(actual.penalties[1].level()).isEqualTo(2),
                () -> assertThat(actual.penalties[1].permanent()).isFalse(),
                () -> assertThat(actual.penalties[2].muteMillis()).isEqualTo(Long.MAX_VALUE),
                () -> assertThat(actual.penalties[2].reason()).isEqualTo(ChatMuteReason.PERMANENT),
                () -> assertThat(actual.penalties[2].level()).isEqualTo(3),
                () -> assertThat(actual.penalties[2].permanent()).isTrue()
        );
    }

    @Test
    void 위반_횟수에_따라_적절한_패널티를_반환한다() {
        // given
        RateLimitPenalty first = new RateLimitPenalty(
                5_000L,
                ChatMuteReason.LEVEL1,
                false,
                1
        );
        RateLimitPenalty second = new RateLimitPenalty(
                10_000L,
                ChatMuteReason.LEVEL2,
                false,
                2
        );
        RateLimitPenalty third = RateLimitPenalty.permanent(ChatMuteReason.PERMANENT, 3);
        RateLimitPolicy policy = new RateLimitPolicy(2, 10_000L, first, second, third);

        // when & then
        assertAll(
                () -> assertThat(policy.penaltyFor(1)).isSameAs(first),
                () -> assertThat(policy.penaltyFor(0)).isSameAs(first),
                () -> assertThat(policy.penaltyFor(-1)).isSameAs(first),
                () -> assertThat(policy.penaltyFor(2)).isSameAs(second),
                () -> assertThat(policy.penaltyFor(3)).isSameAs(third),
                () -> assertThat(policy.penaltyFor(10)).isSameAs(third)
        );
    }

    @Test
    void 영구적이지_않은_패널티에서_차단_지속_시간이_양수가_아니라면_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> new RateLimitPenalty(0L, ChatMuteReason.LEVEL1, false, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("muteMillis는 양수여야 합니다.");
    }

    @Test
    void 패널티_사유가_비어_있다면_생성할_수_없다() {
        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> new RateLimitPenalty(1_000L, null, false, 1))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("reason은 null일 수 없습니다."),
                () -> assertThatThrownBy(() -> RateLimitPenalty.permanent(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("reason은 null일 수 없습니다.")
        );
    }

    @Test
    void 패널티_레벨이_양수가_아니면_생성할_수_없다() {
        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> new RateLimitPenalty(1_000L, ChatMuteReason.LEVEL1, false, 0))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("level은 양수여야 합니다."),
                () -> assertThatThrownBy(() -> RateLimitPenalty.permanent(ChatMuteReason.PERMANENT, 0))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("level은 양수여야 합니다.")
        );
    }

    @Test
    void 영구_패널티는_Long_MAX_지속시간과_플래그를_설정한다() {
        // when
        RateLimitPenalty actual = RateLimitPenalty.permanent(ChatMuteReason.PERMANENT, 5);

        // then
        assertAll(
                () -> assertThat(actual.muteMillis()).isEqualTo(Long.MAX_VALUE),
                () -> assertThat(actual.level()).isEqualTo(5),
                () -> assertThat(actual.permanent()).isTrue()
        );
    }
}
