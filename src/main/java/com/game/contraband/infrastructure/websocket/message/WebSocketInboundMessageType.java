package com.game.contraband.infrastructure.websocket.message;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum WebSocketInboundMessageType {

    HEARTBEAT_PING("PING"),
    SESSION_HEALTH_PONG("WS_PONG");

    private final String type;

    WebSocketInboundMessageType(String type) {
        this.type = type;
    }

    @JsonValue
    public String type() {
        return type;
    }

    public boolean isSameType(String rawType) {
        return type.equalsIgnoreCase(rawType);
    }

    public static Optional<WebSocketInboundMessageType> from(String rawType) {
        return Arrays.stream(values())
                     .filter(value -> value.isSameType(rawType))
                     .findFirst();
    }
}
