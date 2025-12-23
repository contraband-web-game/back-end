package com.game.contraband.domain.game.engine.match;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.PlayerStates;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.dto.RoundDto;
import com.game.contraband.domain.game.transfer.TransferFailureException;
import com.game.contraband.domain.game.transfer.TransferFailureReason;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class ContrabandGame {

    public static ContrabandGame notStarted(TeamRoster smugglerRoster, TeamRoster inspectorRoster, int totalRounds) {
        validateTeam(smugglerRoster, inspectorRoster);
        validateTotalRounds(totalRounds);

        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);
        RoundEngine roundEngine = RoundEngine.create(teamState);

        return new ContrabandGame(
                teamState,
                totalRounds,
                roundEngine,
                GameStatus.NOT_STARTED
        );
    }

    private static void validateTeam(TeamRoster smugglerRoster, TeamRoster inspectorRoster) {
        if (smugglerRoster.isInspectorTeam() || inspectorRoster.isSmugglerTeam()) {
            throw new IllegalArgumentException("팀 역할이 올바르지 않습니다.");
        }
    }

    private static void validateTotalRounds(int totalRounds) {
        if (totalRounds <= 0) {
            throw new IllegalArgumentException("총 진행 라운드는 1 이상이어야 합니다.");
        }
    }

    private ContrabandGame(
            TeamState teamState,
            int totalRounds,
            RoundEngine roundEngine,
            GameStatus status
    ) {
        this.teamState = teamState;
        this.totalRounds = totalRounds;
        this.roundEngine = roundEngine;
        this.status = status;
    }

    private final TeamState teamState;
    private final int totalRounds;
    private final RoundEngine roundEngine;
    private GameStatus status;

    public void transferWithinTeam(Long fromPlayerId, Long toPlayerId, Money amount) {
        validateTransferWithinTeam(fromPlayerId, toPlayerId, amount);

        Player sender = teamState.requirePlayer(fromPlayerId);
        Player receiver = teamState.requirePlayer(toPlayerId);
        Player updatedSender = sender.minusBalance(amount);
        Player updatedReceiver = receiver.plusBalance(amount);

        teamState.replace(updatedSender);
        teamState.replace(updatedReceiver);
        roundEngine.markTransferUsed(updatedSender.getId(), updatedReceiver.getId());
    }

    private void validateTransferWithinTeam(Long fromPlayerId, Long toPlayerId, Money amount) {
        validateGameStateForTransfer();
        validateTransferRequest(fromPlayerId, toPlayerId, amount);

        Player sender = teamState.requirePlayer(fromPlayerId);
        Player receiver = teamState.requirePlayer(toPlayerId);

        validateTransferRules(sender, receiver, amount);
    }

    private void validateGameStateForTransfer() {
        if (status.isFinished()) {
            throw new IllegalStateException("게임이 종료된 후에는 송금할 수 없습니다.");
        }
    }

    private void validateTransferRequest(Long fromPlayerId, Long toPlayerId, Money amount) {
        if (Objects.equals(fromPlayerId, toPlayerId)) {
            throw new IllegalArgumentException("자기 자신에게 송금할 수 없습니다.");
        }
        if (amount.isLessThanOrEqual(Money.ZERO)) {
            throw new IllegalArgumentException("송금 금액은 0보다 커야 합니다.");
        }
        if (amount.isNotHundredsUnit()) {
            throw new TransferFailureException(
                    TransferFailureReason.INVALID_UNIT,
                    "송금 금액은 100원 단위여야 합니다."
            );
        }
    }

    private void validateTransferRules(Player sender, Player receiver, Money amount) {
        teamState.validateSameTeam(sender, receiver);

        if (roundEngine.cannotTransferNextRound(sender.getId())
                || roundEngine.cannotTransferNextRound(receiver.getId())) {
            throw new TransferFailureException(
                    TransferFailureReason.ALREADY_PARTICIPATED,
                    "이미 해당 라운드에서 송금에 참여했습니다."
            );
        }

        roundEngine.validateTransferAvailable(sender.getId(), receiver.getId());

        if (sender.cannotTransfer(amount)) {
            throw new TransferFailureException(
                    TransferFailureReason.INSUFFICIENT_BALANCE,
                    "송금 금액이 보유 금액을 초과합니다."
            );
        }
    }

    private Round requireCurrentRound() {
        return roundEngine.requireCurrentRound();
    }

    public boolean isOneVersusOne() {
        return teamState.isOneVersusOne();
    }

    public boolean canFinishCurrentRound() {
        return roundEngine.currentRound()
                          .map(Round::isInspectionDecisionCompleted)
                          .orElse(false);
    }

    public boolean cannotFinishCurrentRound() {
        return !canFinishCurrentRound();
    }

    public boolean isNotFinished() {
        return !status.isFinished();
    }

    public Round startNewRound(Long smugglerId, Long inspectorId) {
        if (status.isFinished()) {
            throw new IllegalStateException("이미 게임이 종료되었습니다.");
        }
        if (roundEngine.hasRound()) {
            throw new IllegalStateException("이전 라운드가 아직 완료되지 않았습니다.");
        }
        if (roundEngine.completedRoundCount() >= totalRounds) {
            throw new IllegalStateException("모든 라운드를 이미 소진했습니다.");
        }

        Player smuggler = teamState.requirePlayer(smugglerId);
        Player inspector = teamState.requirePlayer(inspectorId);

        teamState.validateSmugglerInRoster(smugglerId);
        teamState.validateInspectorInRoster(inspectorId);

        if (!smuggler.isSmugglerTeam()) {
            throw new IllegalArgumentException("밀수꾼은 SMUGGLER 팀에 속해야 합니다.");
        }
        if (!inspector.isInspectorTeam()) {
            throw new IllegalArgumentException("검사관은 INSPECTOR 팀에 속해야 합니다.");
        }

        Round round = Round.newRound(roundEngine.nextRoundNumber(), smugglerId, inspectorId);
        roundEngine.startRound(round);

        if (status.isNotStarted()) {
            status = GameStatus.IN_PROGRESS;
        }

        return round;
    }

    public void decideSmuggleAmountForCurrentRound(Money amount) {
        Round round = requireCurrentRound();

        if (round.isSmuggleAmountDeclared()) {
            throw new IllegalStateException("이미 밀수 금액이 설정되었거나, 잘못된 상태입니다.");
        }

        Player smuggler = teamState.requirePlayer(round.getSmugglerId());
        Money smugglerBalance = smuggler.getBalance();

        Round updated = round.declareSmuggleAmount(round.getSmugglerId(), amount, smugglerBalance);
        roundEngine.updateRound(updated);
    }

    public void decidePassForCurrentRound() {
        Round round = requireCurrentRound();
        Round updated = round.decidePass(round.getInspectorId());

        roundEngine.updateRound(updated);
    }

    public void decideInspectionForCurrentRound(Money claimedAmount) {
        Round round = requireCurrentRound();

        Round updated = round.decideInspection(round.getInspectorId(), claimedAmount);

        roundEngine.updateRound(updated);
    }

    public RoundDto finishCurrentRound() {
        RoundDto completed = roundEngine.finishCurrentRound();

        boolean bothTeamsOutOfMoney = bothTeamsOutOfMoney();

        if (bothTeamsOutOfMoney || roundEngine.completedRoundCount() >= totalRounds) {
            status = GameStatus.FINISHED;
            return completed;
        }

        roundEngine.prepareNextRound();

        return completed;
    }

    public GameWinnerType determineWinner() {
        if (status.isNotFinished()) {
            throw new IllegalStateException("게임이 아직 종료되지 않았습니다.");
        }

        Money smugglerTotal = teamState.totalBalanceOfSmugglerTeam();
        Money inspectorTotal = teamState.totalBalanceOfInspectorTeam();

        if (smugglerTotal.isGreaterThan(inspectorTotal)) {
            return GameWinnerType.SMUGGLER_TEAM;
        }
        if (inspectorTotal.isGreaterThan(smugglerTotal)) {
            return GameWinnerType.INSPECTOR_TEAM;
        }

        return GameWinnerType.DRAW;
    }

    public Money getSmugglerTotalBalance() {
        return teamState.totalBalanceOfSmugglerTeam();
    }

    public Money getInspectorTotalBalance() {
        return teamState.totalBalanceOfInspectorTeam();
    }

    public boolean isFinished() {
        return status == GameStatus.FINISHED;
    }

    public int getCompletedRoundCount() {
        return roundEngine.completedRoundCount();
    }

    public int getCurrentRoundNumber() {
        return roundEngine.currentRoundNumber();
    }

    public boolean hasCurrentRound() {
        return roundEngine.hasRound();
    }

    public Round getCurrentRound() {
        return roundEngine.requireCurrentRound();
    }

    public Player getPlayer(Long playerId) {
        return teamState.requirePlayer(playerId);
    }

    public Money getPlayerBalance(Long playerId) {
        return teamState.requirePlayer(playerId)
                        .getBalance();
    }

    public List<PlayerProfile> smugglerPlayers() {
        return teamState.smugglerPlayers();
    }

    public List<PlayerProfile> inspectorPlayers() {
        return teamState.inspectorPlayers();
    }

    public int smugglerTeamSize() {
        return teamState.smugglerTeamSize();
    }

    public int inspectorTeamSize() {
        return teamState.inspectorTeamSize();
    }

    public Long findSingleTeamSmugglerId() {
        return teamState.findSingleTeamSmugglerId();
    }

    public Long findSingleTeamInspectorId() {
        return teamState.findSingleTeamInspectorId();
    }

    private boolean bothTeamsOutOfMoney() {
        return teamState.bothTeamsOutOfMoney();
    }

    public List<PlayerProfile> getSmugglerDraft() {
        return teamState.getSmugglerDraft();
    }

    public List<PlayerProfile> getInspectorDraft() {
        return teamState.getInspectorDraft();
    }
}
