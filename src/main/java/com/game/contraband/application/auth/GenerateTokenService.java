package com.game.contraband.application.auth;

import com.game.contraband.application.auth.dto.TokenDto;
import com.game.contraband.domain.auth.PrivateClaims;
import com.game.contraband.domain.auth.TokenDecoder;
import com.game.contraband.domain.auth.TokenEncoder;
import com.game.contraband.domain.auth.TokenScheme;
import com.game.contraband.domain.auth.TokenType;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateTokenService {

    private final Clock clock;
    private final TokenDecoder tokenDecoder;
    private final TokenEncoder tokenEncoder;

    public TokenDto generate(Long userId) {
        String accessToken = tokenEncoder.encode(LocalDateTime.now(clock), TokenType.ACCESS, userId);
        String refreshToken = tokenEncoder.encode(LocalDateTime.now(clock), TokenType.REFRESH, userId);

        return new TokenDto(accessToken, refreshToken, TokenScheme.BEARER.name());
    }

    public TokenDto refreshToken(String refreshToken) {
        PrivateClaims privateClaims = tokenDecoder.decode(TokenType.REFRESH, refreshToken);

        return generate(privateClaims.userId());
    }
}
