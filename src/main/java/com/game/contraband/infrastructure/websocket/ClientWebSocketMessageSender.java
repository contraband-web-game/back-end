package com.game.contraband.infrastructure.websocket;

import com.game.contraband.infrastructure.message.ExceptionCode;
import com.game.contraband.infrastructure.message.WebSocketMessagePayload.ExceptionMessagePayload;
import com.game.contraband.infrastructure.message.WebSocketOutboundMessage;
import com.game.contraband.infrastructure.message.WebSocketOutboundMessageType;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Sinks.Many;

public class ClientWebSocketMessageSender {

    private final AtomicReference<Many<WebSocketOutboundMessage>> sinkHolder;

    public ClientWebSocketMessageSender() {
        this(null);
    }

    public ClientWebSocketMessageSender(Many<WebSocketOutboundMessage> sink) {
        this.sinkHolder = new AtomicReference<>(sink);
    }

    public void attachSink(Many<WebSocketOutboundMessage> sink) {
        sinkHolder.set(sink);
    }

    public void detachSink(Many<WebSocketOutboundMessage> sink) {
        sinkHolder.compareAndSet(sink, null);
    }

    public void sendExceptionMessage(ExceptionCode code, String exceptionMessage) {
        ExceptionCode resolvedCode = code == null ? ExceptionCode.UNKNOWN_ERROR : code;
        ExceptionMessagePayload payload = new ExceptionMessagePayload(resolvedCode, exceptionMessage);
        WebSocketOutboundMessage webSocketOutboundMessage = new WebSocketOutboundMessage(
                WebSocketOutboundMessageType.EXCEPTION_MESSAGE,
                payload
        );

        emit(webSocketOutboundMessage);
    }

    public void sendWebSocketPing() {
        emit(WebSocketOutboundMessage.PING_MESSAGE);
    }

    public void requestSessionReconnect() {
        emit(WebSocketOutboundMessage.RECONNECT_MESSAGE);
    }

    private void emit(WebSocketOutboundMessage payload) {
        Many<WebSocketOutboundMessage> sink = sinkHolder.get();

        if (sink != null) {
            sink.tryEmitNext(payload);
        }
    }
}
