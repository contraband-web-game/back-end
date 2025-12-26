package com.game.contraband.global.security.core;

import com.game.contraband.domain.auth.PrivateClaims;
import com.game.contraband.domain.auth.TokenDecoder;
import com.game.contraband.domain.auth.TokenType;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Profile("!dev && !test")
@Component
@RequiredArgsConstructor
public class OAuth2UserDetailsService implements ReactiveUserDetailsService {

    private final TokenDecoder tokenDecoder;

    @Override
    public Mono<UserDetails> findByUsername(String token) {
        return Mono.fromCallable(
                () -> {
                    PrivateClaims privateClaims = tokenDecoder.decode(TokenType.ACCESS, token);

                    return convert(privateClaims);
                }
        );
    }

    private OAuth2UserDetails convert(PrivateClaims privateClaims) {
        return new OAuth2UserDetails(
                privateClaims.userId(),
                Set.of(new SimpleGrantedAuthority(String.valueOf(privateClaims.userId())))
        );
    }
}
