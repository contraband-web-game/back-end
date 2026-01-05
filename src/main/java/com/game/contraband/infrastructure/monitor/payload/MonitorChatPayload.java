package com.game.contraband.infrastructure.monitor.payload;

import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher.ChatRoundInfo;

public record MonitorChatPayload(
        String entityId,
        Long roomId,
        ChatRoundInfo round,
        ChatEventType chatEvent,
        ChatMessage chatMessage
) { }
