package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyMetadataTest {

    @Test
    void 로비_메타데이터를_초기화_한다() {
        // when
        LobbyMetadata actual = LobbyMetadata.create(1L, "게임방", 10L, 6);

        // then
        assertAll(
                () -> assertThat(actual.getId()).isEqualTo(1L),
                () -> assertThat(actual.getName()).isEqualTo("게임방"),
                () -> assertThat(actual.getHostId()).isEqualTo(10L),
                () -> assertThat(actual.getMaxPlayerCount()).isEqualTo(6),
                () -> assertThat(actual.maxTeamSize()).isEqualTo(3)
        );
    }

    @Test
    void 로비_메타데이터_초기화_시_이름이_비어있으면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> LobbyMetadata.create(1L, "  ", 10L, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방 이름은 비어 있을 수 없습니다.");
    }

    @Test
    void 로비_메타데이터_초기화_시_호스트_ID가_비어_있으면_초기화할_수_없다() {
        // when & then
        assertThatThrownBy(() -> LobbyMetadata.create(1L, "게임방", null, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장은 필수입니다.");
    }

    @Test
    void 로비_메타데이터_초기화_시_로비_최대_인원이_2명_이하라면_초기화할_수_없다() {
        // when & then
        assertThatThrownBy(() -> LobbyMetadata.create(1L, "게임방", 10L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비 최대 인원은 2명 이상이고 짝수여야 합니다.");
    }

    @Test
    void 로비_메타데이터_초기화_시_로비_최대_인원이_홀수라면_초기화할_수_없다() {
        // when & then
        assertThatThrownBy(() -> LobbyMetadata.create(1L, "게임방", 10L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비 최대 인원은 2명 이상이고 짝수여야 합니다.");
    }

    @Test
    void 로비_최대_인원을_변경한다() {
        // given
        LobbyMetadata metadata = LobbyMetadata.create(1L, "게임방", 10L, 6);

        // when
        LobbyMetadata actual = metadata.withMaxPlayerCount(8);

        // then
        assertAll(
                () -> assertThat(actual.getMaxPlayerCount()).isEqualTo(8),
                () -> assertThat(metadata.getMaxPlayerCount()).isEqualTo(6),
                () -> assertThat(actual.getName()).isEqualTo(metadata.getName()),
                () -> assertThat(actual.getHostId()).isEqualTo(metadata.getHostId()),
                () -> assertThat(actual.maxTeamSize()).isEqualTo(4)
        );
    }

    private static Stream<Arguments> isHostTestArguments() {
        return Stream.of(
                Arguments.of(10L, true),
                Arguments.of(11L, false),
                Arguments.of(null, false)
        );
    }

    @ParameterizedTest(name = "{0} 플레이어가 방장인지 여부는 {1}을 반환한다")
    @MethodSource("isHostTestArguments")
    void 플레이어가_방장인지_확인한다(Long playerId, boolean expected) {
        // given
        LobbyMetadata metadata = LobbyMetadata.create(1L, "게임방", 10L, 6);

        // when
        boolean actual = metadata.isHost(playerId);

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
