package com.game.contraband.infrastructure.actor.dummy;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Set;
import java.util.function.LongConsumer;

public class DummyChatBlacklistRepository implements ChatBlacklistRepository {

    @Override
    public void block(Long playerId) {
    }

    @Override
    public void unblock(Long playerId) {
    }

    @Override
    public boolean isBlocked(Long playerId) {
        return false;
    }

    @Override
    public Runnable registerListener(LongConsumer listener) {
        return null;
    }

    @Override
    public Set<Long> findAll() {
        return Set.of();
    }
}
