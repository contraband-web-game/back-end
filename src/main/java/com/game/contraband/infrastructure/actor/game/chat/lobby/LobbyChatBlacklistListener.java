package com.game.contraband.infrastructure.actor.game.chat.lobby;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Objects;
import java.util.function.LongConsumer;

public class LobbyChatBlacklistListener {

    private final ChatBlacklistRepository repository;
    private final Runnable unsubscribe;

    public LobbyChatBlacklistListener(ChatBlacklistRepository repository, LongConsumer blockedHandler) {
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
