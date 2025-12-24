package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyLifeCycleTest {

    @Test
    void 로비_라이프사이클을_초기화한다() {
        // when
        LobbyLifeCycle lifeCycle = LobbyLifeCycle.create();

        // then
        assertThat(lifeCycle.getPhase()).isEqualTo(LobbyPhase.LOBBY);
    }

    @Test
    void 게임_시작_시_로비의_상태를_게임_진행_중_상태로_변경한다() {
        // given
        LobbyLifeCycle lifeCycle = LobbyLifeCycle.create();
        ContrabandGame game = newGame();

        // when
        lifeCycle.start(game);

        // then
        assertAll(
                () -> assertThat(lifeCycle.getPhase()).isEqualTo(LobbyPhase.IN_PROGRESS),
                () -> assertThat(lifeCycle.currentGame()).isEqualTo(game)
        );
    }

    @Test
    void 게임_종료_시_로비의_상태를_종료_상태로_변경한다() {
        // given
        LobbyLifeCycle lifeCycle = LobbyLifeCycle.create();
        ContrabandGame game = newGame();

        lifeCycle.start(game);
        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();
        game.finishCurrentRound();

        // when
        lifeCycle.markFinishedIfDone();

        // then
        assertThat(lifeCycle.getPhase()).isEqualTo(LobbyPhase.FINISHED);
    }

    @Test
    void 로비_상태가_아니라면_팀_로스터를_변경할_수_없다() {
        // given
        LobbyLifeCycle lifeCycle = LobbyLifeCycle.create();

        lifeCycle.start(newGame());

        // when & then
        assertThatThrownBy(lifeCycle::requireLobbyPhase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로비 상태에서만 로스터를 수정할 수 있습니다.");
    }

    @Test
    void 로비에서_게임_방을_삭제시키면_로비_상태를_종료_상태로_변경한다() {
        // given
        LobbyLifeCycle lifeCycle = LobbyLifeCycle.create();

        // when
        lifeCycle.finishFromLobby();

        // then
        assertThat(lifeCycle.getPhase()).isEqualTo(LobbyPhase.FINISHED);
    }

    private ContrabandGame newGame() {
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);
        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼 팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관 팀", TeamRole.INSPECTOR, List.of(inspector));

        return ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);
    }
}
