package com.game.contraband.presentation.api;

import com.game.contraband.application.game.GameService;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameResumeController {

    private final GameService gameService;

    @GetMapping("/resume")
    public CompletionStage<ResponseEntity<ActiveGameResponse>> resume(@RequestParam Long userId) {
        return gameService.findActiveGame(userId)
                          .thenApply(info -> {
                                   boolean hasGame = info != null;
                                   return ResponseEntity.ok(
                                           new ActiveGameResponse(
                                                   hasGame,
                                                   hasGame ? info.roomId() : null,
                                                   hasGame ? info.entityId() : null
                                           )
                                   );
                               })
                          .exceptionally(ex -> ResponseEntity.ok(new ActiveGameResponse(false, null, null)));
    }

    public record ActiveGameResponse(boolean hasGame, Long roomId, String entityId) { }
}
