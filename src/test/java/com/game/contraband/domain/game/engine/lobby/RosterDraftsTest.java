package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RosterDraftsTest {

    @Test
    void 플레이어를_추가한다() {
        // given
        RosterDrafts drafts = RosterDrafts.create(2);

        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        // when
        drafts.addSmuggler(smuggler);
        drafts.addInspector(inspector);

        // then
        assertAll(
                () -> assertThat(drafts.smugglerPlayers()).containsExactly(smuggler),
                () -> assertThat(drafts.inspectorPlayers()).containsExactly(inspector),
                () -> assertThat(drafts.totalPlayerCount()).isEqualTo(2)
        );
    }

    @Test
    void 팀을_토글하면_반대_팀으로_이동한다() {
        // given
        RosterDrafts drafts = RosterDrafts.create(2);
        PlayerProfile smuggler = PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);
        drafts.addSmuggler(smuggler);
        drafts.addInspector(inspector);

        // when
        drafts.toggleTeam(1L);

        // then
        assertAll(
                () -> assertThat(drafts.smugglerPlayers()).isEmpty(),
                () -> assertThat(drafts.inspectorPlayers())
                        .extracting(PlayerProfile::getPlayerId)
                        .containsExactlyInAnyOrder(1L, 2L)
        );
    }

    @Test
    void 상대_팀이_가득차면_토글할_수_없다() {
        // given
        RosterDrafts drafts = RosterDrafts.create(1);

        drafts.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));
        drafts.addSmuggler(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER));

        // when & then
        assertThatThrownBy(() -> drafts.toggleTeam(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("검사관 팀 인원이 꽉 찼습니다.");
    }

    @Test
    void 최대_팀_인원을_현재_팀_인원보다_더_큰_경우로_바꿀_수_있는지_확인한다() {
        // given
        RosterDrafts drafts = RosterDrafts.create(3);

        drafts.addSmuggler(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER));
        drafts.addSmuggler(PlayerProfile.create(3L, "밀수꾼2", TeamRole.SMUGGLER));
        drafts.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        // when
        boolean actual = drafts.canResizeTo(3);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 최대_팀_인원을_현재_팀_인원과_같은_경우로_바꿀_수_있는지_확인한다() {
        // given
        RosterDrafts drafts = RosterDrafts.create(3);

        drafts.addSmuggler(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER));
        drafts.addSmuggler(PlayerProfile.create(3L, "밀수꾼2", TeamRole.SMUGGLER));
        drafts.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        // when
        boolean actual = drafts.canResizeTo(2);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 최대_팀_인원을_현재_팀_인원보다_적은_경우로_바꿀_수_없는지_확인한다() {
        // given
        RosterDrafts drafts = RosterDrafts.create(3);

        drafts.addSmuggler(PlayerProfile.create(1L, "밀수꾼", TeamRole.SMUGGLER));
        drafts.addSmuggler(PlayerProfile.create(3L, "밀수꾼2", TeamRole.SMUGGLER));
        drafts.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        // when
        boolean actual = drafts.cannotResizeTo(1);

        // then
        assertThat(actual).isTrue();
    }
}
