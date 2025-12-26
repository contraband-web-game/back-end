package com.game.contraband.infrastructure.actor.game.engine.match;

import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.game.engine.match.dto.RoundReadySelection;

public interface ContrabandGameProtocol {

    interface ContrabandGameCommand extends CborSerializable { }

    interface SelectionCommand extends ContrabandGameCommand { }

    interface RoundCommand extends ContrabandGameCommand { }

    record RegisterSmugglerId(Long smugglerId, int currentRound) implements SelectionCommand { }

    record RegisterSmuggler(Long smugglerId) implements SelectionCommand { }

    record FixSmugglerId(Long requesterId) implements SelectionCommand { }

    record RegisterInspectorId(Long inspectorId, int currentRound) implements SelectionCommand { }

    record RegisterInspector(Long inspectorId) implements SelectionCommand { }

    record FixInspectorId(Long requesterId) implements SelectionCommand { }

    record StartNewRound() implements ContrabandGameCommand { }

    record TransferAmount(Long fromPlayerId, Long toPlayerId, Money amount) implements RoundCommand { }

    record DecideSmuggleAmount(Long smugglerId, int amount) implements RoundCommand { }

    record DecidePass(Long inspectorId) implements RoundCommand { }

    record DecideInspection(Long inspectorId, int amount) implements RoundCommand { }

    record FinishCurrentRound() implements RoundCommand { }

    record FinishedGame() implements RoundCommand { }

    record RoundTimeout(int round) implements RoundCommand { }

    record RoundSelectionTimeout(int round) implements SelectionCommand { }

    record SyncReconnectedPlayer(Long playerId) implements ContrabandGameCommand { }

    record RoundReady(RoundReadySelection selection) implements SelectionCommand { }

    record StartSelectedRound(RoundReadySelection selection) implements RoundCommand { }

    record PrepareNextSelection(int nextRound) implements SelectionCommand { }

    record GameCleanup() implements ContrabandGameCommand { }
}
