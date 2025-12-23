package com.game.contraband.domain.game.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlayerStatesTest {

    @Test
    void 두_개의_팀_로스터로부터_플레이어_상태를_생성한다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검문관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "I1", TeamRole.INSPECTOR))
        );

        // when
        PlayerStates actual = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());

        // then
        assertAll(
                () -> assertThat(actual.require(1L).getName()).isEqualTo("S1"),
                () -> assertThat(actual.require(2L).getName()).isEqualTo("I1"),
                () -> assertThat(actual.require(1L).getBalance()).isEqualTo(Money.startingAmount()),
                () -> assertThat(actual.require(2L).getBalance()).isEqualTo(Money.startingAmount())
        );
    }

    @Test
    void 같은_플레이어는_두_팀에_중복해서_참가할_수_없다() {
        // given
        PlayerProfile duplicateProfile = PlayerProfile.create(1L, "중복", TeamRole.SMUGGLER);
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(duplicateProfile)
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검문관 팀",
                TeamRole.INSPECTOR,
                List.of(duplicateProfile)
        );

        // when & then
        assertThatThrownBy(() -> PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 다른 팀에 참가한 플레이어입니다.");
    }

    @Test
    void 존재하는_플레이어를_조회한다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검문관 팀",
                TeamRole.INSPECTOR,
                List.of()
        );
        PlayerStates playerStates = PlayerStates.create(
                smugglerRoster,
                inspectorRoster,
                Money.startingAmount()
        );

        // when
        Player actual = playerStates.require(1L);

        // then
        assertThat(actual.getName()).isEqualTo("S1");
    }

    @Test
    void 존재하지_않는_플레이어는_조회할_수_없다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검문관 팀",
                TeamRole.INSPECTOR,
                List.of()
        );
        PlayerStates playerStates = PlayerStates.create(
                smugglerRoster,
                inspectorRoster,
                Money.startingAmount()
        );

        // when & then
        assertThatThrownBy(() -> playerStates.require(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("플레이어를 찾을 수 없습니다.");
    }

    @Test
    void 팀의_총_잔액을_계산한다() {
        // given
        PlayerProfile s1 = PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER);
        PlayerProfile s2 = PlayerProfile.create(2L, "S2", TeamRole.SMUGGLER);
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(s1, s2)
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검문관 팀",
                TeamRole.INSPECTOR,
                List.of()
        );
        PlayerStates playerStates = PlayerStates.create(
                smugglerRoster,
                inspectorRoster,
                Money.from(1_500)
        );

        // when
        Money actual = playerStates.totalBalanceOf(smugglerRoster);

        // then
        assertThat(actual.getAmount()).isEqualTo(3_000);
    }

    @Test
    void 플레이어_정보를_대체한다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검문관 팀",
                TeamRole.INSPECTOR,
                List.of()
        );
        PlayerStates playerStates = PlayerStates.create(
                smugglerRoster,
                inspectorRoster,
                Money.startingAmount()
        );
        Player updatedPlayer = Player.create(1L, "변경된이름", TeamRole.SMUGGLER, Money.ZERO);

        // when
        playerStates.replace(updatedPlayer);

        // then
        assertAll(
                () -> assertThat(playerStates.require(1L).getName()).isEqualTo("변경된이름"),
                () -> assertThat(playerStates.require(1L).getBalance()).isEqualTo(Money.ZERO)
        );
    }
}
