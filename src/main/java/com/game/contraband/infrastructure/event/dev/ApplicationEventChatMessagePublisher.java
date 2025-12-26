package com.game.contraband.infrastructure.event.dev;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class ApplicationEventChatMessagePublisher implements ChatMessageEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(ChatMessageEvent event) {
        applicationEventPublisher.publishEvent(new ChatMessageApplicationEvent(this, event));
    }

    public static class ChatMessageApplicationEvent extends ApplicationEvent {

        private final ChatMessageEvent payload;

        public ChatMessageApplicationEvent(Object source, ChatMessageEvent payload) {
            super(source);
            this.payload = payload;
        }

        public ChatMessageEvent payload() {
            return payload;
        }
    }
}
