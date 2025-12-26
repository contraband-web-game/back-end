package com.game.contraband.global.config;

import com.game.contraband.infrastructure.websocket.GameWebSocketHandler;
import com.game.contraband.infrastructure.websocket.MonitorWebSocketHandler;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final MonitorWebSocketHandler monitorWebSocketHandler;

    @Bean
    public SimpleUrlHandlerMapping webSocketHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(Map.of(
                "/ws", gameWebSocketHandler,
                "/monitor-ws", monitorWebSocketHandler
        ));
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
