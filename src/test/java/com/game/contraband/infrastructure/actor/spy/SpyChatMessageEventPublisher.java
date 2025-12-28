package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import com.game.contraband.infrastructure.actor.game.chat.ChatEventType;
import java.util.ArrayList;
import java.util.List;

public class SpyChatMessageEventPublisher implements ChatMessageEventPublisher {

    private final List<ChatMessageEvent> published = new ArrayList<>();

    @Override
    public void publish(ChatMessageEvent event) {
        published.add(event);
    }

    public List<ChatMessageEvent> publishedEvents() {
        return List.copyOf(published);
    }

    public boolean hasEvent(ChatEventType eventType) {
        return published.stream().anyMatch(event -> event.chatEvent() == eventType);
    }
}
