package com.game.contraband.infrastructure.actor.game.chat;

import java.time.LocalDateTime;

public record ChatMessage(
        Long id,
        Long roomId,
        Long writerId,
        String writerName,
        String message,
        LocalDateTime createdAt,
        boolean masked
) {
    public ChatMessage(Long id, Long roomId, Long writerId, String writerName, String message, LocalDateTime createdAt) {
        this(id, roomId, writerId, writerName, message, createdAt, false);
    }

    public ChatMessage {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("GameRoom ID는 양수여야 합니다.");
        }

        if (writerId == null || writerId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }

        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("메시지는 비어 있을 수 없습니다.");
        }
    }

    public ChatMessage markMasked() {
        return new ChatMessage(id, roomId, writerId, writerName, message, createdAt, true);
    }
}
