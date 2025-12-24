package com.game.contraband.infrastructure.monitor.payload;

public record MonitorMessage(
        String type,
        Object payload
) { }
