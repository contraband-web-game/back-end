package com.game.contraband.infrastructure.actor.game.chat.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandRoundParticipantsTest {

    @Test
    void 라운드_채팅_참여자가_없으면_채팅할_수_없다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        boolean actual = participants.cannotRoundChat();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 밀수꾼과_검사관_후보를_등록한_상태에서는_채팅이_가능하다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        participants.registerSmuggler(1L);
        participants.registerInspector(2L);

        // then
        assertThat(participants.cannotRoundChat()).isFalse();
    }

    @Test
    void 밀수꾼_후보를_등록한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        participants.registerSmuggler(1L);

        // then
        assertAll(
                () -> assertThat(participants.smugglerId()).isEqualTo(1L),
                () -> assertThat(participants.isSmuggler(1L)).isTrue(),
                () -> assertThat(participants.isSmuggler(2L)).isFalse()
        );
    }

    @Test
    void 검사관_후보를_등록한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        participants.registerInspector(2L);

        // then
        assertAll(
                () -> assertThat(participants.inspectorId()).isEqualTo(2L),
                () -> assertThat(participants.isInspector(2L)).isTrue(),
                () -> assertThat(participants.isInspector(3L)).isFalse()
        );
    }

    @Test
    void 밀수꾼_ID가_비어_있으면_후보로_등록할_수_없다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when & then
        assertThatThrownBy(() -> participants.registerSmuggler(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("smugglerId");
    }

    @Test
    void 검사관_ID가_비어_있으면_후보로_등록할_수_없다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when & then
        assertThatThrownBy(() -> participants.registerInspector(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("inspectorId");
    }

    @Test
    void 라운드_참여자가_아닌지_확인한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();
        participants.registerSmuggler(1L);
        participants.registerInspector(2L);

        // when
        boolean actual = participants.isNotRoundParticipant(3L);

        // then
        assertAll(
                () -> assertThat(actual).isTrue(),
                () -> assertThat(participants.isNotRoundParticipant(1L)).isFalse(),
                () -> assertThat(participants.isNotRoundParticipant(2L)).isFalse()
        );
    }

    @Test
    void 라운드_번호가_다른지_확인한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        boolean actual = participants.isRoundMismatch(2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 라운드_번호가_같은지_확인한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        boolean actual = participants.isRoundMismatch(1);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 참여자_ID를_조회한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();
        participants.registerSmuggler(1L);
        participants.registerInspector(2L);

        // when
        Long smugglerId = participants.smugglerId();
        Long inspectorId = participants.inspectorId();

        // then
        assertAll(
                () -> assertThat(smugglerId).isEqualTo(1L),
                () -> assertThat(inspectorId).isEqualTo(2L)
        );
    }

    @Test
    void 라운드_정보를_초기화한다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();
        participants.registerSmuggler(1L);
        participants.registerInspector(2L);

        // when
        participants.clear();

        // then
        assertAll(
                () -> assertThat(participants.smugglerId()).isNull(),
                () -> assertThat(participants.inspectorId()).isNull(),
                () -> assertThat(participants.currentRound()).isEqualTo(2),
                () -> assertThat(participants.cannotRoundChat()).isTrue()
        );
    }

    @Test
    void 초기_라운드는_1이다() {
        // given
        ContrabandRoundParticipants participants = new ContrabandRoundParticipants();

        // when
        int actual = participants.currentRound();

        // then
        assertThat(actual).isEqualTo(1);
    }
}
