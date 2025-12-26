package com.game.contraband.domain.auth;

import java.time.LocalDateTime;

public record PrivateClaims(Long userId, LocalDateTime issuedAt) {
}
