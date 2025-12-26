package com.game.contraband.domain.auth;

import java.time.LocalDateTime;

public interface TokenEncoder {

    String encode(LocalDateTime targetTime, TokenType tokenType, Long memberId);
}
