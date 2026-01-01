package com.game.contraband.infrastructure.monitor;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Set;
import java.util.function.LongConsumer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!dev")
@Component
public class DbChatBlacklistRepository implements ChatBlacklistRepository {

    @Override
    public void block(Long playerId) {
        // NO-OP
    }

    @Override
    public void unblock(Long playerId) {
        // NO-OP
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
