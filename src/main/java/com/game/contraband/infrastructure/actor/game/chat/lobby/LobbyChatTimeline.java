package com.game.contraband.infrastructure.actor.game.chat.lobby;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessages;
import com.game.contraband.infrastructure.actor.sequence.SnowflakeSequenceGenerator;
import java.time.LocalDateTime;
import java.util.List;

public class LobbyChatTimeline {

    private final Long roomId;
    private final SnowflakeSequenceGenerator sequenceGenerator;
    private final ChatMessages chatMessages;

    public LobbyChatTimeline(Long roomId) {
        this(roomId, new SnowflakeSequenceGenerator(roomId), new ChatMessages());
    }

    LobbyChatTimeline(Long roomId, SnowflakeSequenceGenerator sequenceGenerator, ChatMessages chatMessages) {
        this.roomId = roomId;
        this.sequenceGenerator = sequenceGenerator;
        this.chatMessages = chatMessages;
    }

    public ChatMessage append(Long writerId, String writerName, String message) {
        ChatMessage chatMessage = new ChatMessage(
                sequenceGenerator.nextSequence(),
                roomId,
                writerId,
                writerName,
                message,
                LocalDateTime.now()
        );
        chatMessages.add(chatMessage);
        return chatMessage;
    }

    public List<ChatMessage> maskMessagesByWriter(Long playerId, String replacement) {
        return chatMessages.maskMessagesByWriter(playerId, replacement);
    }
}
