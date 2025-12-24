package com.game.contraband.domain.game.engine.lobby;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.engine.match.slot.ActiveGameSlot;
import com.game.contraband.domain.game.engine.match.slot.EmptyGameSlot;
import com.game.contraband.domain.game.engine.match.slot.GameSlot;
import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
public class LobbyLifeCycle {

    public static LobbyLifeCycle create() {
        return create(LobbyGuards.create());
    }

    public static LobbyLifeCycle create(LobbyGuards guards) {
        return new LobbyLifeCycle(EmptyGameSlot.INSTANCE, LobbyPhase.LOBBY, guards);
    }

    private LobbyLifeCycle(GameSlot gameSlot, LobbyPhase phase, LobbyGuards guards) {
        this.gameSlot = gameSlot;
        this.phase = phase;
        this.guards = guards;
    }

    private LobbyPhase phase;
    private GameSlot gameSlot;
    private final LobbyGuards guards;

    public boolean isLobbyPhase() {
        return phase.isLobby();
    }

    public ContrabandGame currentGame() {
        return gameSlot.currentGame()
                       .orElseThrow(() -> new IllegalStateException("게임이 아직 시작되지 않았습니다."));
    }

    public void start(ContrabandGame game) {
        requireLobbyPhase();

        if (game == null) {
            throw new IllegalArgumentException("시작할 게임이 필요합니다.");
        }

        this.gameSlot = new ActiveGameSlot(game);
        this.phase = LobbyPhase.IN_PROGRESS;
    }

    public void finishFromLobby() {
        requireLobbyPhase();

        this.gameSlot = EmptyGameSlot.INSTANCE;
        this.phase = LobbyPhase.FINISHED;
    }

    public void requireLobbyPhase() {
        guards.requireLobbyPhase(phase);
    }

    public void markFinishedIfDone() {
        gameSlot.currentGame()
                .filter(ContrabandGame::isFinished)
                .ifPresent(ignored -> this.phase = LobbyPhase.FINISHED);
    }
}
