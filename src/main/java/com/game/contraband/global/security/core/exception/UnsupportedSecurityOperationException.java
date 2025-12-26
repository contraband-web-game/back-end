package com.game.contraband.global.security.core.exception;

import org.springframework.security.core.AuthenticationException;

public class UnsupportedSecurityOperationException extends AuthenticationException {

    public UnsupportedSecurityOperationException() {
        super("지원하지 않는 기능입니다.");
    }
}
