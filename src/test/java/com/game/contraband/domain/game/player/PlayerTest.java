package com.game.contraband.domain.game.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.game.contraband.domain.game.vo.Money;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlayerTest {

    @Test
    void 플레이어를_초기화_한다() {
        // when
        Player actual = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));

        // then
        assertAll(
                () -> assertThat(actual.getId()).isEqualTo(1L),
                () -> assertThat(actual.getName()).isEqualTo("플레이어1"),
                () -> assertThat(actual.getTeamRole()).isEqualTo(TeamRole.SMUGGLER),
                () -> assertThat(actual.getBalance().getAmount()).isEqualTo(1_000)
        );
    }

    @Test
    void 플레이어의_잔액을_변경한다() {
        // given
        Player original = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));
        Money newBalance = Money.from(2_000);

        // when
        Player actual = original.withBalance(newBalance);

        // then
        assertAll(
                () -> assertThat(actual).isNotSameAs(original),
                () -> assertThat(actual.getId()).isEqualTo(original.getId()),
                () -> assertThat(actual.getName()).isEqualTo(original.getName()),
                () -> assertThat(actual.getTeamRole()).isEqualTo(original.getTeamRole()),
                () -> assertThat(actual.getBalance()).isEqualTo(newBalance),
                () -> assertThat(original.getBalance().getAmount()).isEqualTo(1_000)
        );
    }

    @Test
    void 플레이어의_잔액을_증가시킨다() {
        // given
        Player original = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));

        // when
        Player actual = original.plusBalance(Money.from(500));

        // then
        assertAll(
                () -> assertThat(actual.getBalance().getAmount()).isEqualTo(1_500),
                () -> assertThat(original.getBalance().getAmount()).isEqualTo(1_000)
        );
    }

    @Test
    void 플레이어의_잔액을_감소시킨다() {
        // given
        Player original = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(2_000));

        // when
        Player actual = original.minusBalance(Money.from(500));

        // then
        assertAll(
                () -> assertThat(actual.getBalance().getAmount()).isEqualTo(1_500),
                () -> assertThat(original.getBalance().getAmount()).isEqualTo(2_000)
        );
    }

    @Test
    void 잔액이_송금액_보다_많거나_같으면_송금_가능함을_알린다() {
        // given
        Player player = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(2_000));

        // when
        boolean actual = player.canTransfer(Money.from(1_000));

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 잔액이_송금액_보다_적으면_송금_불가능함을_알린다() {
        // given
        Player player = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(800));

        // when
        boolean actual = player.cannotTransfer(Money.from(1_000));

        // then
        assertThat(actual).isTrue();
    }


    @Test
    void 밀수범_팀인지_확인한다() {
        // given
        Player player = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));

        // when
        boolean actual = player.isSmugglerTeam();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 검문관_팀인지_확인한다() {
        // given
        Player player = Player.create(1L, "플레이어1", TeamRole.INSPECTOR, Money.from(1_000));

        // when
        boolean actual = player.isInspectorTeam();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void ID가_같은지_확인한다() {
        // given
        Player player = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));

        // when
        boolean actual = player.isEqualId(1L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void ID가_다른지_확인한다() {
        // given
        Player player = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));

        // when
        boolean actual = player.isNotEqualId(1L);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 플레이어의_동등성을_ID로_확인한다() {
        // given
        Player player1 = Player.create(1L, "플레이어1", TeamRole.SMUGGLER, Money.from(1_000));
        Player player2 = Player.create(1L, "다른이름", TeamRole.INSPECTOR, Money.from(2_000));

        // when & then
        assertAll(
                () -> assertThat(player1).isEqualTo(player2),
                () -> assertThat(player1).hasSameHashCodeAs(player2)
        );
    }
}
