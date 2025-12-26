package com.game.contraband.global.auth;

import com.game.contraband.domain.auth.TokenType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("token")
public record TokenProperties(
        String accessKey,
        String refreshKey,
        String encryptionKey,
        String issuer,
        int accessExpiredSeconds,
        int refreshExpiredSeconds,
        long accessExpiredMillisSeconds,
        long refreshExpiredMillisSeconds
) {

    public String findTokenKey(TokenType tokenType) {
        if (TokenType.ACCESS == tokenType) {
            return accessKey;
        }

        return refreshKey;
    }

    public Long findExpiredMillisSeconds(TokenType tokenType) {
        if (TokenType.ACCESS == tokenType) {
            return accessExpiredMillisSeconds;
        }

        return refreshExpiredMillisSeconds;
    }
}
