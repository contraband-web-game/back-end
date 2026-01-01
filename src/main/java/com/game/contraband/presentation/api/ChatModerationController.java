package com.game.contraband.presentation.api;

import com.game.contraband.application.manage.ChatModerationService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/chat/moderation")
public class ChatModerationController {

    private final ChatModerationService chatModerationService;

    public ChatModerationController(ChatModerationService chatModerationService) {
        this.chatModerationService = chatModerationService;
    }

    @PostMapping("/blacklist/{playerId}")
    public ResponseEntity<Void> blockPlayer(@PathVariable Long playerId) {
        chatModerationService.blockPlayer(playerId);
        return ResponseEntity.accepted()
                             .build();
    }

    @GetMapping("/blacklist")
    public ResponseEntity<Iterable<Long>> listBlacklist() {
        return ResponseEntity.ok(chatModerationService.listBlockedPlayers());
    }
}
