package com.game.contraband.domain.game.player;

import com.game.contraband.domain.game.vo.Money;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStates {

    public static PlayerStates create(TeamRoster smugglerRoster, TeamRoster inspectorRoster, Money startingMoney) {
        Map<Long, Player> playersMap = new HashMap<>();

        initTeamPlayers(playersMap, smugglerRoster.getPlayers(), startingMoney);
        initTeamPlayers(playersMap, inspectorRoster.getPlayers(), startingMoney);

        return new PlayerStates(playersMap);
    }

    private static void initTeamPlayers(
            Map<Long, Player> playersMap,
            List<PlayerProfile> profiles,
            Money startingMoney
    ) {
        for (PlayerProfile profile : profiles) {

            Long playerId = profile.getPlayerId();

            if (playersMap.containsKey(playerId)) {
                throw new IllegalArgumentException("이미 다른 팀에 참가한 플레이어입니다.");
            }

            playersMap.put(playerId, profile.toPlayer(startingMoney));
        }
    }

    private PlayerStates(Map<Long, Player> states) {
        this.states = states;
    }

    private final Map<Long, Player> states;

    public Player require(Long playerId) {
        Player player = states.get(playerId);

        if (player == null) {
            throw new IllegalArgumentException("플레이어를 찾을 수 없습니다.");
        }

        return player;
    }

    public void replace(Player player) {
        states.put(player.getId(), player);
    }

    public Money totalBalanceOf(TeamRoster roster) {
        return roster.getPlayers()
                     .stream()
                     .map(profile -> require(profile.getPlayerId()))
                     .map(Player::getBalance)
                     .reduce(Money.ZERO, Money::plus);
    }
}
