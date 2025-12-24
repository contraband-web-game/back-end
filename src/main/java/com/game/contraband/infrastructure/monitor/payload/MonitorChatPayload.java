package com.game.contraband.infrastructure.monitor.payload;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;

public record MonitorChatPayload(
        String entityId,
        Long roomId,
        Integer round,
        String chatEvent,
        ChatMessage chatMessage
) { }
