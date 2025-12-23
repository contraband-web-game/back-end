package com.game.contraband.domain.game.engine.match;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.Player;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.PlayerStates;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.transfer.TransferFailureException;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TeamStateTest {

    @Test
    void 팀_역할이_잘못되면_생성할_수_없다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER))
        );
        TeamRoster wrongInspectorRoster = TeamRoster.create(
                "잘못된 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, wrongInspectorRoster, Money.startingAmount());

        // when & then
        assertThatThrownBy(() -> TeamState.create(smugglerRoster, wrongInspectorRoster, playerStates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀 역할이 올바르지 않습니다.");
    }

    @Test
    void 같은_팀이_아니면_송금_대상이_될_수_없다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검사관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);

        Player smuggler = teamState.requirePlayer(1L);
        Player inspector = teamState.requirePlayer(2L);

        // when & then
        assertThatThrownBy(() -> teamState.validateSameTeam(smuggler, inspector))
                .isInstanceOf(TransferFailureException.class)
                .hasMessage("같은 팀 플레이어 간에만 송금할 수 있습니다.");
    }

    @Test
    void 로스터에_없는_플레이어는_유효하지_않다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검사관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);

        // when & then
        assertAll(
                () -> assertThatThrownBy(() -> teamState.validateSmugglerInRoster(999L))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("밀수꾼은 밀수꾼 로스터에 포함되어야 합니다."),
                () -> assertThatThrownBy(() -> teamState.validateInspectorInRoster(999L))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("검사관은 검사관 로스터에 포함되어야 합니다.")
        );
    }
}

