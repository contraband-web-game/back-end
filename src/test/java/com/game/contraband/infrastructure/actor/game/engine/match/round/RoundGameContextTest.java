package com.game.contraband.infrastructure.actor.game.engine.match.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.round.RoundStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundGameContextTest {

    @Test
    void 현재_라운드를_조회할_수_있다() {
        // given
        RoundGameContext context = createContext(3);

        // when
        context.startNewRound(1L, 2L);

        // then
        assertAll(
                () -> assertThat(context.currentRound()).isPresent(),
                () -> assertThat(context.currentRound().get().getRoundNumber()).isEqualTo(1)
        );
    }

    @Test
    void 밀수꾼이_금액을_선언하면_라운드_상태가_업데이트된다() {
        // given
        RoundGameContext context = createContext(3);

        context.startNewRound(1L, 2L);

        // when
        context.decideSmuggleAmount(500);

        // then
        assertAll(
                () -> assertThat(context.currentRound()).isPresent(),
                () -> assertThat(context.currentRound().get().getSmuggleAmount().getAmount()).isEqualTo(500),
                () -> assertThat(context.currentRound().get().getStatus()).isEqualTo(RoundStatus.SMUGGLE_DECLARED)
        );
    }

    @Test
    void 검사관이_통과를_선택하면_라운드_상태가_업데이트된다() {
        // given
        RoundGameContext context = createContext(3);

        context.startNewRound(1L, 2L);
        context.decideSmuggleAmount(500);

        // when
        context.decidePass();

        // then
        assertAll(
                () -> assertThat(context.currentRound()).isPresent(),
                () -> assertThat(context.currentRound().get().getStatus()).isEqualTo(RoundStatus.INSPECTION_DECIDED),
                () -> assertThat(context.currentRound().get().isPass()).isTrue()
        );
    }

    @Test
    void 라운드를_완료하면_현재_라운드가_비워지고_완료_라운드가_증가한다() {
        // given
        RoundGameContext context = createContext(1);

        context.startNewRound(1L, 2L);
        context.decideSmuggleAmount(500);
        context.decidePass();

        // when
        context.finishCurrentRound();

        // then
        assertAll(
                () -> assertThat(context.currentRound()).isEmpty(),
                () -> assertThat(context.isFinished()).isTrue()
        );
    }

    @Test
    void 플레이어_역할을_해석한다() {
        // given
        RoundGameContext context = createContext(3);

        // when
        TeamRole smugglerRole = context.resolveTeamRole(1L);
        TeamRole inspectorRole = context.resolveTeamRole(2L);

        // then
        assertAll(
                () -> assertThat(smugglerRole).isEqualTo(TeamRole.SMUGGLER),
                () -> assertThat(inspectorRole).isEqualTo(TeamRole.INSPECTOR)
        );
    }

    private RoundGameContext createContext(int totalRounds) {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼 팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관 팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, totalRounds);

        return new RoundGameContext(game);
    }
}
