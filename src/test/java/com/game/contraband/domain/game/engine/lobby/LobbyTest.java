package com.game.contraband.domain.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.vo.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyTest {

    @Test
    void 밀수꾼_방장으로_로비를_생성한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);

        // when
        Lobby actual = Lobby.create(1L, "게임방", hostProfile, 6);

        // then
        assertAll(
                () -> assertThat(actual.getId()).isEqualTo(1L),
                () -> assertThat(actual.getName()).isEqualTo("게임방"),
                () -> assertThat(actual.getHostId()).isEqualTo(1L),
                () -> assertThat(actual.getPhase()).isEqualTo(LobbyPhase.LOBBY),
                () -> assertThat(actual.getSmugglerDraft()).hasSize(1),
                () -> assertThat(actual.getInspectorDraft()).isEmpty(),
                () -> assertThat(actual.getReadyStates()).containsEntry(1L, false)
        );
    }

    @Test
    void 검사관_방장으로_로비를_생성한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.INSPECTOR);

        // when
        Lobby actual = Lobby.create(1L, "게임방", hostProfile, 6);

        // then
        assertAll(
                () -> assertThat(actual.getHostId()).isEqualTo(1L),
                () -> assertThat(actual.getPhase()).isEqualTo(LobbyPhase.LOBBY),
                () -> assertThat(actual.getSmugglerDraft()).isEmpty(),
                () -> assertThat(actual.getInspectorDraft()).hasSize(1)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void 방_이름이_비어_있으면_생성할_수_없다(String name) {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);

        // when & then
        assertThatThrownBy(() -> Lobby.create(1L, name, hostProfile, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방 이름은 비어 있을 수 없습니다.");
    }

    @Test
    void 방장_정보가_없으면_생성할_수_없다() {
        // when & then
        assertThatThrownBy(() -> Lobby.create(1L, "게임방", null, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장은 필수입니다.");
    }

    @Test
    void 최대_참여_인원이_짝수가_아니면_생성할_수_없다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);

        // when & then
        assertThatThrownBy(() -> Lobby.create(1L, "게임방", hostProfile, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비 최대 인원은 2명 이상이고 짝수여야 합니다.");
    }

    @Test
    void 방장은_최대_참여_인원을_변경할_수_있다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        // when
        lobby.changeMaxPlayerCount(2, 1L);

        // then
        assertThat(lobby.getMaxPlayerCount()).isEqualTo(2);
    }

    @Test
    void 방장이_아니면_최대_참여_인원을_변경할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        lobby.addInspector(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR));

        // when & then
        assertThatThrownBy(() -> lobby.changeMaxPlayerCount(8, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장이 아닙니다.");
    }

    @Test
    void 팀_보다_작은_최대_참여_인원으로_변경할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        lobby.addSmuggler(PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER));
        lobby.addSmuggler(PlayerProfile.create(5L, "밀수꾼2", TeamRole.SMUGGLER));

        // when & then
        assertThatThrownBy(() -> lobby.changeMaxPlayerCount(4, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("새 최대 팀 정원은 현재 팀 구성보다 작을 수 없습니다.");
    }

    @Test
    void 플레이어가_팀을_토글해_팀이_변경된다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile additionalSmuggler = PlayerProfile.create(2L, "밀수꾼2", TeamRole.SMUGGLER);

        lobby.addSmuggler(additionalSmuggler);

        // when
        lobby.toggleTeam(1L);

        // then
        assertAll(
                () -> assertThat(lobby.getSmugglerDraft()).hasSize(1),
                () -> assertThat(lobby.getInspectorDraft()).hasSize(1),
                () -> assertThat(lobby.getInspectorDraft()).extracting(PlayerProfile::getPlayerId).contains(1L)
        );

        // when
        lobby.toggleTeam(1L);

        // then
        assertThat(lobby.getSmugglerDraft()).extracting(PlayerProfile::getPlayerId).contains(1L);
    }

    @Test
    void 팀_정원이_가득하면_팀을_토글할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        lobby.addInspector(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR));
        lobby.addInspector(PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR));
        lobby.addInspector(PlayerProfile.create(6L, "검사관3", TeamRole.INSPECTOR));

        // when & then
        assertThatThrownBy(() -> lobby.toggleTeam(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("검사관 팀 인원이 꽉 찼습니다.");
    }

    @Test
    void 플레이어_프로필_정보를_조회한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);
        lobby.addInspector(inspector);

        // when
        PlayerProfile actualSmuggler = lobby.findPlayerProfile(1L);
        PlayerProfile actualInspector = lobby.findPlayerProfile(2L);

        // then
        assertThat(actualSmuggler.getTeamRole()).isEqualTo(TeamRole.SMUGGLER);
        assertThat(actualInspector.getTeamRole()).isEqualTo(TeamRole.INSPECTOR);
    }

    @Test
    void 로비에_참여하지_않은_플레이어의_프로필_정보는_조회할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        // when & then
        assertThatThrownBy(() -> lobby.findPlayerProfile(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비에 존재하지 않는 플레이어입니다.");
    }

    @Test
    void 밀수꾼을_추가한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(1L, "게임방", hostProfile, 6);
        PlayerProfile smuggler = PlayerProfile.create(2L, "밀수꾼", TeamRole.SMUGGLER);

        // when
        lobby.addSmuggler(smuggler);

        // then
        assertThat(lobby.getSmugglerDraft()).hasSize(1);
    }

    @Test
    void 검사관을_추가한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);
        Lobby lobby = Lobby.create(1L, "게임방", hostProfile, 6);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        // when
        lobby.addInspector(inspector);

        // then
        assertThat(lobby.getInspectorDraft()).hasSize(1);
    }

    @Test
    void 로비에_참여중인_밀수꾼을_제거한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);
        Lobby lobby = Lobby.create(1L, "게임방", hostProfile, 6);
        PlayerProfile smuggler = PlayerProfile.create(2L, "밀수꾼", TeamRole.SMUGGLER);

        lobby.addSmuggler(smuggler);

        // when
        lobby.removeSmuggler(2L);

        // then
        assertThat(lobby.getSmugglerDraft()).hasSize(1);
    }

    @Test
    void 로비에_참여중인_검사관을_제거한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.INSPECTOR);
        Lobby lobby = Lobby.create(1L, "게임방", hostProfile, 6);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        lobby.addInspector(inspector);

        // when
        lobby.removeInspector(2L);

        // then
        assertThat(lobby.getInspectorDraft()).hasSize(1);
    }

    @Test
    void 로비의_호스트만이_로비를_삭제할_수_있다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        // when
        lobby.deleteLobby(1L);

        // then
        assertAll(
                () -> assertThat(lobby.getPhase()).isEqualTo(LobbyPhase.FINISHED),
                () -> assertThat(lobby.getReadyStates()).isEmpty()
        );
    }

    @Test
    void 로비의_호스트가_아니라면_로비를_삭제할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        // when & then
        assertThatThrownBy(() -> lobby.deleteLobby(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장이 아닙니다.");
    }

    @Test
    void 로비에서_ready_상태로_변경한다() {
        // given
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);
        Lobby lobby = Lobby.create(1L, "게임방", hostProfile, 6);

        // when
        lobby.toggleReady(1L);

        // then
        assertThat(lobby.getReadyStates().get(1L)).isTrue();
    }

    @Test
    void ready_상태의_플레이어는_팀을_변경할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        lobby.toggleReady(1L);

        // when & then
        assertThatThrownBy(() -> lobby.removeSmuggler(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ready 상태에서는 팀을 변경할 수 없습니다.");
    }

    @Test
    void 로비에_없는_플레이어는_ready를_토글할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        // when & then
        assertThatThrownBy(() -> lobby.toggleReady(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비에 존재하지 않는 플레이어입니다.");
    }

    @Test
    void 게임_시작_후에는_플레이어를_추가할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler = PlayerProfile.create(3L, "밀수범1", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler);
        lobby.addInspector(inspector);
        lobby.addInspector(inspector2);
        lobby.toggleReady(1L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);
        lobby.toggleReady(3L);
        lobby.startGame(3, 1L);

        // when & then
        assertThatThrownBy(() -> lobby.addSmuggler(PlayerProfile.create(5L, "밀수범3", TeamRole.SMUGGLER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로비 상태에서만 로스터를 수정할 수 있습니다.");
    }

    @Test
    void 모든_플레이어가_ready이면_게임을_시작한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler = PlayerProfile.create(3L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler);
        lobby.addInspector(inspector);
        lobby.addInspector(inspector2);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);

        // when
        ContrabandGame actual = lobby.startGame(3, 1L);

        // then
        assertAll(
                () -> assertThat(lobby.getPhase()).isEqualTo(LobbyPhase.IN_PROGRESS),
                () -> assertThat(actual).isNotNull()
        );
    }

    @Test
    void 팀_인원수가_다르면_게임을_시작할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler1 = PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(5L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler1);
        lobby.addSmuggler(smuggler2);
        lobby.addInspector(inspector1);
        lobby.addInspector(inspector2);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(5L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);

        // when & then
        assertThatThrownBy(() -> lobby.startGame(3, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("두 팀의 인원 수가 같아야 게임을 시작할 수 있습니다.");
    }

    @Test
    void 모든_플레이어가_ready가_아니면_게임을_시작할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler = PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler);
        lobby.addInspector(inspector1);
        lobby.addInspector(inspector2);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(4L);

        // when & then
        assertThatThrownBy(() -> lobby.startGame(3, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("모든 플레이어가 준비 완료(ready) 상태여야 게임을 시작할 수 있습니다.");
    }

    @Test
    void 방장만_게임을_시작할_수_있다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler = PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler);
        lobby.addInspector(inspector);
        lobby.addInspector(inspector2);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);

        // when & then
        assertThatThrownBy(() -> lobby.startGame(3, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장이 아닙니다.");
    }

    @Test
    void 방장은_강퇴할_수_있다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler = PlayerProfile.create(2L, "밀수꾼", TeamRole.SMUGGLER);
        lobby.addSmuggler(smuggler);

        // when
        lobby.kick(1L, 2L);

        // then
        assertAll(
                () -> assertThat(lobby.getSmugglerDraft()).hasSize(1),
                () -> assertThat(lobby.getReadyStates()).doesNotContainKey(2L)
        );
    }

    @Test
    void 방장이_아니면_강퇴할_수_없다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        lobby.addInspector(inspector);

        // when & then
        assertThatThrownBy(() -> lobby.kick(2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방장만 강퇴할 수 있습니다.");
    }

    @Test
    void 게임_시작_후_밀수_게임의_밀수꾼_플레이어들을_조회한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler1 = PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(5L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);
        PlayerProfile inspector3 = PlayerProfile.create(6L, "검사관3", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler1);
        lobby.addSmuggler(smuggler2);
        lobby.addInspector(inspector1);
        lobby.addInspector(inspector2);
        lobby.addInspector(inspector3);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(5L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);
        lobby.toggleReady(6L);
        lobby.startGame(3, 1L);

        // when
        List<PlayerProfile> actual = lobby.getSmugglerDraft();

        // then
        assertAll(
                () -> assertThat(actual).hasSize(3),
                () -> assertThat(actual).contains(smuggler1, smuggler2)
        );
    }

    @Test
    void 진행_중인_게임을_조회한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler1 = PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(5L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);
        PlayerProfile inspector3 = PlayerProfile.create(6L, "검사관3", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler1);
        lobby.addSmuggler(smuggler2);
        lobby.addInspector(inspector1);
        lobby.addInspector(inspector2);
        lobby.addInspector(inspector3);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(5L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);
        lobby.toggleReady(6L);
        lobby.startGame(3, 1L);

        // when
        ContrabandGame actual = lobby.currentGame();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void 게임이_완료되면_로비_상태가_게임_종료_상태가_된다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        PlayerProfile smuggler1 = PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER);
        PlayerProfile smuggler2 = PlayerProfile.create(5L, "밀수꾼2", TeamRole.SMUGGLER);
        PlayerProfile inspector1 = PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR);
        PlayerProfile inspector2 = PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR);
        PlayerProfile inspector3 = PlayerProfile.create(6L, "검사관3", TeamRole.INSPECTOR);

        lobby.addSmuggler(smuggler1);
        lobby.addSmuggler(smuggler2);
        lobby.addInspector(inspector1);
        lobby.addInspector(inspector2);
        lobby.addInspector(inspector3);
        lobby.toggleReady(1L);
        lobby.toggleReady(3L);
        lobby.toggleReady(5L);
        lobby.toggleReady(2L);
        lobby.toggleReady(4L);
        lobby.toggleReady(6L);

        ContrabandGame game = lobby.startGame(1, 1L);

        game.startNewRound(1L, 2L);
        game.decideSmuggleAmountForCurrentRound(Money.from(500));
        game.decidePassForCurrentRound();
        game.finishCurrentRound();

        // when
        lobby.markFinishedIfDone();

        // then
        assertThat(lobby.getPhase()).isEqualTo(LobbyPhase.FINISHED);
    }

    @Test
    void 밀수꾼_팀에_플레이어를_추가할_수_있는지_확인한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        // when
        boolean actual = lobby.canAddSmuggler(2L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 검사관_팀에_플레이어를_추가할_수_있는지_확인한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();

        lobby.addInspector(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR));
        lobby.addInspector(PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR));
        lobby.addInspector(PlayerProfile.create(6L, "검사관3", TeamRole.INSPECTOR));

        // when
        boolean actual = lobby.canAddInspector(7L);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 로비에_플레이어를_추가할_수_있는지_확인한다() {
        // given
        Lobby lobby = createLobbyWithSmugglerHost();
        lobby.addSmuggler(PlayerProfile.create(3L, "밀수꾼1", TeamRole.SMUGGLER));
        lobby.addSmuggler(PlayerProfile.create(5L, "밀수꾼2", TeamRole.SMUGGLER));
        lobby.addInspector(PlayerProfile.create(2L, "검사관1", TeamRole.INSPECTOR));
        lobby.addInspector(PlayerProfile.create(4L, "검사관2", TeamRole.INSPECTOR));
        lobby.addInspector(PlayerProfile.create(6L, "검사관3", TeamRole.INSPECTOR));

        // when & then
        assertThat(lobby.canAddToLobby()).isFalse();
    }

    private Lobby createLobbyWithSmugglerHost() {
        PlayerProfile hostProfile = PlayerProfile.create(1L, "방장", TeamRole.SMUGGLER);
        return Lobby.create(1L, "게임방", hostProfile, 6);
    }
}
