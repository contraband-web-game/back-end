package com.game.contraband.domain.game.engine;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.PlayerStates;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.transfer.TransferFailureException;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import lombok.Getter;

@Getter
public class TeamState {

    public static TeamState create(TeamRoster smugglerRoster, TeamRoster inspectorRoster, PlayerStates playerStates) {
        validateTeam(smugglerRoster, inspectorRoster);

        return new TeamState(smugglerRoster, inspectorRoster, playerStates);
    }

    private TeamState(TeamRoster smugglerRoster, TeamRoster inspectorRoster, PlayerStates playerStates) {
        this.smugglerRoster = smugglerRoster;
        this.inspectorRoster = inspectorRoster;
        this.playerStates = playerStates;
    }

    private final TeamRoster smugglerRoster;
    private final TeamRoster inspectorRoster;
    private final PlayerStates playerStates;

    public List<PlayerProfile> smugglerPlayers() {
        return smugglerRoster.getPlayers();
    }

    public List<PlayerProfile> inspectorPlayers() {
        return inspectorRoster.getPlayers();
    }

    public int smugglerTeamSize() {
        return smugglerRoster.getPlayers().size();
    }

    public int inspectorTeamSize() {
        return inspectorRoster.getPlayers().size();
    }

    public boolean isSmuggler(Long playerId) {
        return smugglerRoster.hasPlayer(playerId);
    }

    public boolean isInspector(Long playerId) {
        return inspectorRoster.hasPlayer(playerId);
    }

    public Player requirePlayer(Long playerId) {
        return playerStates.require(playerId);
    }

    public void replace(Player player) {
        playerStates.replace(player);
    }

    public void validateSmugglerInRoster(Long playerId) {
        validatePlayerInRoster(playerId, smugglerRoster);
    }

    public void validateInspectorInRoster(Long playerId) {
        validatePlayerInRoster(playerId, inspectorRoster);
    }

    private void validatePlayerInRoster(Long playerId, TeamRoster roster) {
        if (!roster.hasPlayer(playerId)) {
            String message = roster.isSmugglerTeam()
                    ? "밀수꾼은 밀수꾼 로스터에 포함되어야 합니다."
                    : "검사관은 검사관 로스터에 포함되어야 합니다.";
            throw new IllegalArgumentException(message);
        }
    }

    public void validateSameTeam(Player player1, Player player2) {
        boolean inSmugglerTeam = smugglerRoster.hasPlayer(player1.getId()) && smugglerRoster.hasPlayer(player2.getId());
        boolean inInspectorTeam = inspectorRoster.hasPlayer(player1.getId()) && inspectorRoster.hasPlayer(player2.getId());

        if (!(inSmugglerTeam || inInspectorTeam)) {
            throw new TransferFailureException(
                    TransferFailureReason.DIFFERENT_TEAM,
                    "같은 팀 플레이어 간에만 송금할 수 있습니다."
            );
        }
    }

    public boolean isOneVersusOne() {
        return smugglerRoster.getPlayers().size() == 1 && inspectorRoster.getPlayers().size() == 1;
    }

    public Money totalBalanceOfSmugglerTeam() {
        return playerStates.totalBalanceOf(smugglerRoster);
    }

    public Money totalBalanceOfInspectorTeam() {
        return playerStates.totalBalanceOf(inspectorRoster);
    }

    public Long findSingleTeamSmugglerId() {
        return smugglerRoster.getPlayers()
                             .get(0)
                             .getPlayerId();
    }

    public Long findSingleTeamInspectorId() {
        return inspectorRoster.getPlayers()
                              .get(0)
                              .getPlayerId();
    }

    public List<PlayerProfile> getSmugglerDraft() {
        return smugglerRoster.getPlayers();
    }

    public List<PlayerProfile> getInspectorDraft() {
        return inspectorRoster.getPlayers();
    }

    public boolean bothTeamsOutOfMoney() {
        return totalBalanceOfSmugglerTeam().isZero() && totalBalanceOfInspectorTeam().isZero();
    }

    private static void validateTeam(TeamRoster smugglerRoster, TeamRoster inspectorRoster) {
        if (smugglerRoster.isInspectorTeam() || inspectorRoster.isSmugglerTeam()) {
            throw new IllegalArgumentException("팀 역할이 올바르지 않습니다.");
        }
    }
}
