package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyGuardsTest {

    @Test
    void 로비_상태가_아니면_로스터를_수정할_수_없다() {
        // given
        LobbyGuards guards = LobbyGuards.create();

        // when & then
        assertThatThrownBy(() -> guards.requireLobbyPhase(LobbyPhase.IN_PROGRESS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로비 상태에서만 로스터를 수정할 수 있습니다.");
    }

    @Test
    void 로비의_호스트가_아니라면_검증에_실패한다() {
        // given
        LobbyGuards guards = LobbyGuards.create();

        // when & then
        assertThatThrownBy(() -> guards.requireHost(2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장이 아닙니다.");
    }

    @Test
    void 호스트_검증_시_비어_있는_호스트_ID를_전달하면_검증에_실패한다() {
        // given
        LobbyGuards guards = LobbyGuards.create();

        // when & then
        assertThatThrownBy(() -> guards.requireHost(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장이 아닙니다.");
    }

    @Test
    void 호스트_검증_실패_시_발생하는_실패_메시지를_지정할_수_있다() {
        // given
        LobbyGuards guards = LobbyGuards.create();

        // when & then
        assertThatThrownBy(() -> guards.requireHost(2L, 1L, "방장만 강퇴할 수 있습니다."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장만 강퇴할 수 있습니다.");
    }
}
