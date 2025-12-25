package com.game.contraband.infrastructure.websocket.message;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum WebSocketOutboundMessageType {

    WS_HEALTH_PING("WS_HEALTH_PING"),
    WS_RECONNECT("WS_RECONNECT"),
    HEARTBEAT_PING("PING"),
    HEARTBEAT_PONG("PONG"),
    SESSION_HEALTH_PONG("WS_PONG"),
    EXCEPTION_MESSAGE("EXCEPTION_MESSAGE"),

    ROOM_DIRECTORY_UPDATED("ROOM_DIRECTORY_UPDATED"),

    CHAT_WELCOME("CHAT_WELCOME"),
    LOBBY_CHAT_MESSAGE("LOBBY_CHAT_MESSAGE"),
    CHAT_LEFT("CHAT_LEFT"),
    CHAT_KICKED("CHAT_KICKED"),
    CHAT_MESSAGE_MASKED("CHAT_MESSAGE_MASKED"),

    SMUGGLER_TEAM_CHAT_MESSAGE("SMUGGLER_TEAM_CHAT_MESSAGE"),
    INSPECTOR_TEAM_CHAT_MESSAGE("INSPECTOR_TEAM_CHAT_MESSAGE"),
    ROUND_CHAT_MESSAGE("ROUND_CHAT_MESSAGE");

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
