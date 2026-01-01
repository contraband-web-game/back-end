package com.game.contraband.application.manage;

import com.game.contraband.domain.monitor.ChatBlacklistRepository;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatModerationService {

    private final ChatBlacklistRepository chatBlacklistRepository;

    @Transactional
    public void blockPlayer(Long playerId) {
        chatBlacklistRepository.block(playerId);
    }

    public Iterable<Long> listBlockedPlayers() {
        return Optional.ofNullable(chatBlacklistRepository.findAll())
                       .orElse(Collections.emptySet());
    }
}
