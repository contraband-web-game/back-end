package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RosterDraftTest {

    @Test
    void 팀_구성을_위한_임시_드래프트를_초기화_한다() {
        // when
        RosterDraft actual = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);

        // then
        assertThat(actual.players()).isEmpty();
    }

    @Test
    void 올바른_역할의_플레이어를_추가한다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        // when
        draft.add(profile);

        // then
        assertAll(
                () -> assertThat(draft.players()).hasSize(1),
                () -> assertThat(draft.players().get(0)).isEqualTo(profile),
                () -> assertThat(draft.hasPlayer(1L)).isTrue()
        );
    }

    @Test
    void 잘못된_역할의_플레이어는_추가할_수_없다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);
        PlayerProfile wrongProfile = PlayerProfile.create(1L, "플레이어1", TeamRole.INSPECTOR);

        // when & then
        assertThatThrownBy(() -> draft.add(wrongProfile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 팀 역할입니다.");
    }

    @Test
    void 최대_인원수를_초과하면_플레이어를_추가할_수_없다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 3);
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);

        for (long i = 0L; i < 3L; i++) {
            draft.add(PlayerProfile.create(i + 1L, "플레이어" + (i + 1L), TeamRole.SMUGGLER));
        }

        // when & then
        assertThatThrownBy(() -> draft.add(profile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("팀 인원은 최대 3명까지 가능합니다.");
    }

    @Test
    void 플레이어를_제거한다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);
        PlayerProfile profile1 = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);
        PlayerProfile profile2 = PlayerProfile.create(2L, "플레이어2", TeamRole.SMUGGLER);
        draft.add(profile1);
        draft.add(profile2);

        // when
        draft.remove(1L);

        // then
        assertAll(
                () -> assertThat(draft.players()).hasSize(1),
                () -> assertThat(draft.hasPlayer(1L)).isFalse(),
                () -> assertThat(draft.hasPlayer(2L)).isTrue()
        );
    }

    @Test
    void 존재하지_않는_플레이어_제거는_무시한다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);

        // when & then
        assertDoesNotThrow(() -> draft.remove(999L));
    }

    @Test
    void 특정_플레이어가_포함되어_있는지_확인한다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);
        draft.add(profile);

        // when & then
        assertAll(
                () -> assertThat(draft.hasPlayer(1L)).isTrue(),
                () -> assertThat(draft.hasPlayer(2L)).isFalse()
        );
    }

    @Test
    void 최소_인원_미달시_팀_로스터를_만들_수_없다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);

        // when & then
        assertThatThrownBy(() -> draft.toRoster())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("팀 인원은 최소 1명 이상이어야 합니다.");
    }

    @Test
    void 최소_인원_이상일_때_팀_로스터로_만든다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);

        draft.add(PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER));
        draft.add(PlayerProfile.create(2L, "플레이어2", TeamRole.SMUGGLER));

        // when
        TeamRoster actual = draft.toRoster();

        // then
        assertAll(
                () -> assertThat(actual.getName()).isEqualTo("밀수꾼 팀"),
                () -> assertThat(actual.getRole()).isEqualTo(TeamRole.SMUGGLER),
                () -> assertThat(actual.getPlayers()).hasSize(2)
        );
    }

    @Test
    void 플레이어_ID로_플레이어를_조회한다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);
        PlayerProfile profile = PlayerProfile.create(1L, "플레이어1", TeamRole.SMUGGLER);
        draft.add(profile);

        // when
        PlayerProfile actual = draft.getPlayer(1L);

        // then
        assertThat(actual).isEqualTo(profile);
    }

    @Test
    void 존재하지_않는_플레이어_ID로는_플레이어를_조회할_수_없다() {
        // given
        RosterDraft draft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, 4);

        // when & then
        assertThatThrownBy(() -> draft.getPlayer(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비에 존재하지 않는 플레이어입니다.");
    }
}
