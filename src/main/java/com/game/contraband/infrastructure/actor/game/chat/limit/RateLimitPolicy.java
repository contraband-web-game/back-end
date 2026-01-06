package com.game.contraband.infrastructure.actor.game.chat.limit;

import com.game.contraband.infrastructure.actor.game.chat.ChatMuteReason;

public class RateLimitPolicy {

    public final int maxMessagesInWindow;
    public final long windowMillis;
    public final RateLimitPenalty[] penalties;

    public RateLimitPolicy(int maxMessagesInWindow, long windowMillis, RateLimitPenalty... penalties) {
        if (maxMessagesInWindow <= 0) {
            throw new IllegalArgumentException("maxMessagesInWindow는 양수여야 합니다.");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis는 양수여야 합니다.");
        }
        if (penalties == null || penalties.length == 0) {
            throw new IllegalArgumentException("penalties는 비어 있을 수 없습니다.");
        }

        this.maxMessagesInWindow = maxMessagesInWindow;
        this.windowMillis = windowMillis;
        this.penalties = penalties;
    }

    public static RateLimitPolicy defaultPolicy() {
        return new RateLimitPolicy(
                20,
                10_000L,
                new RateLimitPenalty(
                        30_000L,
                        ChatMuteReason.LEVEL1,
                        false,
                        1
                ),
                new RateLimitPenalty(
                        60_000L,
                        ChatMuteReason.LEVEL2,
                        false,
                        2
                ),
                RateLimitPenalty.permanent(ChatMuteReason.PERMANENT, 3)
        );
    }

    public RateLimitPenalty penaltyFor(int violationCount) {
        int index = Math.min(Math.max(violationCount - 1, 0), penalties.length - 1);

        return penalties[index];
    }
}
