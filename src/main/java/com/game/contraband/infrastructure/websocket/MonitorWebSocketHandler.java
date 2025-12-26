package com.game.contraband.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.contraband.infrastructure.event.MonitorEventBroadcaster;
import com.game.contraband.infrastructure.monitor.payload.MonitorMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MonitorWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final MonitorEventBroadcaster broadcaster;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<WebSocketMessage> outbound = broadcaster.flux()
                                                     .flatMap(this::serialize)
                                                     .map(session::textMessage);
        return session.send(outbound);
    }

    private Mono<String> serialize(MonitorMessage payload) {
        try {
            return Mono.just(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }
}
