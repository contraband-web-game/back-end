package com.game.contraband.domain.game.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.game.contraband.domain.game.vo.Money;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlayerProfileTest {

    @Test
    void 플레이어_프로필을_초기화한다() {
        // when
        PlayerProfile actual = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        // then
        assertAll(
                () -> assertThat(actual.getPlayerId()).isEqualTo(1L),
                () -> assertThat(actual.getName()).isEqualTo("플레이어1"),
                () -> assertThat(actual.getTeamRole()).isEqualTo(TeamRole.SMUGGLER)
        );
    }

    @Test
    void 프로필로_게임_플레이어를_생성한다() {
        // given
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        // when
        Player actual = profile.toPlayer(Money.startingAmount());

        // then
        assertAll(
                () -> assertThat(actual.getId()).isEqualTo(1L),
                () -> assertThat(actual.getName()).isEqualTo("플레이어1"),
                () -> assertThat(actual.getTeamRole()).isEqualTo(TeamRole.SMUGGLER),
                () -> assertThat(actual.getBalance().getAmount()).isEqualTo(3_000)
        );
    }

    private static Stream<Arguments> smugglerTeamTestArguments() {
        return Stream.of(
                Arguments.of(TeamRole.SMUGGLER, true),
                Arguments.of(TeamRole.INSPECTOR, false)
        );
    }

    @ParameterizedTest(name = "{0} 역할일 때 {1}을 반환한다")
    @MethodSource("smugglerTeamTestArguments")
    void 밀수범_팀인지_확인한다(TeamRole teamRole, boolean expected) {
        // given
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", teamRole);

        // when
        boolean actual = profile.isSmugglerTeam();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> inspectorTeamTestArguments() {
        return Stream.of(
                Arguments.of(TeamRole.SMUGGLER, false),
                Arguments.of(TeamRole.INSPECTOR, true)
        );
    }

    @ParameterizedTest(name = "{0} 역할일 때 {1}을 반환한다")
    @MethodSource("inspectorTeamTestArguments")
    void 검문관_팀인지_확인한다(TeamRole teamRole, boolean expected) {
        // given
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", teamRole);

        // when
        boolean actual = profile.isInspectorTeam();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void 플레이어_ID가_같은지_확인한다() {
        // given
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        // when
        boolean actual = profile.isEqualId(1L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 플레이어의_팀과_동일한_팀_역할인지_확인한다() {
        // given
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        // when
        boolean actual = profile.isSameRole(TeamRole.SMUGGLER);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 플레이어의_팀과_다른_팀_역할인지_확인한다() {
        // given
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        // when
        boolean actual = profile.isDifferentRole(TeamRole.INSPECTOR);

        // then
        assertThat(actual).isTrue();
    }
}
