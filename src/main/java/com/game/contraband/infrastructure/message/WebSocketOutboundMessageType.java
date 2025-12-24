package com.game.contraband.infrastructure.message;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum WebSocketOutboundMessageType {

    WS_HEALTH_PING("WS_HEALTH_PING"),
    WS_RECONNECT("WS_RECONNECT"),
    HEARTBEAT_PING("PING"),
    HEARTBEAT_PONG("PONG"),
    SESSION_HEALTH_PONG("WS_PONG"),
    EXCEPTION_MESSAGE("EXCEPTION_MESSAGE");

    private final String type;

    WebSocketOutboundMessageType(String type) {
        this.type = type;
    }

    @JsonValue
    public String type() {
        return type;
    }

    public boolean isSameType(String rawType) {
        return type.equalsIgnoreCase(rawType);
    }

    public static Optional<WebSocketOutboundMessageType> from(String rawType) {
        return Arrays.stream(values())
                     .filter(value -> value.isSameType(rawType))
                     .findFirst();
    }
}
