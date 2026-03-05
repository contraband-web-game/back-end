package com.game.contraband.infrastructure.actor.game.chat.limit;

import com.game.contraband.infrastructure.actor.game.chat.ChatMuteReason;

public class PlayerModerationState {

    private final SlidingWindowCounter counter;
    private int violationCount = 0;
    private long mutedUntilMillis = 0L;
    private ChatMuteReason mutedReason = null;
    private int lastLevel = 0;

    public PlayerModerationState(long windowMillis) {
        this.counter = new SlidingWindowCounter(windowMillis);
    }

    public ModerationResult evaluate(long now, RateLimitPolicy policy) {
        if (isExpiredMute(now)) {
            int releasedLevel = lastLevel;
            mutedUntilMillis = 0L;
            mutedReason = null;
            lastLevel = 0;
            counter.reset();
            int countAfterRelease = counter.incrementAndGet(now);
            return ModerationResult.allowed(countAfterRelease, releasedLevel);
        }

        if (isMuted(now)) {
            return ModerationResult.blocked(counter.currentCount(now), mutedUntilMillis, mutedReason, lastLevel);
        }

        int count = counter.incrementAndGet(now);

        if (count <= policy.maxMessagesInWindow) {
            return ModerationResult.allowed(count, 0);
        }

        violationCount += 1;

        RateLimitPenalty penalty = policy.penaltyFor(violationCount);

        mutedUntilMillis = penalty.permanent() ? Long.MAX_VALUE : now + penalty.muteMillis();
        mutedReason = penalty.reason();
        lastLevel = penalty.level();
        return ModerationResult.blocked(count, mutedUntilMillis, mutedReason, penalty.level());
    }

    private boolean isExpiredMute(long now) {
        return mutedUntilMillis > 0L
                && mutedUntilMillis != Long.MAX_VALUE
                && mutedUntilMillis <= now;
    }

    private boolean isMuted(long now) {
        if (mutedUntilMillis == 0L) {
            return false;
        }
        if (mutedUntilMillis == Long.MAX_VALUE) {
            return true;
        }

        return mutedUntilMillis > now;
    }
}
