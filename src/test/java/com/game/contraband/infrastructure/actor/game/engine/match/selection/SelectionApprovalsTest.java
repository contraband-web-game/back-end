package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SelectionApprovalsTest {

    @Test
    void 기존_밀수꾼_후보에_대한_찬성이_없을_때만_밀수꾼_후보_등록이_가능하다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.addSmugglerApproval(1L, 10L);

        // when
        approvals.initSmugglerCandidate(20L);

        // then
        assertAll(
                () -> assertThat(approvals.smugglerCandidateId()).isEqualTo(20L),
                () -> assertThat(approvals.smugglerApprovalsSnapshot()).isEmpty()
        );
    }

    @Test
    void 기존_검사관_후보에_대한_찬성이_없을_때만_검사관_후보_등록이_가능하다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.addInspectorApproval(1L, 10L);

        // when
        approvals.initInspectorCandidate(20L);

        // then
        assertAll(
                () -> assertThat(approvals.inspectorCandidateId()).isEqualTo(20L),
                () -> assertThat(approvals.inspectorApprovalsSnapshot()).isEmpty()
        );
    }

    @Test
    void 모든_찬성_정보를_초기화한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initSmugglerCandidate(10L);
        approvals.initInspectorCandidate(20L);
        approvals.addSmugglerApproval(1L, 10L);
        approvals.addInspectorApproval(2L, 20L);

        // when
        approvals.resetAll();

        // then
        assertAll(
                () -> assertThat(approvals.smugglerCandidateId()).isNull(),
                () -> assertThat(approvals.inspectorCandidateId()).isNull(),
                () -> assertThat(approvals.smugglerApprovalsSnapshot()).isEmpty(),
                () -> assertThat(approvals.inspectorApprovalsSnapshot()).isEmpty()
        );
    }

    @Test
    void 해당_밀수꾼_후보에_대한_라운드_참여를_찬성한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initSmugglerCandidate(10L);

        // when
        approvals.addSmugglerApproval(1L, 10L);

        // then
        assertAll(
                () -> assertThat(approvals.hasSmugglerApprovalFrom(1L)).isTrue(),
                () -> assertThat(approvals.smugglerApprovalsSnapshot()).containsExactly(1L)
        );
    }

    @Test
    void 해당_검사관_후보에_대한_라운드_참여를_찬성한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initInspectorCandidate(20L);

        // when
        approvals.addInspectorApproval(2L, 20L);

        // then
        assertAll(
                () -> assertThat(approvals.hasInspectorApprovalFrom(2L)).isTrue(),
                () -> assertThat(approvals.inspectorApprovalsSnapshot()).containsExactly(2L)
        );
    }

    @Test
    void 후보는_자기_자신에_대해_찬성할_수_없다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initSmugglerCandidate(10L);

        // when
        approvals.addSmugglerApproval(10L, 10L);

        // then
        assertThat(approvals.smugglerApprovalsSnapshot()).isEmpty();
    }

    @Test
    void 밀수꾼_후보를_제외한_모든_팀원이_해당_후보에_대한_라운드_참여를_찬성했는지_확인한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initSmugglerCandidate(10L);
        approvals.addSmugglerApproval(1L, 10L);

        // when
        boolean actual = approvals.hasEnoughSmugglerApprovals(10L, 1);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 검사관_후보를_제외한_모든_팀원이_해당_후보에_대한_라운드_참여를_찬성했는지_확인한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initInspectorCandidate(20L);
        approvals.addInspectorApproval(2L, 20L);
        approvals.addInspectorApproval(3L, 20L);

        // when
        boolean actual = approvals.hasEnoughInspectorApprovals(20L, 2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 해당_후보에_대한_찬성이_있는_경우_후보를_교체할_수_없다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initSmugglerCandidate(10L);
        approvals.addSmugglerApproval(1L, 10L);

        // when & then
        assertThatThrownBy(() -> approvals.ensureCanReplaceSmuggler(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 찬성이 진행된 후보는 교체할 수 없습니다.");
    }

    @Test
    void 해당_밀수꾼_후보에_대해_라운드_참여를_찬성한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();
        approvals.initSmugglerCandidate(10L);

        // when
        boolean actual = approvals.toggleSmugglerApproval(1L, 10L);

        // then
        assertAll(
                () -> assertThat(actual).isTrue(),
                () -> assertThat(approvals.hasSmugglerApprovalFrom(1L)).isTrue()
        );
    }

    @Test
    void 해당_검사관_후보에_대해_라운드_참여를_찬성한다() {
        // given
        SelectionApprovals approvals = new SelectionApprovals();

        approvals.initInspectorCandidate(20L);
        approvals.toggleInspectorApproval(2L, 20L);

        // when
        boolean actual = approvals.toggleInspectorApproval(2L, 20L);

        // then
        assertAll(
                () -> assertThat(actual).isFalse(),
                () -> assertThat(approvals.hasInspectorApprovalFrom(2L)).isFalse()
        );
    }
}
