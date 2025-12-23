package com.game.contraband.domain.game.transfer;

import java.util.HashSet;
import java.util.Set;

public class TransferUsageTracker {

    private int currentRoundNumber;
    private int lastPreparedRoundNumber;
    private final Set<Long> usedInRound = new HashSet<>();
    private boolean roundActive;

    public void prepareRound(int roundNumber) {
        if (roundActive) {
            throw new IllegalStateException("이전 라운드가 정산된 이후에만 다음 라운드를 준비할 수 있습니다.");
        }

        int expectedNextRound = lastPreparedRoundNumber + 1;

        if (roundNumber != expectedNextRound) {
            throw new IllegalStateException("다음 라운드 준비는 이전 라운드 정산 직후에만 실행되어야 합니다.");
        }

        currentRoundNumber = roundNumber;
        lastPreparedRoundNumber = roundNumber;
        usedInRound.clear();
        roundActive = true;
    }

    public void validateAvailable(int roundNumber, long senderId, long receiverId) {
        validateRoundActive(roundNumber);

        if (usedInRound.contains(senderId) || usedInRound.contains(receiverId)) {
            throw new IllegalStateException("각 라운드에서는 각 플레이어가 한 번만 송금에 참여할 수 있습니다.");
        }
    }

    public void markUsed(int roundNumber, long senderId, long receiverId) {
        validateAvailable(roundNumber, senderId, receiverId);
        usedInRound.add(senderId);
        usedInRound.add(receiverId);
    }

    public boolean canTransfer(int roundNumber, long playerId) {
        return roundActive
                && roundNumber == currentRoundNumber
                && !usedInRound.contains(playerId);
    }

    public void finishRound(int roundNumber) {
        validateRoundActive(roundNumber);
        roundActive = false;
    }

    private void validateRoundActive(int roundNumber) {
        if (!roundActive) {
            throw new IllegalStateException("현재 라운드가 준비되지 않았거나 이미 정산되었습니다.");
        }
        if (roundNumber != currentRoundNumber) {
            throw new IllegalStateException("현재 라운드 정보와 송금 라운드가 일치하지 않습니다.");
        }
    }
}
