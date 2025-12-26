package com.game.contraband.infrastructure.websocket.message;

public enum ExceptionCode {
    UNKNOWN_ERROR,

    CHAT_MESSAGE_EMPTY,
    CHAT_USER_BLOCKED,

    GAME_INVALID_STATE,

    NOT_CURRENT_ROUND_INSPECTOR,
    NOT_CURRENT_ROUND_SMUGGLER
}
