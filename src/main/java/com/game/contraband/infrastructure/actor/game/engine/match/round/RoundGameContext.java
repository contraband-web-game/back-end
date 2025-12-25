package com.game.contraband.infrastructure.actor.game.engine.match.round;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.engine.match.GameWinnerType;
import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.dto.RoundDto;
import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.GameStartPlayer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RoundGameContext {

    private final ContrabandGame game;

    public RoundGameContext(ContrabandGame game) {
        this.game = game;
    }

    public void startNewRound(Long smugglerId, Long inspectorId) {
        game.startNewRound(smugglerId, inspectorId);
    }

    public void transfer(Long from, Long to, Money amount) {
        game.transferWithinTeam(from, to, amount);
    }

    public void decideSmuggleAmount(int amount) {
        game.decideSmuggleAmountForCurrentRound(Money.from(amount));
    }

    public void decidePass() {
        game.decidePassForCurrentRound();
    }

    public void decideInspection(int amount) {
        game.decideInspectionForCurrentRound(Money.from(amount));
    }

    public RoundDto finishCurrentRound() {
        return game.finishCurrentRound();
    }

    public boolean canFinishCurrentRound() {
        return game.canFinishCurrentRound();
    }

    public boolean cannotFinishCurrentRound() {
        return game.cannotFinishCurrentRound();
    }

    public boolean isFinished() {
        return game.isFinished();
    }

    public boolean isNotFinished() {
        return game.isNotFinished();
    }

    public Optional<Round> currentRound() {
        return game.getRoundEngine()
                   .getRoundSlotManager()
                   .currentRound();
    }

    public Money balanceOf(Long playerId) {
        return game.getPlayerBalance(playerId);
    }

    public GameWinnerType winner() {
        return game.determineWinner();
    }

    public Money smugglerTotalBalance() {
        return game.getSmugglerTotalBalance();
    }

    public Money inspectorTotalBalance() {
        return game.getInspectorTotalBalance();
    }

    public TeamRole resolveTeamRole(Long playerId) {
        if (game.getTeamState().isSmuggler(playerId)) {
            return TeamRole.SMUGGLER;
        }
        if (game.getTeamState().isInspector(playerId)) {
            return TeamRole.INSPECTOR;
        }

        return null;
    }

    public List<GameStartPlayer> buildStartPlayerEntries() {
        return Stream.concat(
                        game.smugglerPlayers().stream(),
                        game.inspectorPlayers().stream()
                )
                .map(
                        profile -> {
                            Player player = game.getTeamState().requirePlayer(profile.getPlayerId());

                            return new GameStartPlayer(
                                    profile.getPlayerId(),
                                    profile.getName(),
                                profile.getTeamRole(),
                                player.getBalance().getAmount()
                            );
                        }
                )
                .toList();
    }
}
