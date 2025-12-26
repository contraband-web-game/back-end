package com.game.contraband.global.security.handler;

import com.game.contraband.application.auth.GenerateTokenService;
import com.game.contraband.application.auth.LoginService;
import com.game.contraband.application.auth.dto.LoggedInUserDto;
import com.game.contraband.global.auth.TokenProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Profile("!dev && !profile")
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {

    public static final String DOMAIN = "/";
    public static final String REFRESH_TOKEN_KEY = "refreshToken";
    public static final String ACCESS_TOKEN_KEY = "accessToken";

    private final TokenProperties tokenProperties;
    private final LoginService loginService;
    private final GenerateTokenService generateTokenService;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        ServerHttpResponse response = exchange.getResponse();

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        return Mono.fromCallable(
                           () -> {
                               String socialId = oAuth2User.getAttribute(StandardClaimNames.SUB);
                               if (socialId == null) {
                                   socialId = oAuth2User.getName();
                               }

                               String registrationId = oauthToken.getAuthorizedClientRegistrationId();

                               LoggedInUserDto loggedInUserDto = loginService.login(registrationId, socialId);
                               return generateTokenService.generate(loggedInUserDto.id());
                           }
                   )
                   .subscribeOn(Schedulers.boundedElastic())
                   .flatMap(
                           tokenDto -> {
                               createRefreshTokenCookie(response, tokenDto.refreshToken());
                               createAccessTokenCookie(response, tokenDto.accessToken());

                               return writeResponse(response);
                           }
                   );
    }

    private Mono<Void> writeResponse(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.CREATED);
        response.getHeaders().setContentType(MediaType.TEXT_HTML);

        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset='UTF-8'></head>
                <body>
                <script>
                  if (window.opener) {
                    window.opener.postMessage({type: 'LOGIN_SUCCESS'}, '*');
                  }
                  window.close();
                </script>
                </body>
                </html>
                """;

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    private void createRefreshTokenCookie(ServerHttpResponse response, String refreshToken) {
        String encodedToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_KEY, encodedToken)
                                              .path(DOMAIN)
                                              .maxAge(tokenProperties.refreshExpiredSeconds())
                                              .secure(false)
                                              .sameSite("Lax")
                                              .httpOnly(true)
                                              .build();

        response.addCookie(cookie);
    }

    private void createAccessTokenCookie(ServerHttpResponse response, String accessToken) {
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_KEY, encodedToken)
                                              .path(DOMAIN)
                                              .maxAge(tokenProperties.accessExpiredSeconds())
                                              .secure(false)
                                              .sameSite("Lax")
                                              .httpOnly(true)
                                              .build();

        response.addCookie(cookie);
    }
}
