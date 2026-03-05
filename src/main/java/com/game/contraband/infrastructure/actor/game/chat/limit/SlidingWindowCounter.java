package com.game.contraband.infrastructure.actor.game.chat.limit;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowCounter {

    private final long windowMillis;
    private final Deque<Long> times = new ArrayDeque<>();

    public SlidingWindowCounter(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    int incrementAndGet(long nowMillis) {
        evictOld(nowMillis);
        times.addLast(nowMillis);
        return times.size();
    }

    int currentCount(long nowMillis) {
        evictOld(nowMillis);
        return times.size();
    }

    void reset() {
        times.clear();
    }

    private void evictOld(long nowMillis) {
        long threshold = nowMillis - windowMillis;
        while (!times.isEmpty() && times.peekFirst() < threshold) {
            times.removeFirst();
        }
    }
}
