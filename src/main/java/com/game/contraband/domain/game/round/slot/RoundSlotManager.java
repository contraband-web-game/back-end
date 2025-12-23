package com.game.contraband.domain.game.round.slot;

import com.game.contraband.domain.game.round.Round;
import java.util.Optional;

public class RoundSlotManager {

    public static RoundSlotManager create() {
        return new RoundSlotManager(EmptyRoundSlot.INSTANCE);
    }

    private RoundSlotManager(RoundSlot current) {
        this.current = current;
    }

    private RoundSlot current;

    public boolean hasRound() {
        return current.hasRound();
    }

    public Optional<Round> currentRound() {
        return current.currentRound();
    }

    public void start(Round round) {
        if (round == null) {
            throw new IllegalArgumentException("라운드는 비어 있을 수 없습니다.");
        }
        if (current.hasRound()) {
            throw new IllegalStateException("이전 라운드가 아직 완료되지 않았습니다.");
        }

        this.current = ActiveRoundSlot.create(round);
    }

    public void update(Round updated) {
        if (updated == null) {
            throw new IllegalArgumentException("라운드는 비어 있을 수 없습니다.");
        }
        if (!current.hasRound()) {
            throw new IllegalStateException("진행 중인 라운드가 없습니다.");
        }

        this.current = ActiveRoundSlot.create(updated);
    }

    public void clear() {
        if (!current.hasRound()) {
            throw new IllegalStateException("진행 중인 라운드가 없습니다.");
        }

        this.current = EmptyRoundSlot.INSTANCE;
    }
}
