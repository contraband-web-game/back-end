package com.game.contraband.domain.game.round;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoundStatusTest {

    private static Stream<Arguments> isNewTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, true),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, false),
                Arguments.of(RoundStatus.INSPECTION_DECISION_DECLARED, false),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNewTestArguments")
    void 새_라운드인지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isNew();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotNewTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, false),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECISION_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotNewTestArguments")
    void 새_라운드가_아닌지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isNotNew();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isSmuggleDeclaredTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, false),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECISION_DECLARED, false),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isSmuggleDeclaredTestArguments")
    void 밀수가_신고된_라운드인지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isSmuggleDeclared();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotSmuggleDeclaredTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, true),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, false),
                Arguments.of(RoundStatus.INSPECTION_DECISION_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotSmuggleDeclaredTestArguments")
    void 밀수가_신고되지_않은_라운드인지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isNotSmuggleDeclared();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isInspectionDecidedTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, false),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, false),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isInspectionDecidedTestArguments")
    void 검문이_결정된_라운드인지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isInspectionDecided();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotInspectionDecidedTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, true),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECISION_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotInspectionDecidedTestArguments")
    void 검문이_결정되지_않은_라운드인지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isNotInspectionDecided();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isInspectionDecisionDeclaredTestArguments() {
        return Stream.of(
                Arguments.of(RoundStatus.NEW, false),
                Arguments.of(RoundStatus.SMUGGLE_DECLARED, false),
                Arguments.of(RoundStatus.INSPECTION_DECISION_DECLARED, true),
                Arguments.of(RoundStatus.INSPECTION_DECIDED, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 검사관 선택 도중인지 확인한다")
    @MethodSource("isInspectionDecisionDeclaredTestArguments")
    void 검사관_선택이_선언된_라운드인지_확인한다(RoundStatus status, boolean expected) {
        // when
        boolean actual = status.isInspectionDecisionDeclared();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
