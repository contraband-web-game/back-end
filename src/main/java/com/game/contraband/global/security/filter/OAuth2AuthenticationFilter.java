package com.game.contraband.global.security.filter;

import com.game.contraband.global.security.core.OAuth2AuthenticationToken;
import com.game.contraband.global.security.core.OAuth2UserDetails;
import com.game.contraband.global.security.core.OAuth2UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Profile("!dev && !test")
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFilter implements WebFilter {

    private static final String TOKEN_SCHEME = "Bearer ";
    private final OAuth2UserDetailsService oAuth2UserDetailsService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return extractToken(exchange).map(this::parseToken)
                                     .flatMap(oAuth2UserDetailsService::findByUsername)
                                     .cast(OAuth2UserDetails.class)
                                     .flatMap(
                                             userDetails -> {
                                                 setAuthentication(exchange, userDetails);

                                                 return chain.filter(exchange);
                                             }
                                     )
                                     .switchIfEmpty(chain.filter(exchange));
    }

    private void setAuthentication(ServerWebExchange exchange, OAuth2UserDetails userDetails) {
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                userDetails,
                userDetails.getAuthorities()
        );
        exchange.getAttributes().put("authentication", authentication);
    }

    private Mono<String> extractToken(ServerWebExchange exchange) {
        return Mono.justOrEmpty(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION)
        );
    }

    private String parseToken(String token) {
        if (!token.startsWith(TOKEN_SCHEME)) {
            throw new InvalidTokenTypeException();
        }

        return token.substring(TOKEN_SCHEME.length());
    }

    public static class InvalidTokenTypeException extends IllegalArgumentException {

        public InvalidTokenTypeException() {
            super("Bearer 타입의 토큰이 아닙니다.");
        }
    }
}
