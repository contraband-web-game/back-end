package com.game.contraband.domain.user.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Nickname {

    public static final Nickname EMPTY_NICKNAME = new Nickname("익명");
    private static final int MAX_LENGTH = 10;

    private final String value;

    public static Nickname create(String value) {
        validateValue(value);

        return new Nickname(value);
    }

    private static void validateValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 " + MAX_LENGTH + "자를 초과할 수 없습니다.");
        }
    }

    private Nickname(String value) {
        this.value = value;
    }
}
