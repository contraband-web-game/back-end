package com.game.contraband.infrastructure.actor.manage;

import com.game.contraband.global.actor.CborSerializable;

public class GameManagerEntity {

    public interface GameManagerCommand extends CborSerializable { }

    public record SyncRoomPlayerCount(Long roomId, String lobbyName, int maxPlayerCount, int currentPlayerCount, boolean gameStarted) implements GameManagerCommand { }

    public record SyncDeleteLobby(Long roomId) implements GameManagerCommand { }

    public record SyncEndGame(Long roomId) implements GameManagerCommand { }

    public record SyncRoomStarted(Long roomId, String lobbyName, int maxPlayerCount, int currentPlayerCount) implements GameManagerCommand { }
}
