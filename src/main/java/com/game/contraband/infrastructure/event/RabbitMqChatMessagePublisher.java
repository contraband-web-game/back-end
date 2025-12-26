package com.game.contraband.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessageEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("rabbit-mq")
@Component
@RequiredArgsConstructor
public class RabbitMqChatMessagePublisher implements ChatMessageEventPublisher {

    private static final String ROUTING_KEY = "monitor.chat";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(ChatMessageEvent event) {
        try {
            rabbitTemplate.convertAndSend("", ROUTING_KEY, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("채팅 이벤트 직렬화에 실패했습니다.", e);
        }
    }
}
