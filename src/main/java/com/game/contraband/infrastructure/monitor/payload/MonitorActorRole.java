package com.game.contraband.infrastructure.monitor.payload;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MonitorActorRole {
    GUARDIAN_ACTOR("GuardianActor"),
    ROOM_DIRECTORY_SUBSCRIBER_ACTOR("RoomDirectorySubscriberActor");

    private final String label;

    MonitorActorRole(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }
}
