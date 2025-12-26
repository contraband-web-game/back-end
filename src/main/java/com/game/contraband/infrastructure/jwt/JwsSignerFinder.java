package com.game.contraband.infrastructure.jwt;

import com.game.contraband.domain.auth.TokenType;
import com.nimbusds.jose.JWSSigner;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwsSignerFinder {

    private final JWSSigner accessTokenSigner;
    private final JWSSigner refreshTokenSigner;

    public JWSSigner findByTokenType(TokenType tokenType) {
        if (TokenType.ACCESS == tokenType) {
            return accessTokenSigner;
        }

        return refreshTokenSigner;
    }
}
