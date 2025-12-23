package com.game.contraband.domain.game.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GameStatusTest {

    private static Stream<Arguments> isStartedTestArguments() {
        return Stream.of(
                Arguments.of(GameStatus.NOT_STARTED, false),
                Arguments.of(GameStatus.IN_PROGRESS, true),
                Arguments.of(GameStatus.FINISHED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isStartedTestArguments")
    void 게임이_시작되었는지_확인한다(GameStatus status, boolean expected) {
        // when
        boolean actual = status.isStarted();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotStartedTestArguments() {
        return Stream.of(
                Arguments.of(GameStatus.NOT_STARTED, true),
                Arguments.of(GameStatus.IN_PROGRESS, false),
                Arguments.of(GameStatus.FINISHED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotStartedTestArguments")
    void 게임이_시작되지_않은지_확인한다(GameStatus status, boolean expected) {
        // when
        boolean actual = status.isNotStarted();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isInProgressTestArguments() {
        return Stream.of(
                Arguments.of(GameStatus.NOT_STARTED, false),
                Arguments.of(GameStatus.IN_PROGRESS, true),
                Arguments.of(GameStatus.FINISHED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isInProgressTestArguments")
    void 게임이_진행중인지_확인한다(GameStatus status, boolean expected) {
        // when
        boolean actual = status.isInProgress();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotInProgressTestArguments() {
        return Stream.of(
                Arguments.of(GameStatus.NOT_STARTED, true),
                Arguments.of(GameStatus.IN_PROGRESS, false),
                Arguments.of(GameStatus.FINISHED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotInProgressTestArguments")
    void 게임이_진행중이지_않은지_확인한다(GameStatus status, boolean expected) {
        // when
        boolean actual = status.isNotInProgress();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isFinishedTestArguments() {
        return Stream.of(
                Arguments.of(GameStatus.NOT_STARTED, false),
                Arguments.of(GameStatus.IN_PROGRESS, false),
                Arguments.of(GameStatus.FINISHED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isFinishedTestArguments")
    void 게임이_완료되었는지_확인한다(GameStatus status, boolean expected) {
        // when
        boolean actual = status.isFinished();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotFinishedTestArguments() {
        return Stream.of(
                Arguments.of(GameStatus.NOT_STARTED, true),
                Arguments.of(GameStatus.IN_PROGRESS, true),
                Arguments.of(GameStatus.FINISHED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotFinishedTestArguments")
    void 게임이_완료되지_않은지_확인한다(GameStatus status, boolean expected) {
        // when
        boolean actual = status.isNotFinished();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
