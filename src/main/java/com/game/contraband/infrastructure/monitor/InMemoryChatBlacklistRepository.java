package com.game.contraband.infrastructure.monitor;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongConsumer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
public class InMemoryChatBlacklistRepository implements ChatBlacklistRepository {

    private final Map<Long, Boolean> blacklist = new ConcurrentHashMap<>();
    private final Set<LongConsumer> listeners = ConcurrentHashMap.newKeySet();

    @Override
    public void block(Long playerId) {
        if (playerId == null) {
            return;
        }
        blacklist.put(playerId, Boolean.TRUE);
        notifyListeners(playerId);
    }

    @Override
    public void unblock(Long playerId) {
        if (playerId == null) {
            return;
        }
        blacklist.remove(playerId);
    }

    @Override
    public boolean isBlocked(Long playerId) {
        return playerId != null && blacklist.containsKey(playerId);
    }

    @Override
    public Runnable registerListener(LongConsumer listener) {
        if (listener == null) {
            return () -> { };
        }
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public Set<Long> findAll() {
        return Set.copyOf(blacklist.keySet());
    }

    private void notifyListeners(Long playerId) {
        listeners.forEach(
                listener -> {
                    try {
                        listener.accept(playerId);
                    } catch (Exception ignored) {
                        // 개별 리스너 예외 무시
                    }
                }
        );
    }
}
