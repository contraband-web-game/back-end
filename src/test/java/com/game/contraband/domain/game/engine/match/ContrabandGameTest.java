package com.game.contraband.domain.game.engine.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import com.game.contraband.domain.game.round.Round;
import com.game.contraband.domain.game.round.RoundStatus;
import com.game.contraband.domain.game.round.dto.RoundDto;
import com.game.contraband.domain.game.transfer.TransferFailureException;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandGameTest {

    @Test
    void 게임을_생성한다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(3L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector1, inspector2));

        // when
        ContrabandGame actual = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 5);

        // then
        assertAll(
                () -> assertThat(actual.getStatus()).isEqualTo(GameStatus.NOT_STARTED),
                () -> assertThat(actual.getTotalRounds()).isEqualTo(5),
                () -> assertThat(actual.getCompletedRoundCount()).isZero(),
                () -> assertThat(actual.hasCurrentRound()).isFalse()
        );
    }

    @Test
    void 팀_별_총액을_조회한다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(3L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector1, inspector2));
        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 5);

        // when
        Money smugglerTotal = game.getSmugglerTotalBalance();
        Money inspectorTotal = game.getInspectorTotalBalance();

        // then
        assertAll(
                () -> assertThat(smugglerTotal).isEqualTo(Money.from(6_000)),
                () -> assertThat(inspectorTotal).isEqualTo(Money.from(6_000))
        );
    }

    @Test
    void 지정한_플레이어_잔액을_조회한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);
        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));
        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when
        Money smugglerBalance = game.getPlayerBalance(1L);

        // then
        assertThat(smugglerBalance).isEqualTo(Money.from(3_000));
    }

    @Test
    void 팀_역할이_잘못되면_게임을_생성할_수_없다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster wrongRoster = TeamRoster.create("잘못된팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));

        // when & then
        assertThatThrownBy(() -> ContrabandGame.notStarted(smugglerRoster, wrongRoster, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀 역할이 올바르지 않습니다.");
    }

    @Test
    void 총_라운드가_1_미만이면_게임을_생성할_수_없다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector1));

        // when & then
        assertThatThrownBy(() -> ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("총 진행 라운드는 1 이상이어야 합니다.");
    }

    @Test
    void 새_라운드를_시작한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when
        Round actual = game.startNewRound(1L, 2L);

        // then
        assertAll(
                () -> assertThat(actual.getRoundNumber()).isEqualTo(1),
                () -> assertThat(actual.getSmugglerId()).isEqualTo(1L),
                () -> assertThat(actual.getInspectorId()).isEqualTo(2L),
                () -> assertThat(actual.getStatus()).isEqualTo(RoundStatus.NEW),
                () -> assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS)
        );
    }

    @Test
    void 게임이_종료되면_새_라운드를_시작할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();
        game.finishCurrentRound();

        // when & then
        assertThatThrownBy(() -> game.startNewRound(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 게임이 종료되었습니다.");
    }

    @Test
    void 진행_중인_라운드가_있으면_새_라운드를_시작할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        game.startNewRound(1L, 2L);

        // when & then
        assertThatThrownBy(() -> game.startNewRound(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이전 라운드가 아직 완료되지 않았습니다.");
    }

    @Test
    void 존재하지_않는_플레이어로_라운드를_시작할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.startNewRound(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("플레이어를 찾을 수 없습니다.");
    }

    @Test
    void 현재_라운드에서_밀수_금액을_선언한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);
        game.startNewRound(1L, 2L);

        // when
        game.decideSmuggleAmountForCurrentRound(Money.from(500));

        // then
        Round actual = game.getCurrentRound();

        assertAll(
                () -> assertThat(actual.getStatus()).isEqualTo(RoundStatus.SMUGGLE_DECLARED),
                () -> assertThat(actual.getSmuggleAmount().getAmount()).isEqualTo(500)
        );
    }

    @Test
    void 진행_중인_라운드가_없으면_밀수_금액을_선언할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.decideSmuggleAmountForCurrentRound(Money.from(500)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중인 라운드가 없습니다.");
    }

    @Test
    void 이미_밀수_금액을_결정한_뒤_중복해서_밀수_금액을_결정할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));

        // when & then
        assertThatThrownBy(() -> game.decideSmuggleAmountForCurrentRound(Money.from(600)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 밀수 금액이 설정되었거나, 잘못된 상태입니다.");
    }

    @Test
    void 현재_라운드에서_검사관이_검문을_하지_않고_통과를_선택한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));

        // when
        game.decidePassForCurrentRound();

        // then
        Round actual = game.getCurrentRound();

        assertThat(actual.getStatus()).isEqualTo(RoundStatus.INSPECTION_DECIDED);
    }

    @Test
    void 현재_라운드에서_검문을_선택한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));

        // when
        game.decideInspectionForCurrentRound(Money.from(1_000));

        // then
        Round actual = game.getCurrentRound();

        assertAll(
                () -> assertThat(actual.getStatus()).isEqualTo(RoundStatus.INSPECTION_DECIDED),
                () -> assertThat(actual.getInspectionThreshold().getAmount()).isEqualTo(1_000)
        );
    }

    @Test
    void 라운드를_완료하고_정산한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();

        // when
        RoundDto actual = game.finishCurrentRound();

        // then
        assertAll(
                () -> assertThat(actual.round().getRoundNumber()).isEqualTo(1),
                () -> assertThat(game.hasCurrentRound()).isFalse(),
                () -> assertThat(game.getCompletedRoundCount()).isEqualTo(1)
        );
    }

    @Test
    void 검사관_행동_결정_전에는_라운드를_완료할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));

        // when & then
        assertThatThrownBy(() -> game.finishCurrentRound())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정산을 위해서는 검사관 선택이 완료된 상태여야 합니다.");
    }

    @Test
    void 모든_라운드_완료시_게임이_종료된다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();

        // when
        game.finishCurrentRound();

        // then
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void 게임_종료_후_승리한_팀을_확인한다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);
        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();
        game.finishCurrentRound();

    // when
    GameWinnerType actual = game.determineWinner();

    // then
    assertThat(actual).isEqualTo(GameWinnerType.SMUGGLER_TEAM);
}

