package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessages;
import java.util.ArrayList;
import java.util.List;

public class SpyChatMessages extends ChatMessages {

    private final List<ChatMessage> added = new ArrayList<>();
    private List<ChatMessage> messages = List.of();
    private List<ChatMessage> maskedToReturn = List.of();

    public void initMessages(List<ChatMessage> messagesToReturn, List<ChatMessage> maskedMessages) {
        this.messages = messagesToReturn;
        this.maskedToReturn = maskedMessages;
    }

    public List<ChatMessage> addedMessages() {
        return List.copyOf(added);
    }

    @Override
    public void add(ChatMessage message) {
        added.add(message);
    }

    @Override
    public List<ChatMessage> getMessages() {
        return messages;
    }

    @Override
    public List<ChatMessage> maskMessagesByWriter(Long writerId) {
        return maskedToReturn;
    }
}
