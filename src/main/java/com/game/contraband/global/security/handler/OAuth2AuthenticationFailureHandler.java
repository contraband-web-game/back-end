package com.game.contraband.global.security.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.contraband.global.security.dto.response.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Profile("!dev && !test")
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ExceptionResponse responseBody = new ExceptionResponse(exception.getMessage());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseBody);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
