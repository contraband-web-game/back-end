package com.game.contraband.domain.monitor;

import java.util.Set;
import java.util.function.LongConsumer;

public interface ChatBlacklistRepository {

    void block(Long playerId);

    void unblock(Long playerId);

    boolean isBlocked(Long playerId);

    Runnable registerListener(LongConsumer listener);

    Set<Long> findAll();
}
