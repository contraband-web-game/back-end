package com.game.contraband.infrastructure.actor.game.engine.lobby.dto;

import com.game.contraband.domain.game.player.TeamRole;

public record LobbyParticipant(Long playerId, String playerName, TeamRole teamRole, boolean ready) {
}
