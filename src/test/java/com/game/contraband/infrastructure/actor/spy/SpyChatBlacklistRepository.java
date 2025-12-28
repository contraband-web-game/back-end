package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Set;
import java.util.function.LongConsumer;

public class SpyChatBlacklistRepository implements ChatBlacklistRepository {

    private boolean blockedReturn;
    private boolean unsubscribeInvoked;
    private LongConsumer registeredHandler;
    private Long lastCheckedPlayerId;

    public void setBlockedReturn(boolean blockedReturn) {
        this.blockedReturn = blockedReturn;
    }

    public boolean isUnsubscribeInvoked() {
        return unsubscribeInvoked;
    }

    public LongConsumer registeredHandler() {
        return registeredHandler;
    }

    public Long lastCheckedPlayerId() {
        return lastCheckedPlayerId;
    }

    @Override
    public void block(Long playerId) {
    }

    @Override
    public void unblock(Long playerId) {
    }

    @Override
    public boolean isBlocked(Long playerId) {
        lastCheckedPlayerId = playerId;
        return blockedReturn;
    }

    @Override
    public Runnable registerListener(LongConsumer listener) {
        registeredHandler = listener;
        return () -> unsubscribeInvoked = true;
    }

    @Override
    public Set<Long> findAll() {
        return Set.of();
    }
}
