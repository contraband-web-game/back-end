package com.game.contraband.global.security.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.contraband.domain.user.vo.RegistrationId;
import com.game.contraband.global.security.dto.response.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Profile("!dev && !test")
@Component
@RequiredArgsConstructor
public class OAuth2RegistrationValidateFilter implements WebFilter {

    private static final String AUTHORIZE_URI = "/login";
    private static final String REQUEST_DELIMITER = "/";

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (!path.contains(AUTHORIZE_URI)) {
            return chain.filter(exchange);
        }

        String[] splitRequestUri = path.split(REQUEST_DELIMITER);

        if (splitRequestUri.length == 0) {
            return chain.filter(exchange);
        }

        String registrationId = splitRequestUri[splitRequestUri.length - 1];

        if (RegistrationId.notContains(registrationId)) {
            return sendErrorResponse(exchange, "지원하지 않는 소셜 로그인 방식입니다.");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ExceptionResponse exceptionResponse = new ExceptionResponse(message);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(exceptionResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
