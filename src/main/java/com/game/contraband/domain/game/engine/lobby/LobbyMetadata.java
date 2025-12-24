package com.game.contraband.domain.game.engine.lobby;

import lombok.Getter;

@Getter
public class LobbyMetadata {

    private static final int LOBBY_MIN_PLAYER_COUNT = 2;

    public static LobbyMetadata create(Long id, String name, Long hostId, int maxPlayerCount) {
        validateName(name);
        validateHost(hostId);
        validateMaxPlayerCount(maxPlayerCount);

        return new LobbyMetadata(id, name, hostId, maxPlayerCount);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("방 이름은 비어 있을 수 없습니다.");
        }
    }

    private static void validateHost(Long hostId) {
        if (hostId == null) {
            throw new IllegalArgumentException("방장은 필수입니다.");
        }
    }

    private static void validateMaxPlayerCount(int maxPlayerCount) {
        if (maxPlayerCount < LOBBY_MIN_PLAYER_COUNT || maxPlayerCount % 2 != 0) {
            throw new IllegalArgumentException("로비 최대 인원은 2명 이상이고 짝수여야 합니다.");
        }
    }

    private LobbyMetadata(Long id, String name, Long hostId, int maxPlayerCount) {
        this.id = id;
        this.name = name;
        this.hostId = hostId;
        this.maxPlayerCount = maxPlayerCount;
    }

    private final Long id;
    private final String name;
    private final Long hostId;
    private final int maxPlayerCount;

    public LobbyMetadata withMaxPlayerCount(int newMaxPlayerCount) {
        validateMaxPlayerCount(newMaxPlayerCount);

        return new LobbyMetadata(id, name, hostId, newMaxPlayerCount);
    }

    public boolean isHost(Long playerId) {
        return hostId.equals(playerId);
    }

    public int maxTeamSize() {
        return maxPlayerCount / 2;
    }
}
