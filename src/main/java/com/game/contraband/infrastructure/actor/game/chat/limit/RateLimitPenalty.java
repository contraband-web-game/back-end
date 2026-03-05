package com.game.contraband.infrastructure.actor.game.chat.limit;

import com.game.contraband.infrastructure.actor.game.chat.ChatMuteReason;

public record RateLimitPenalty(long muteMillis, ChatMuteReason reason, boolean permanent, int level) {

    public RateLimitPenalty {
        if (!permanent && muteMillis <= 0) {
            throw new IllegalArgumentException("muteMillis는 양수여야 합니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("reason은 null일 수 없습니다.");
        }
        if (level <= 0) {
            throw new IllegalArgumentException("level은 양수여야 합니다.");
        }
    }

    public static RateLimitPenalty permanent(ChatMuteReason reason) {
        return permanent(reason, 3);
    }

    public static RateLimitPenalty permanent(ChatMuteReason reason, int level) {
        return new RateLimitPenalty(Long.MAX_VALUE, reason, true, level);
    }
}
