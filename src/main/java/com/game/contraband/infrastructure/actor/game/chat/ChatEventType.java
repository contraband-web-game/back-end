package com.game.contraband.infrastructure.actor.game.chat;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatEventType {
    LOBBY_CHAT,
    TEAM_CHAT,
    SMUGGLER_TEAM_CHAT,
    INSPECTOR_TEAM_CHAT,
    ROUND_CHAT;

    @JsonValue
    public String value() {
        return name();
    }
}
