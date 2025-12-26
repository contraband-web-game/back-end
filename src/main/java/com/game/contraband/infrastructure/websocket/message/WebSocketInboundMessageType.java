package com.game.contraband.infrastructure.websocket.message;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Optional;

public enum WebSocketInboundMessageType {

    HEARTBEAT_PING("PING"),
    SESSION_HEALTH_PONG("WS_PONG"),
    DELETE_LOBBY("DELETE_LOBBY"),

    TOGGLE_READY("TOGGLE_READY"),
    TOGGLE_TEAM("TOGGLE_TEAM"),
    CHANGE_MAX_PLAYER_COUNT("CHANGE_MAX_PLAYER_COUNT"),
    START_GAME("START_GAME"),
    LEAVE_LOBBY("LEAVE_LOBBY"),
    KICK_PLAYER("KICK_PLAYER"),
    SEND_CHAT("SEND_CHAT"),
    SEND_ROUND_CHAT("SEND_ROUND_CHAT"),
    SEND_TEAM_CHAT("SEND_TEAM_CHAT"),
    FIX_SMUGGLER_ID("FIX_SMUGGLER_ID"),
    FIX_INSPECTOR_ID("FIX_INSPECTOR_ID"),
    QUERY_ROOM_DIRECTORY("QUERY_ROOM_DIRECTORY"),

    DECIDE_SMUGGLE_AMOUNT("DECIDE_SMUGGLE_AMOUNT"),
    DECIDE_PASS("DECIDE_PASS"),
    DECIDE_INSPECTION("DECIDE_INSPECTION"),
    REGISTER_SMUGGLER("REGISTER_SMUGGLER"),
    REGISTER_INSPECTOR("REGISTER_INSPECTOR"),
    TRANSFER_MONEY("TRANSFER_MONEY");

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
