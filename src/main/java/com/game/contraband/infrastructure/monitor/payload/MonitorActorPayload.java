package com.game.contraband.infrastructure.monitor.payload;

public record MonitorActorPayload(
        String actorPath,
        String parentPath,
        MonitorActorRole role,
        MonitorActorState state
) { }
