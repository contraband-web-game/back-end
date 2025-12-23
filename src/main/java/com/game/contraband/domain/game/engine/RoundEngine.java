package com.game.contraband.domain.game.engine;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.dto.RoundDto;
import com.game.contraband.domain.game.round.settle.RoundSettlementProcessor;
import com.game.contraband.domain.game.round.settle.RoundSettlementProcessor.RoundSettlementResult;
import com.game.contraband.domain.game.round.slot.RoundSlotManager;
import com.game.contraband.domain.game.transfer.TransferUsageTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

@Getter
public class RoundEngine {

    private final TeamState teamState;
    private final RoundSlotManager roundSlotManager;
    private final List<RoundDto> completedRounds;
    private final TransferUsageTracker transferUsageTracker;
    private final RoundSettlementProcessor settlementProcessor;

    public static RoundEngine create(TeamState teamState) {
        RoundSlotManager roundSlotManager = RoundSlotManager.create();
        List<RoundDto> completedRounds = new ArrayList<>();
        TransferUsageTracker transferUsageTracker = new TransferUsageTracker();
        RoundSettlementProcessor settlementProcessor = RoundSettlementProcessor.create();

        transferUsageTracker.prepareRound(1);

        return new RoundEngine(
                teamState,
                roundSlotManager,
                completedRounds,
                transferUsageTracker,
                settlementProcessor
        );
    }

    private RoundEngine(
            TeamState teamState,
            RoundSlotManager roundSlotManager,
            List<RoundDto> completedRounds,
            TransferUsageTracker transferUsageTracker,
            RoundSettlementProcessor settlementProcessor
    ) {
        this.teamState = teamState;
        this.roundSlotManager = roundSlotManager;
        this.completedRounds = completedRounds;
        this.transferUsageTracker = transferUsageTracker;
        this.settlementProcessor = settlementProcessor;
    }

    public boolean hasRound() {
        return roundSlotManager.hasRound();
    }

    public Optional<Round> currentRound() {
        return roundSlotManager.currentRound();
    }

    public Round requireCurrentRound() {
        return roundSlotManager.currentRound()
                               .orElseThrow(() -> new IllegalStateException("진행 중인 라운드가 없습니다."));
    }

    public int completedRoundCount() {
        return completedRounds.size();
    }

    public List<RoundDto> completedRounds() {
        return List.copyOf(completedRounds);
    }

    public int currentRoundNumber() {
        return completedRounds.size() + (hasRound() ? 1 : 0);
    }

    public int nextRoundNumber() {
        return completedRounds.size() + 1;
    }

    public Round startRound(Round round) {
        roundSlotManager.start(round);
        return round;
    }

    public void updateRound(Round round) {
        roundSlotManager.update(round);
    }

    public RoundDto finishCurrentRound() {
        Round round = requireCurrentRound();

        round.validateReadyToSettle();

        Player smuggler = teamState.requirePlayer(round.getSmugglerId());
        Player inspector = teamState.requirePlayer(round.getInspectorId());

        RoundSettlementResult settlementResult = settlementProcessor.process(round, smuggler, inspector);
        Player updatedSmuggler = settlementResult.updatedSmuggler();
        Player updatedInspector = settlementResult.updatedInspector();

        teamState.replace(updatedSmuggler);
        teamState.replace(updatedInspector);

        RoundDto completed = new RoundDto(round, settlementResult.settlement());
        completedRounds.add(completed);

        roundSlotManager.clear();
        transferUsageTracker.finishRound(round.getRoundNumber());

        return completed;
    }

    public void prepareNextRound() {
        transferUsageTracker.prepareRound(nextRoundNumber());
    }

    public boolean canTransferNextRound(Long playerId) {
        return transferUsageTracker.canTransfer(nextRoundNumber(), playerId);
    }

    public boolean cannotTransferNextRound(Long playerId) {
        return !canTransferNextRound(playerId);
    }

    public void validateTransferAvailable(Long senderId, Long receiverId) {
        transferUsageTracker.validateAvailable(nextRoundNumber(), senderId, receiverId);
    }

    public void markTransferUsed(Long senderId, Long receiverId) {
        transferUsageTracker.markUsed(nextRoundNumber(), senderId, receiverId);
    }
}
