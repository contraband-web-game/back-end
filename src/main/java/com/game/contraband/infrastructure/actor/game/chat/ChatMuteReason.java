package com.game.contraband.infrastructure.actor.game.chat;

import lombok.Getter;

@Getter
public enum ChatMuteReason {

    NONE(false),
    ADMIN_BLOCKED(false),
    LEVEL1(true),
    LEVEL2(true),
    PERMANENT(true);

    private final boolean rateLimitReason;

    ChatMuteReason(boolean rateLimitReason) {
        this.rateLimitReason = rateLimitReason;
    }

    public boolean isAdminBlocked() {
        return this == ADMIN_BLOCKED;
    }
}
