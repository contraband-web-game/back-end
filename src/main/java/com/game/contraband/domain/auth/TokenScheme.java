package com.game.contraband.domain.auth;

public enum TokenScheme {
    BEARER("Bearer ");

    private final String prefix;

    TokenScheme(String prefix) {
        this.prefix = prefix;
    }

    public boolean startsWith(String token) {
        return token != null && token.startsWith(prefix);
    }

    public boolean doesNotStartsWith(String token) {
        return !startsWith(token);
    }

    public String parse(String token) {
        return token.substring(prefix.length());
    }
}
