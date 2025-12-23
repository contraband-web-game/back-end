package com.game.contraband.domain.game.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void 플레이어를_로스터에_추가한다() {
        // given
        TeamRoster original = TeamRoster.create(1L, "밀수범팀", TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER)));
        PlayerProfile newPlayer = PlayerProfile.create(3L, "S3", TeamRole.SMUGGLER);

        // when
        TeamRoster actual = original.addPlayer(newPlayer);

        // then
        assertThat(actual.hasPlayer(3L)).isTrue();
    }

    @Test
    void 역할이_다른_플레이어는_추가할_수_없다() {
        // given
        TeamRoster roster = TeamRoster.create(
                1L,
                "밀수범팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER))
        );
        PlayerProfile wrongRolePlayer = PlayerProfile.create(2L, "I2", TeamRole.INSPECTOR);

        // when & then
        assertThatThrownBy(() -> roster.addPlayer(wrongRolePlayer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("플레이어 역할이 로스터와 일치하지 않습니다.");
    }

    @Test
    void 이미_포함된_플레이어를_중복해서_추가할_수_없다() {
        // given
        TeamRoster roster = TeamRoster.create(
                1L,
                "밀수범팀",
                TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER))
        );
        PlayerProfile duplicatePlayer = PlayerProfile.create(1L, "중복", TeamRole.SMUGGLER);

        // when & then
        assertThatThrownBy(() -> roster.addPlayer(duplicatePlayer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 로스터에 포함된 플레이어입니다");
    }

    @Test
    void 플레이어를_제거한_새로운_로스터를_반환한다() {
        // given
        TeamRoster original = TeamRoster.create(1L, "밀수범팀", TeamRole.SMUGGLER,
                List.of(
                        PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER),
                        PlayerProfile.create(2L, "S2", TeamRole.SMUGGLER)
                )
        );

        // when
        TeamRoster actual = original.removePlayer(2L);

        // then
        assertThat(actual.hasPlayer(2L)).isFalse();
    }

    @Test
    void 존재하지_않는_플레이어를_제거하면_예외를_던진다() {
        // given
        TeamRoster roster = TeamRoster.create(1L, "밀수범팀", TeamRole.SMUGGLER,
                List.of(PlayerProfile.create(1L, "S1", TeamRole.SMUGGLER)));

        // when & then
        assertThatThrownBy(() -> roster.removePlayer(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로스터에 존재하지 않는 플레이어입니다.");
    }
}
