package com.game.contraband.infrastructure.event.dev;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher.ChatMessageEvent;
import com.game.contraband.infrastructure.event.MonitorEventBroadcaster;
import com.game.contraband.infrastructure.event.MonitorEventType;
import com.game.contraband.infrastructure.event.dev.ApplicationEventChatMessagePublisher.ChatMessageApplicationEvent;
import com.game.contraband.infrastructure.monitor.payload.MonitorChatPayload;
import com.game.contraband.infrastructure.monitor.payload.MonitorMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class MonitorChatEventListener implements ApplicationListener<ChatMessageApplicationEvent> {

    private final MonitorEventBroadcaster broadcaster;

    @Override
    public void onApplicationEvent(ChatMessageApplicationEvent event) {
        ChatMessageEvent payload = event.payload();
        MonitorChatPayload monitorPayload = new MonitorChatPayload(
                payload.entityId(),
                payload.roomId(),
                payload.round(),
                payload.chatEvent(),
                payload.chatMessage()
        );

        broadcaster.publish(new MonitorMessage(MonitorEventType.CHAT_EVENT, monitorPayload));
    }
}
