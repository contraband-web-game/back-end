package com.game.contraband.domain.common;

import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class AuditTimestamps {

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static AuditTimestamps create(LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AuditTimestamps(createdAt, updatedAt);
    }

    public static AuditTimestamps now() {
        LocalDateTime now = LocalDateTime.now();

        return new AuditTimestamps(now, now);
    }

    public AuditTimestamps updateTimestamp() {
        return new AuditTimestamps(this.createdAt, LocalDateTime.now());
    }

    private AuditTimestamps(LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

