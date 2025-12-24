package com.game.contraband.infrastructure.message;

import com.game.contraband.infrastructure.message.WebSocketMessagePayload.WebSocketEmptyPayload;

public record WebSocketOutboundMessage(WebSocketOutboundMessageType type, WebSocketMessagePayload payload) {

    public static WebSocketOutboundMessage PING_MESSAGE = new WebSocketOutboundMessage(
            WebSocketOutboundMessageType.WS_HEALTH_PING,
            WebSocketEmptyPayload.INSTANCE
    );
    public static WebSocketOutboundMessage RECONNECT_MESSAGE = new WebSocketOutboundMessage(
            WebSocketOutboundMessageType.WS_RECONNECT,
            WebSocketEmptyPayload.INSTANCE
    );

    public static WebSocketOutboundMessage withoutPayload(WebSocketOutboundMessageType type) {
        return new WebSocketOutboundMessage(type, WebSocketEmptyPayload.INSTANCE);
    }
}