@Test
void 게임이_종료되지_않으면_승리한_팀을_확인할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.determineWinner())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("게임이 아직 종료되지 않았습니다.");
    }

    @Test
    void 같은_팀_내에서_송금한다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(3L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when
        game.transferWithinTeam(1L, 2L, Money.from(500));

        // then
        assertAll(
                () -> assertThat(game.getPlayer(1L).getBalance().getAmount()).isEqualTo(2_500),
                () -> assertThat(game.getPlayer(2L).getBalance().getAmount()).isEqualTo(3_500)
        );
    }

    @Test
    void 게임_종료_후에는_송금할_수_없다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(3L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 1);

        game.startNewRound(1L, 3L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();
        game.finishCurrentRound();

        // when & then
        assertThatThrownBy(() -> game.transferWithinTeam(1L, 2L, Money.from(500)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("게임이 종료된 후에는 송금할 수 없습니다.");
    }

    @Test
    void 자기_자신에게는_송금할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.transferWithinTeam(1L, 1L, Money.from(500)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신에게 송금할 수 없습니다.");
    }

    @Test
    void 송금_금액이_0원_이하면_송금할_수_없다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(3L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.transferWithinTeam(1L, 2L, Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("송금 금액은 0보다 커야 합니다.");
    }

    @Test
    void 다른_팀에게는_송금할_수_없다() {
        // given
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.transferWithinTeam(1L, 2L, Money.from(500)))
                .isInstanceOf(TransferFailureException.class)
                .hasMessage("같은 팀 플레이어 간에만 송금할 수 있습니다.");
    }

    @Test
    void 보유_금액보다_많이_송금할_수_없다() {
        // given
        PlayerProfile smuggler1 = PlayerProfile.create(1L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(3L, "검사관", TeamRole.INSPECTOR);

        TeamRoster smugglerRoster = TeamRoster.create("밀수꾼팀", TeamRole.SMUGGLER, List.of(smuggler1, smuggler2));
        TeamRoster inspectorRoster = TeamRoster.create("검사관팀", TeamRole.INSPECTOR, List.of(inspector));

        ContrabandGame game = ContrabandGame.notStarted(smugglerRoster, inspectorRoster, 3);

        // when & then
        assertThatThrownBy(() -> game.transferWithinTeam(1L, 2L, Money.from(10_000)))
                .isInstanceOf(TransferFailureException.class)
                .hasMessage("송금 금액이 보유 금액을 초과합니다.");
    }
}
