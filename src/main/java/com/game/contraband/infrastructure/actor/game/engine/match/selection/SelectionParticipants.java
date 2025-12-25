package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class SelectionParticipants {

    private final List<PlayerProfile> smugglerPlayers;
    private final List<PlayerProfile> inspectorPlayers;
    private final int smugglerTeamSize;
    private final int inspectorTeamSize;

    public SelectionParticipants(
            List<PlayerProfile> smugglerPlayers,
            List<PlayerProfile> inspectorPlayers,
            int smugglerTeamSize,
            int inspectorTeamSize
    ) {
        this.smugglerPlayers = smugglerPlayers;
        this.inspectorPlayers = inspectorPlayers;
        this.smugglerTeamSize = smugglerTeamSize;
        this.inspectorTeamSize = inspectorTeamSize;
    }

    boolean isTwoPlayerGame() {
        return smugglerTeamSize == 1
                && inspectorTeamSize == 1
                && smugglerPlayers.size() == 1
                && inspectorPlayers.size() == 1;
    }

    boolean isNotTwoPlayerGame() {
        return !isTwoPlayerGame();
    }

    boolean requiresSmugglerConsensus() {
        return isNotTwoPlayerGame() && smugglerTeamSize > 1;
    }

    boolean requiresInspectorConsensus() {
        return isNotTwoPlayerGame() && inspectorTeamSize > 1;
    }

    int requiredSmugglerApprovals() {
        return Math.max(1, smugglerTeamSize - 1);
    }

    int requiredInspectorApprovals() {
        return Math.max(1, inspectorTeamSize - 1);
    }

    Optional<Long> firstSmugglerId() {
        if (smugglerPlayers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(smugglerPlayers.get(0).getPlayerId());
    }

    Optional<Long> firstInspectorId() {
        if (inspectorPlayers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(inspectorPlayers.get(0).getPlayerId());
    }

    boolean hasSmugglerCandidate() {
        return !smugglerPlayers.isEmpty();
    }

    boolean hasInspectorCandidate() {
        return !inspectorPlayers.isEmpty();
    }

    boolean hasBothCandidates() {
        return hasSmugglerCandidate() && hasInspectorCandidate();
    }

    Optional<Long> pickRandomPlayerId(TeamRole teamRole, Random random) {
        List<PlayerProfile> candidates = teamRole.isInspector() ? inspectorPlayers : smugglerPlayers;

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(candidates.get(random.nextInt(candidates.size())).getPlayerId());
    }
}
