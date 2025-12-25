package com.game.contraband.infrastructure.actor.game.chat;

public interface ChatMessageEventPublisher {

    void publish(ChatMessageEvent event);

    record ChatMessageEvent(
            String entityId,
            Long roomId,
            ChatEventType chatEvent,
            Integer round,
            ChatMessage chatMessage
    ) {
        public ChatMessageEvent {
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("entityId는 비어 있을 수 없습니다.");
            }
            if (roomId == null || roomId <= 0) {
                throw new IllegalArgumentException("roomId는 양수여야 합니다.");
            }
            if (chatEvent == null) {
                throw new IllegalArgumentException("chatEvent는 null일 수 없습니다.");
            }
            if (chatMessage == null) {
                throw new IllegalArgumentException("chatMessage는 비어 있을 수 없습니다.");
            }
        }
    }
}
