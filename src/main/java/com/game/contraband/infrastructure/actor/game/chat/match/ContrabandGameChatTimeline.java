package com.game.contraband.infrastructure.actor.game.chat.match;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessages;
import com.game.contraband.infrastructure.actor.sequence.SnowflakeSequenceGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ContrabandGameChatTimeline {

    private final Long roomId;
    private final SnowflakeSequenceGenerator sequenceGenerator;
    private final ChatMessages chatMessages;

    public ContrabandGameChatTimeline(Long roomId) {
        this(roomId, new SnowflakeSequenceGenerator(roomId), new ChatMessages());
    }

    ContrabandGameChatTimeline(Long roomId, SnowflakeSequenceGenerator sequenceGenerator, ChatMessages chatMessages) {
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator, "sequenceGenerator");
        this.chatMessages = Objects.requireNonNull(chatMessages, "chatMessages");
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

    public List<ChatMessage> maskMessagesByWriter(Long writerId) {
        return chatMessages.maskMessagesByWriter(writerId);
    }
}
