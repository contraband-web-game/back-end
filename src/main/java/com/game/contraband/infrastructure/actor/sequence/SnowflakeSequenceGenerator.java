package com.game.contraband.infrastructure.actor.sequence;

import java.time.Clock;

public class SnowflakeSequenceGenerator {

    // Custom Epoch (2025-01-01T00:00:00Z)
    private static final long CUSTOM_EPOCH = 1735689600000L;

    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_CHANNEL_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private static final int TIMESTAMP_SHIFT = NODE_ID_BITS + SEQUENCE_BITS;
    private static final int NODE_ID_SHIFT = SEQUENCE_BITS;

    private final Long entityId;
    private final Clock clock;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeSequenceGenerator(Long entityId) {
        this(entityId, Clock.systemUTC());
    }

    public SnowflakeSequenceGenerator(Long entityId, Clock clock) {
        if (entityId < 0) {
            throw new IllegalArgumentException("엔티티 식별자는 양수여야 합니다.");
        }

        this.entityId = entityId & MAX_CHANNEL_ID;
        this.clock = clock;
    }

    public Long nextSequence() {
        long currentTimestamp = clock.millis();

        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            throw new IllegalStateException(
                    String.format(
                            "시계가 %dms 역행했습니다. 마지막: %d, 현재: %d",
                            offset,
                            lastTimestamp,
                            currentTimestamp
                    )
            );
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0) {
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (entityId << NODE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = clock.millis();

        while (timestamp <= lastTimestamp) {
            timestamp = clock.millis();
        }

        return timestamp;
    }
}
