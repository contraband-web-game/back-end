package com.game.contraband.application.auth.dto;

public record LoggedInUserDto(Long id, String nickname, boolean isSignUp) {
}
