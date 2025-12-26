package com.game.contraband.global.security.dto.response;

public record LoginResponse(String accessToken, String tokenScheme, boolean isSignUp) {
}
