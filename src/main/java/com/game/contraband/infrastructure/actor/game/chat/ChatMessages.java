package com.game.contraband.infrastructure.actor.game.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatMessages {

    private static final int MAX_SIZE = 100;

    private final ChatMessageNode head;
    private final ChatMessageNode tail;
    private final Map<Long, ChatMessageNode> messageIdMap;
    private int size;

    public ChatMessages() {
        this.head = new ChatMessageNode(null);
        this.tail = new ChatMessageNode(null);
        this.head.links().next = tail;
        this.tail.links().prev = head;
        this.messageIdMap = new HashMap<>();
        this.size = 0;
    }

    public void add(ChatMessage message) {
        validateChatMessage(message);
        addNewMessageNode(message);
        evictOldest();
    }

    public ChatMessage delete(Long messageId) {
        validateMessageId(messageId);

        ChatMessageNode node = findChatMessageNode(messageId);
        ChatMessage deletedMessage = node.data;

        removeNode(node);
        messageIdMap.remove(messageId);
        size--;

        return deletedMessage;
    }

    public void syncMessages(List<ChatMessage> newMessages) {
        if (newMessages.isEmpty()) {
            return;
        }

        List<ChatMessage> mergedMessages = mergeWithCurrentMessages(newMessages);
        List<ChatMessage> uniqueMessages = removeDuplicateMessages(mergedMessages);
        List<ChatMessage> sortedMessages = sortByMessageSequenceDescending(uniqueMessages);
        List<ChatMessage> limitedMessages = limitToMaxSize(sortedMessages);

        rebuildWithMessages(limitedMessages);
    }

    public List<ChatMessage> getHistory(Long beforeId, int size) {
        validateSize(size);

        List<ChatMessage> result = new ArrayList<>();
        ChatMessageNode current = head.links().next;

        while (current != tail && result.size() < size) {
            if (current.data.id() < beforeId) {
                result.add(current.data);
            }
            current = current.links().next;
        }

        return result;
    }

    public List<ChatMessage> getRecentMessages(int size) {
        validateSize(size);

        List<ChatMessage> result = new ArrayList<>();
        ChatMessageNode current = head.links().next;

        while (current != tail && result.size() < size) {
            result.add(current.data);
            current = current.links().next;
        }

        return result;
    }

    public List<ChatMessage> getMessagesAfter(long afterId) {
        List<ChatMessage> result = new ArrayList<>();
        ChatMessageNode current = head.links().next;

        while (current != tail) {
            if (current.data.id() > afterId) {
                result.add(current.data);
            }
            current = current.links().next;
        }

        return result;
    }

    public List<ChatMessage> getMessages() {
        List<ChatMessage> result = new ArrayList<>();
        ChatMessageNode current = head.links().next;

        while (current != tail) {
            result.add(current.data);
            current = current.links().next;
        }

        return result;
    }

    public List<ChatMessage> maskMessagesByWriter(Long writerId, String maskedText) {
        if (writerId == null || maskedText == null) {
            return List.of();
        }
        List<ChatMessage> updated = new ArrayList<>();
        ChatMessageNode current = head.links().next;
        while (current != tail) {
            ChatMessage message = current.data;
            if (writerId.equals(message.writerId()) && !maskedText.equals(message.message())) {
                ChatMessage masked = new ChatMessage(
                        message.id(),
                        message.roomId(),
                        message.writerId(),
                        message.writerName(),
                        maskedText,
                        message.createdAt()
                );
                ChatMessageNode maskedNode = new ChatMessageNode(masked);
                current.replaceWith(maskedNode);
                messageIdMap.put(masked.id(), maskedNode);
                updated.add(masked);
                current = maskedNode.links().next;
                continue;
            }
            current = current.links().next;
        }
        return updated;
    }

    private void validateChatMessage(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("메시지는 null 일 수 없습니다.");
        }
    }

    private void validateMessageId(Long messageId) {
        if (messageId == null) {
            throw new IllegalArgumentException("메시지 ID는 null 일 수 없습니다.");
        }
    }

    private void validateSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("조회하려는 메시지 개수는 양수여야 합니다.");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("조회하려는 메시지 개수는 " + MAX_SIZE + "개를 넘을 수 없습니다.");
        }
    }

    private void addNewMessageNode(ChatMessage message) {
        ChatMessageNode newNode = new ChatMessageNode(message);

        addNodeAfterHead(newNode);
        messageIdMap.put(message.id(), newNode);
        size++;
    }

    private void evictOldest() {
        if (size > MAX_SIZE) {
            ChatMessageNode oldest = tail.links().prev;

            removeNode(oldest);
            messageIdMap.remove(oldest.data.id());
            size--;
        }
    }

    private ChatMessageNode findChatMessageNode(Long messageId) {
        ChatMessageNode node = messageIdMap.get(messageId);

        if (node == null) {
            throw new IllegalArgumentException("존재하지 않는 메시지입니다: " + messageId);
        }

        return node;
    }

    private List<ChatMessage> mergeWithCurrentMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> allMessages = getAllMessages();
        allMessages.addAll(newMessages);
        return allMessages;
    }

    private List<ChatMessage> removeDuplicateMessages(List<ChatMessage> messageList) {
        Map<Long, ChatMessage> distinctMessageIdMap = messageList.stream()
                                                                 .collect(
                                                                         Collectors.toMap(
                                                                                 ChatMessage::id,
                                                                                 message -> message,
                                                                                 (existing, replacement) -> replacement
                                                                         )
                                                                 );

        return new ArrayList<>(distinctMessageIdMap.values());
    }

    private List<ChatMessage> sortByMessageSequenceDescending(List<ChatMessage> messages) {
        messages.sort(Comparator.comparingLong(ChatMessage::id).reversed());
        return messages;
    }

    private List<ChatMessage> limitToMaxSize(List<ChatMessage> messages) {
        if (messages.size() > MAX_SIZE) {
            return messages.subList(0, MAX_SIZE);
        }
        return messages;
    }

    private void rebuildWithMessages(List<ChatMessage> messages) {
        clearNodeStatus();

        for (ChatMessage message : messages) {
            ChatMessageNode newNode = new ChatMessageNode(message);
            addNodeBeforeTail(newNode);
            messageIdMap.put(message.id(), newNode);
            size++;
        }
    }

    private void addNodeAfterHead(ChatMessageNode node) {
        node.links().next = head.links().next;
        node.links().prev = head;
        head.links().next.links().prev = node;
        head.links().next = node;
    }

    private void addNodeBeforeTail(ChatMessageNode node) {
        node.links().prev = tail.links().prev;
        node.links().next = tail;
        tail.links().prev.links().next = node;
        tail.links().prev = node;
    }

    private void removeNode(ChatMessageNode node) {
        node.links().prev.links().next = node.links().next;
        node.links().next.links().prev = node.links().prev;
    }

    private void clearNodeStatus() {
        head.links().next = tail;
        tail.links().prev = head;
        messageIdMap.clear();
        size = 0;
    }

    private List<ChatMessage> getAllMessages() {
        List<ChatMessage> allMessages = new ArrayList<>();

        ChatMessageNode current = head.links().next;
        while (current != tail) {
            allMessages.add(current.data);
            current = current.links().next;
        }

        return allMessages;
    }

    private static final class Links {
        ChatMessageNode prev;
        ChatMessageNode next;
    }

    private record ChatMessageNode(ChatMessage data, Links links) {

        ChatMessageNode(ChatMessage data) {
            this(data, new Links());
        }

        void replaceWith(ChatMessageNode replacement) {
            replacement.links.prev = this.links.prev;
            replacement.links.next = this.links.next;
            this.links.prev.links.next = replacement;
            this.links.next.links.prev = replacement;
        }
    }
}
