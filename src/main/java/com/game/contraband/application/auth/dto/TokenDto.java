package com.game.contraband.application.auth.dto;

public record TokenDto(String accessToken, String refreshToken, String tokenScheme) {
}
