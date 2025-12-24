package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyPhaseTest {

    private static Stream<Arguments> isLobbyTestArguments() {
        return Stream.of(
                Arguments.of(LobbyPhase.LOBBY, true),
                Arguments.of(LobbyPhase.IN_PROGRESS, false),
                Arguments.of(LobbyPhase.FINISHED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isLobbyTestArguments")
    void 로비_상태인지_확인한다(LobbyPhase phase, boolean expected) {
        // when
        boolean actual = phase.isLobby();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotLobbyTestArguments() {
        return Stream.of(
                Arguments.of(LobbyPhase.LOBBY, false),
                Arguments.of(LobbyPhase.IN_PROGRESS, true),
                Arguments.of(LobbyPhase.FINISHED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotLobbyTestArguments")
    void 로비_상태가_아닌지_확인한다(LobbyPhase phase, boolean expected) {
        // when
        boolean actual = phase.isNotLobby();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isFinishedTestArguments() {
        return Stream.of(
                Arguments.of(LobbyPhase.LOBBY, false),
                Arguments.of(LobbyPhase.IN_PROGRESS, false),
                Arguments.of(LobbyPhase.FINISHED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isFinishedTestArguments")
    void 로비가_종료되었는지_확인한다(LobbyPhase phase, boolean expected) {
        // when
        boolean actual = phase.isFinished();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
