package com.game.contraband.infrastructure.actor.game.chat.match;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Objects;
import java.util.function.LongConsumer;

public class ContrabandGameChatBlacklistListener {

    private final ChatBlacklistRepository repository;
    private final Runnable unsubscribe;

    public ContrabandGameChatBlacklistListener(ChatBlacklistRepository repository, LongConsumer blockedHandler) {
        Objects.requireNonNull(blockedHandler, "blockedHandler");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.unsubscribe = repository.registerListener(blockedHandler);
    }

    public boolean isBlocked(Long playerId) {
        return repository.isBlocked(playerId);
    }

    public void close() {
        if (unsubscribe != null) {
            unsubscribe.run();
        }
    }
}
