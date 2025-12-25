package com.game.contraband.infrastructure.monitor.payload;

import com.game.contraband.infrastructure.event.MonitorEventType;

public record MonitorMessage(
        MonitorEventType type,
        Object payload
) { }
