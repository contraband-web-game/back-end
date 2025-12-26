package com.game.contraband.domain.user.vo;

import java.util.Arrays;

public enum RegistrationId {

    KAKAO("kakao");

    private final String name;

    RegistrationId(String name) {
        this.name = name;
    }

    public static RegistrationId findBy(String name) {
        return Arrays.stream(RegistrationId.values())
                     .filter(id -> id.name.equalsIgnoreCase(name))
                     .findAny()
                     .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 소셜 로그인 서비스 이름이 아닙니다."));
    }

    public static boolean contains(String name) {
        return Arrays.stream(RegistrationId.values())
                     .anyMatch(registrationId -> registrationId.name.equalsIgnoreCase(name));
    }

    public static boolean notContains(String name) {
        return !contains(name);
    }
}
