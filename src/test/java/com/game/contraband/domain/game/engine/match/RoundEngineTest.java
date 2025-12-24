package com.game.contraband.domain.game.engine.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.PlayerStates;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundEngineTest {

    @Test
    void 다음_라운드에서_아직_송금하지_않은_플레이어만_송금할_수_있다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검사관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);
        RoundEngine roundEngine = RoundEngine.create(teamState);

        // when
        roundEngine.markTransferUsed(1L, 2L);

        // then
        assertAll(
                () -> assertThat(roundEngine.canTransferNextRound(1L)).isFalse(),
                () -> assertThat(roundEngine.cannotTransferNextRound(1L)).isTrue()
        );
    }

    @Test
    void 라운드가_없는_상태에서_정산을_시도하면_예외가_발생한다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검사관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);
        RoundEngine roundEngine = RoundEngine.create(teamState);

        // when & then
        assertThatThrownBy(roundEngine::finishCurrentRound)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중인 라운드가 없습니다.");
    }

    @Test
    void 밀수_금액을_선언하지_않으면_정산을_완료할_수_없다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검사관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);
        RoundEngine roundEngine = RoundEngine.create(teamState);
        Round round = Round.newRound(1, 1L, 2L)
                           .decidePass();

        roundEngine.startRound(round);

        // when & then
        assertThatThrownBy(roundEngine::finishCurrentRound)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("밀수 금액이 선언되어야 정산할 수 있습니다.");
    }

    @Test
    void 검사관_선택이_완료되지_않으면_정산을_완료할_수_없다() {
        // given
        TeamRoster smugglerRoster = TeamRoster.create(
                "밀수꾼 팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER))
        );
        TeamRoster inspectorRoster = TeamRoster.create(
                "검사관 팀",
                TeamRole.INSPECTOR,
                List.of(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR))
        );
        PlayerStates playerStates = PlayerStates.create(smugglerRoster, inspectorRoster, Money.startingAmount());
        TeamState teamState = TeamState.create(smugglerRoster, inspectorRoster, playerStates);
        RoundEngine roundEngine = RoundEngine.create(teamState);
        Round round = Round.newRound(1, 1L, 2L)
                           .declareSmuggleAmount(Money.from(1_000), Money.startingAmount());
        roundEngine.startRound(round);

        // when & then
        assertThatThrownBy(roundEngine::finishCurrentRound)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정산을 위해서는 검사관 선택이 완료된 상태여야 합니다.");
    }
}

