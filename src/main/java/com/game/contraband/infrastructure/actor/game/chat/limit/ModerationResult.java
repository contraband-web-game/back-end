package com.game.contraband.infrastructure.actor.game.chat.limit;

import com.game.contraband.infrastructure.actor.game.chat.ChatMuteReason;

public record ModerationResult(
        boolean allowed,
        int countInWindow,
        long mutedUntil,
        ChatMuteReason reason,
        int releasedLevel,
        int level
) {

    static ModerationResult allowed(int count, int releasedLevel) {
        return new ModerationResult(true, count, 0L, null, releasedLevel, 0);
    }

    static ModerationResult blocked(int count, long mutedUntil, ChatMuteReason reason, int level) {
        return new ModerationResult(false, count, mutedUntil, reason, 0, level);
    }
}
