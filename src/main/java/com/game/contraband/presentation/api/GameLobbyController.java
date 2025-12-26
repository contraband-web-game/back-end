package com.game.contraband.presentation.api;

import com.game.contraband.application.game.GameService;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lobbies")
@RequiredArgsConstructor
public class GameLobbyController {

    private final GameService gameService;

    @PostMapping
    public CompletionStage<ResponseEntity<Void>> createLobby(@RequestBody CreateLobbyRequest request) {
        return gameService.createLobby(
                                       request.userId(),
                                       request.hostName(),
                                       request.maxPlayerCount(),
                                       request.lobbyName()
                               )
                          .handle(
                                       (ignore, ex) -> ex == null
                                               ? ResponseEntity.accepted().build()
                                               : ResponseEntity.status(500).build()
                               );
    }

    @PostMapping("/join")
    public CompletionStage<ResponseEntity<Void>> joinLobby(@RequestBody JoinLobbyRequest request) {
        return gameService.joinLobby(request.userId(), request.playerName(), request.roomId(), request.entityId())
                          .handle(
                                       (ignore, ex) -> ex == null
                                               ? ResponseEntity.accepted().build()
                                               : ResponseEntity.status(500).build()
                               );
    }

    public record CreateLobbyRequest(Long userId, String hostName, int maxPlayerCount, String lobbyName) { }

    public record JoinLobbyRequest(Long userId, String playerName, Long roomId, String entityId) { }
}
