package com.game.contraband.infrastructure.monitor.payload;

public record MonitorLifecyclePayload(
        String entityId,
        Long roomId,
        String lifecycleType
) { }
