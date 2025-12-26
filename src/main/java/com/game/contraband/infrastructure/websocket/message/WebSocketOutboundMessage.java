package com.game.contraband.infrastructure.websocket.message;

import com.game.contraband.infrastructure.websocket.message.WebSocketMessagePayload.WebSocketEmptyPayload;

public record WebSocketOutboundMessage(WebSocketOutboundMessageType type, WebSocketMessagePayload payload) {

    public static WebSocketOutboundMessage PING_MESSAGE = new WebSocketOutboundMessage(
            WebSocketOutboundMessageType.WS_HEALTH_PING,
            WebSocketEmptyPayload.INSTANCE
    );
    public static WebSocketOutboundMessage RECONNECT_MESSAGE = new WebSocketOutboundMessage(
            WebSocketOutboundMessageType.WS_RECONNECT,
            WebSocketEmptyPayload.INSTANCE
    );
    public static WebSocketOutboundMessage DECIDED_INSPECTOR_BEHAVIOR_FOR_SMUGGLER_TEAM = new WebSocketOutboundMessage(
            WebSocketOutboundMessageType.DECIDED_INSPECTOR_BEHAVIOR_FOR_SMUGGLER_TEAM,
            WebSocketEmptyPayload.INSTANCE
    );
    public static WebSocketOutboundMessage DECIDED_SMUGGLER_AMOUNT_FOR_INSPECTOR_TEAM = new WebSocketOutboundMessage(
            WebSocketOutboundMessageType.DECIDED_SMUGGLER_AMOUNT_FOR_INSPECTOR_TEAM,
            WebSocketEmptyPayload.INSTANCE
    );

    public static WebSocketOutboundMessage withoutPayload(WebSocketOutboundMessageType type) {
        return new WebSocketOutboundMessage(type, WebSocketEmptyPayload.INSTANCE);
    }
}
