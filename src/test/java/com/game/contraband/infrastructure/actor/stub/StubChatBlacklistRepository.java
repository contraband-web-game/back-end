package com.game.contraband.infrastructure.actor.stub;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.HashSet;
import java.util.Set;

public class StubChatBlacklistRepository implements ChatBlacklistRepository {

    private final Set<Long> blocked = new HashSet<>();
    private Runnable listener = () -> { };

    @Override
    public void block(Long playerId) {
        blocked.add(playerId);
        listener.run();
    }

    @Override
    public void unblock(Long playerId) {
        blocked.remove(playerId);
        listener.run();
    }

    @Override
    public boolean isBlocked(Long playerId) {
        return blocked.contains(playerId);
    }

    @Override
    public Runnable registerListener(java.util.function.LongConsumer listener) {
        this.listener = () -> listener.accept(0L);
        return () -> this.listener = () -> { };
    }

    @Override
    public Set<Long> findAll() {
        return blocked;
    }

}
