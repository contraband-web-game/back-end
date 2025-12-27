package com.game.contraband.infrastructure.actor.dummy;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;

public class DummyChatMessageEventPublisher implements ChatMessageEventPublisher {

    @Override
    public void publish(ChatMessageEvent event) {
    }
}
