package com.game.contraband.domain.game.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TeamRosterTest {

    @Test
    void 팀_명단을_생성한다() {
        // given
        List<PlayerProfile> players = List.of(
                PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER),
                PlayerProfile.create(2L, "플레이어2", TeamRole.SMUGGLER)
        );

        // when
        TeamRoster actual = TeamRoster.create(1L, "밀수범팀", TeamRole.SMUGGLER, players);

        // then
        assertAll(
                () -> assertThat(actual.getId()).isEqualTo(1L),
                () -> assertThat(actual.getName()).isEqualTo("밀수범팀"),
                () -> assertThat(actual.getRole()).isEqualTo(TeamRole.SMUGGLER),
                () -> assertThat(actual.getPlayers()).hasSize(2)
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
        List<PlayerProfile> players = List.of(
                PlayerProfile.create(1L, "플레이어1", teamRole)
        );
        TeamRoster teamRoster = TeamRoster.create(1L, "팀1", teamRole, players);

        // when
        boolean actual = teamRoster.isSmugglerTeam();

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
        List<PlayerProfile> players = List.of(
                PlayerProfile.create(1L, "플레이어1", teamRole)
        );
        TeamRoster teamRoster = TeamRoster.create(1L, "팀1", teamRole, players);

        // when
        boolean actual = teamRoster.isInspectorTeam();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void 특정_플레이어가_팀에_포함되어_있는지_확인한다() {
        // given
        List<PlayerProfile> players = List.of(
                PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER),
                PlayerProfile.create(2L, "플레이어2", TeamRole.SMUGGLER)
        );
        TeamRoster teamRoster = TeamRoster.create(1L, "밀수범팀", TeamRole.SMUGGLER, players);

        // when
        boolean actual = teamRoster.hasPlayer(1L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 특정_플레이어가_팀에_포함되어_있지_않은지_확인한다() {
        // given
        List<PlayerProfile> players = List.of(
                PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER),
                PlayerProfile.create(2L, "플레이어2", TeamRole.SMUGGLER)
        );
        TeamRoster teamRoster = TeamRoster.create(1L, "밀수범팀", TeamRole.SMUGGLER, players);

        // when
        boolean actual = teamRoster.lacksPlayer(-999L);

        // then
        assertThat(actual).isTrue();
    }
}
