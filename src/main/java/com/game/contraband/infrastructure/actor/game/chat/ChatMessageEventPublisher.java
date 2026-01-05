package com.game.contraband.infrastructure.actor.game.chat;

import lombok.Getter;

public interface ChatMessageEventPublisher {

    void publish(ChatMessageEvent event);

    record ChatMessageEvent(
            String entityId,
            Long roomId,
            ChatEventType chatEvent,
            ChatRoundInfo round,
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
                throw new IllegalArgumentException("chatEvent는 비어 있을 수 없습니다.");
            }
            if (round == null) {
                throw new IllegalArgumentException("round는 비어 있을 수 없습니다.");
            }
            if (chatMessage == null) {
                throw new IllegalArgumentException("chatMessage는 비어 있을 수 없습니다.");
            }
        }
    }

    @Getter
    class ChatRoundInfo {

        private static final ChatRoundInfo LOBBY = new ChatRoundInfo(Kind.LOBBY, 0);

        public enum Kind {
            LOBBY,
            MATCH
        }

        private final Kind kind;
        private final int round;

        private ChatRoundInfo(Kind kind, int round) {
            if (kind == null) {
                throw new IllegalArgumentException("kind는 null일 수 없습니다.");
            }
            if (kind == Kind.LOBBY && round != 0) {
                throw new IllegalArgumentException("LOBBY 라운드는 0이어야 합니다.");
            }
            if (kind == Kind.MATCH && round <= 0) {
                throw new IllegalArgumentException("MATCH 라운드는 양수여야 합니다.");
            }
            this.kind = kind;
            this.round = round;
        }

        public static ChatRoundInfo lobby() {
            return LOBBY;
        }

        public static ChatRoundInfo matchRound(int round) {
            return new ChatRoundInfo(Kind.MATCH, round);
        }

        public boolean isMatchRound() {
            return kind == Kind.MATCH;
        }

        public boolean isLobby() {
            return kind == Kind.LOBBY;
        }
    }
}
