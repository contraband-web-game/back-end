package com.game.contraband.domain.game.player;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TeamRoleTest {

    private static Stream<Arguments> isSmugglerMethodTestArguments() {
        return Stream.of(
                Arguments.of(TeamRole.SMUGGLER, true),
                Arguments.of(TeamRole.INSPECTOR, false)
        );
    }

    @ParameterizedTest(name = "팀 역할이 {0}일 때 {1}을 반환한다")
    @MethodSource("isSmugglerMethodTestArguments")
    void 밀수꾼_팀인지_확인한다(TeamRole teamRole, boolean expected) {
        // when
        boolean actual = teamRole.isSmuggler();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isInspectorMethodTestArguments() {
        return Stream.of(
                Arguments.of(TeamRole.SMUGGLER, false),
                Arguments.of(TeamRole.INSPECTOR, true)
        );
    }

    @ParameterizedTest(name = "팀 역할이 {0}일 때 {1}을 반환한다")
    @MethodSource("isInspectorMethodTestArguments")
    void 검사관_팀인지_확인한다(TeamRole teamRole, boolean expected) {
        // when
        boolean actual = teamRole.isInspector();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
