package com.game.contraband.infrastructure.actor.game.engine.match;

import com.game.contraband.domain.game.vo.Money;
import com.game.contraband.global.actor.CborSerializable;
import com.game.contraband.infrastructure.actor.game.engine.match.round.ContrabandRoundActor.RoundReadySelection;

public interface ContrabandGameProtocol {

    interface ContrabandGameCommand extends CborSerializable { }

    record RegisterSmugglerId(Long smugglerId, int currentRound) implements ContrabandGameCommand { }

    record RegisterSmuggler(Long smugglerId) implements ContrabandGameCommand { }

    record FixSmugglerId(Long requesterId) implements ContrabandGameCommand { }

    record RegisterInspectorId(Long inspectorId, int currentRound) implements ContrabandGameCommand { }

    record RegisterInspector(Long inspectorId) implements ContrabandGameCommand { }

    record FixInspectorId(Long requesterId) implements ContrabandGameCommand { }

    record StartNewRound() implements ContrabandGameCommand { }

    record TransferAmount(Long fromPlayerId, Long toPlayerId, Money amount) implements ContrabandGameCommand { }

    record DecideSmuggleAmount(Long smugglerId, int amount) implements ContrabandGameCommand { }

    record DecidePass(Long inspectorId) implements ContrabandGameCommand { }

    record DecideInspection(Long inspectorId, int amount) implements ContrabandGameCommand { }

    record FinishCurrentRound() implements ContrabandGameCommand { }

    record FinishedGame() implements ContrabandGameCommand { }

    record RoundTimeout(int round) implements ContrabandGameCommand { }

    record RoundSelectionTimeout(int round) implements ContrabandGameCommand { }

    record SyncReconnectedPlayer(Long playerId) implements ContrabandGameCommand { }

    record PrepareNextSelection(int nextRound) implements ContrabandGameCommand { }

    record RoundReady(Long smugglerId, Long inspectorId, int round) implements ContrabandGameCommand { }

    record StartSelectedRound(RoundReadySelection selection) implements ContrabandGameCommand { }

    record GameCleanup() implements ContrabandGameCommand { }
}
