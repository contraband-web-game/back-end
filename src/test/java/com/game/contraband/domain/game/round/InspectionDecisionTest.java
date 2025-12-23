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
class InspectionDecisionTest {

    private static Stream<Arguments> isNoneTestArguments() {
        return Stream.of(
                Arguments.of(InspectionDecision.NONE, true),
                Arguments.of(InspectionDecision.PASS, false),
                Arguments.of(InspectionDecision.INSPECTION, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNoneTestArguments")
    void 검문_미결정_여부를_확인한다(InspectionDecision decision, boolean expected) {
        // when
        boolean actual = decision.isNone();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotNoneTestArguments() {
        return Stream.of(
                Arguments.of(InspectionDecision.NONE, false),
                Arguments.of(InspectionDecision.PASS, true),
                Arguments.of(InspectionDecision.INSPECTION, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotNoneTestArguments")
    void 검문_결정_여부를_확인한다(InspectionDecision decision, boolean expected) {
        // when
        boolean actual = decision.isNotNone();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isPassTestArguments() {
        return Stream.of(
                Arguments.of(InspectionDecision.NONE, false),
                Arguments.of(InspectionDecision.PASS, true),
                Arguments.of(InspectionDecision.INSPECTION, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isPassTestArguments")
    void 통과를_선택했는지_확인한다(InspectionDecision decision, boolean expected) {
        // when
        boolean actual = decision.isPass();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotPassTestArguments() {
        return Stream.of(
                Arguments.of(InspectionDecision.NONE, true),
                Arguments.of(InspectionDecision.PASS, false),
                Arguments.of(InspectionDecision.INSPECTION, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotPassTestArguments")
    void 통과가_아닌지_확인한다(InspectionDecision decision, boolean expected) {
        // when
        boolean actual = decision.isNotPass();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isInspectionTestArguments() {
        return Stream.of(
                Arguments.of(InspectionDecision.NONE, false),
                Arguments.of(InspectionDecision.PASS, false),
                Arguments.of(InspectionDecision.INSPECTION, true)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isInspectionTestArguments")
    void 검문을_선택했는지_확인한다(InspectionDecision decision, boolean expected) {
        // when
        boolean actual = decision.isInspection();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> isNotInspectionTestArguments() {
        return Stream.of(
                Arguments.of(InspectionDecision.NONE, true),
                Arguments.of(InspectionDecision.PASS, true),
                Arguments.of(InspectionDecision.INSPECTION, false)
        );
    }

    @ParameterizedTest(name = "{0} 상태일 때 {1}을 반환한다")
    @MethodSource("isNotInspectionTestArguments")
    void 검문을_선택하지_않았는지_확인한다(InspectionDecision decision, boolean expected) {
        // when
        boolean actual = decision.isNotInspection();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
