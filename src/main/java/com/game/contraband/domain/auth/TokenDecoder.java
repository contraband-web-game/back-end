package com.game.contraband.domain.auth;

public interface TokenDecoder {

    PrivateClaims decode(TokenType tokenType, String token);
}
