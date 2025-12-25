package com.game.contraband.infrastructure.actor.game.engine.match.dto;

import com.game.contraband.domain.game.player.TeamRole;

public record GameStartPlayer(Long playerId, String playerName, TeamRole teamRole, int amount) {
}
