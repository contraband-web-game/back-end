package com.game.contraband.infrastructure.message;

import com.fasterxml.jackson.annotation.JsonValue;

public interface WebSocketMessagePayload {

    enum WebSocketEmptyPayload implements WebSocketMessagePayload {
        INSTANCE;

        @JsonValue
        public Object value() {
            return null;
        }
    }

    record ExceptionMessagePayload(ExceptionCode code, String exceptionMessage) implements WebSocketMessagePayload { }
}
