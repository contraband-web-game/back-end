package com.game.contraband.domain.game.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransferUsageTrackerTest {

    @Test
    void 송금_추적_이력을_초기화한다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        // when
        tracker.prepareRound(1);

        // then
        assertThat(tracker.canTransfer(1, 1L)).isTrue();
    }

    @Test
    void 라운드_변경_시_송금_기록을_초기화한다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        tracker.prepareRound(1);
        tracker.markUsed(1, 1L, 2L);
        tracker.finishRound(1);

        // when
        tracker.prepareRound(2);

        // then
        assertAll(
                () -> assertThat(tracker.canTransfer(2, 1L)).isTrue(),
                () -> assertThat(tracker.canTransfer(2, 2L)).isTrue()
        );
    }

    @Test
    void 해당_라운드에서_송금에_참여하지_않았다면_송금을_주거나_받을_수_있다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        tracker.prepareRound(1);

        // when
        boolean actual = tracker.canTransfer(1, 1L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 다른_라운드에서_현재_라운드로_송금할_수_없다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        tracker.prepareRound(1);

        // when
        boolean actual = tracker.canTransfer(2, 1L);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 같은_라운드에서_이미_송금한_플레이어는_다시_송금에_참여할_수_없다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        tracker.prepareRound(1);
        tracker.markUsed(1, 1L, 2L);

        // when
        boolean actual = tracker.canTransfer(1, 1L);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 서로_다른_라운드에서는_송금을_할_수_없다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        tracker.prepareRound(1);

        // when & then
        assertThatThrownBy(() -> tracker.validateAvailable(2, 1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("현재 라운드 정보와 송금 라운드가 일치하지 않습니다.");
    }

    @Test
    void 송금_참여자를_송금_추적_이력에_기록한다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();
        tracker.prepareRound(1);

        // when
        tracker.markUsed(1, 1L, 2L);

        // then
        assertAll(
                () -> assertThat(tracker.canTransfer(1, 1L)).isFalse(),
                () -> assertThat(tracker.canTransfer(1, 2L)).isFalse(),
                () -> assertThat(tracker.canTransfer(1, 3L)).isTrue()
        );
    }

    @Test
    void 이미_송금을_하거나_받은_플레이어는_다시_송금을_하거나_받을_수_없다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();
        tracker.prepareRound(1);
        tracker.markUsed(1, 1L, 2L);

        // when & then
        assertThatThrownBy(() -> tracker.markUsed(1, 1L, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("각 라운드에서는 각 플레이어가 한 번만 송금에 참여할 수 있습니다.");
    }

    @Test
    void 다른_라운드에서는_송금_이력을_저장할_수_없다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();
        tracker.prepareRound(1);

        // when & then
        assertThatThrownBy(() -> tracker.markUsed(2, 1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("현재 라운드 정보와 송금 라운드가 일치하지 않습니다.");
    }

    @Test
    void 라운드_준비는_반드시_순차적으로_실행되어야_한다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();

        // when & then
        assertThatThrownBy(() -> tracker.prepareRound(2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("다음 라운드 준비는 이전 라운드 정산 직후에만 실행되어야 합니다.");
    }

    @Test
    void 이전_라운드가_정산된_이후에만_다음_라운드를_준비할_수_있다() {
        // given
        TransferUsageTracker tracker = new TransferUsageTracker();
        tracker.prepareRound(1);

        // when & then
        assertThatThrownBy(() -> tracker.prepareRound(2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이전 라운드가 정산된 이후에만 다음 라운드를 준비할 수 있습니다.");
    }
}
