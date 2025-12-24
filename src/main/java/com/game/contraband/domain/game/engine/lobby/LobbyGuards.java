package com.game.contraband.domain.game.engine.lobby;

public class LobbyGuards {

    public static LobbyGuards create() {
        return new LobbyGuards();
    }

    private LobbyGuards() {
    }

    public void requireLobbyPhase(LobbyPhase phase) {
        if (phase != LobbyPhase.LOBBY) {
            throw new IllegalStateException("로비 상태에서만 로스터를 수정할 수 있습니다.");
        }
    }

    public void requireHost(Long executorId, Long hostId) {
        requireHost(executorId, hostId, "방장이 아닙니다.");
    }

    public void requireHost(Long executorId, Long hostId, String message) {
        if (!hostId.equals(executorId)) {
            throw new IllegalArgumentException(message);
        }
    }
}
