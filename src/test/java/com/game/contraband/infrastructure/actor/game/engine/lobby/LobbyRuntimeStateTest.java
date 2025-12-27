package com.game.contraband.infrastructure.actor.game.engine.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.engine.lobby.Lobby;
import com.game.contraband.domain.game.engine.lobby.LobbyPhase;
import com.game.contraband.domain.game.engine.match.ContrabandGame;
import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.game.engine.lobby.dto.LobbyParticipant;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LobbyRuntimeStateTest {

    @Test
    void 정원이_가득차면_플레이어를_추가할_수_없다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));
        lobby.addSmuggler(PlayerProfile.create(3L, "밀수꾼", TeamRole.SMUGGLER));
        lobby.addInspector(PlayerProfile.create(4L, "추가검사관", TeamRole.INSPECTOR));

        LobbyRuntimeState state = createState(lobby);

        // when
        boolean actual = state.cannotAddToLobby();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 플레이어가_호스트인지_확인한다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when
        boolean actual = state.isHost(1L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 플레이어가_호스트가_아닌지_확인한다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when
        boolean actual = state.isNotHost(2L);

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 로비_참가자_목록을_반환한다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));
        lobby.toggleReady(2L);

        LobbyRuntimeState state = createState(lobby);

        // when
        List<LobbyParticipant> actual = state.lobbyParticipants();

        // then
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(
                        List.of(
                                new LobbyParticipant(1L, "호스트", TeamRole.SMUGGLER, false),
                                new LobbyParticipant(2L, "검사관", TeamRole.INSPECTOR, true)
                        )
                );
    }

    @Test
    void 플레이어_프로필을_조회한다() {
        // given
        Lobby lobby = defaultLobby();
        PlayerProfile inspector = PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR);

        lobby.addInspector(inspector);

        LobbyRuntimeState state = createState(lobby);

        // when
        PlayerProfile actual = state.findPlayerProfile(2L);

        // then
        assertThat(actual).isEqualTo(inspector);
    }

    @Test
    void 없는_플레이어에_대한_플레이어_프로필은_조회할_수_없다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when & then
        assertThatThrownBy(() -> state.findPlayerProfile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로비에 존재하지 않는 플레이어입니다.");
    }

    @Test
    void 플레이어를_로비에서_제거한다() {
        // given
        Lobby lobby = defaultLobby();
        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));
        LobbyRuntimeState state = createState(lobby);

        // when
        state.removePlayer(2L);

        // then
        assertThat(state.lobbyParticipants())
                .allMatch(participant -> !participant.playerId().equals(2L));
    }

    @Test
    void 호스트는_제거할_수_없다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when & then
        assertThatThrownBy(() -> state.removePlayer(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("방장은 나갈 수 없습니다");
    }

    @Test
    void 최대_플레이어_수를_조회한다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when
        int actual = state.lobbyMaxPlayerCount();

        // then
        assertThat(actual).isEqualTo(4);
    }

    @Test
    void 최대_플레이어_수를_변경한다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when
        state.changeMaxPlayerCount(6, 1L);

        // then
        assertThat(state.lobbyMaxPlayerCount()).isEqualTo(6);
    }

    @Test
    void 준비_상태로_변경한다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        LobbyRuntimeState state = createState(lobby);

        // when
        state.toggleReady(2L);

        // then
        assertThat(state.readyStateOf(2L)).isTrue();
    }

    @Test
    void 준비_상태인지_확인한다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        LobbyRuntimeState state = createState(lobby);

        // when
        boolean actual = state.readyStateOf(2L);

        // then
        assertThat(actual).isFalse();
    }

    @Test
    void 팀을_변경한다() {
        // given
        Lobby lobby = defaultLobby();
        LobbyRuntimeState state = createState(lobby);

        // when
        state.toggleTeam(1L);

        // then
        assertThat(state.findPlayerProfile(1L).getTeamRole()).isEqualTo(TeamRole.INSPECTOR);
    }

    @Test
    void 준비_상태에서는_팀을_변경할_수_없다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.toggleReady(1L);

        LobbyRuntimeState state = createState(lobby);

        // when & then
        assertThatThrownBy(() -> state.toggleTeam(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ready 상태에서는 팀을 변경할 수 없습니다");
    }

    @Test
    void 플레이어를_강퇴한다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));

        LobbyRuntimeState state = createState(lobby);

        // when
        PlayerProfile kicked = state.kick(1L, 2L);

        // then
        assertAll(
                () -> assertThat(state.lobbyParticipants())
                        .allMatch(participant -> !participant.playerId().equals(2L)),
                () -> assertThat(kicked.getName()).isEqualTo("검사관"),
                () -> assertThat(kicked.getTeamRole()).isEqualTo(TeamRole.INSPECTOR)
        );
    }

    @Test
    void 로비를_삭제하면_로비_종료_상태로_변경한다() {
        // given
        LobbyRuntimeState state = createState(defaultLobby());

        // when
        state.deleteLobby(1L);

        // then
        assertThat(state.getLobby().getPhase()).isEqualTo(LobbyPhase.FINISHED);
    }

    @Test
    void 게임을_시작하면_단계를_진행중으로_변경한다() {
        // given
        Lobby lobby = defaultLobby();

        lobby.addInspector(PlayerProfile.create(2L, "검사관", TeamRole.INSPECTOR));
        lobby.toggleReady(1L);
        lobby.toggleReady(2L);

        LobbyRuntimeState state = createState(lobby);

        // when
        ContrabandGame actual = state.startGame(3, 1L);

        // then
        assertAll(
                () -> assertThat(state.getLobby().getPhase()).isEqualTo(LobbyPhase.IN_PROGRESS),
                () -> assertThat(actual.getTotalRounds()).isEqualTo(3)
        );
    }

    private LobbyRuntimeState createState(Lobby lobby) {
        return new LobbyRuntimeState(100L, 1L, "entity-1", lobby);
    }

    private Lobby defaultLobby() {
        PlayerProfile host = PlayerProfile.create(1L, "호스트", TeamRole.SMUGGLER);

        return Lobby.create(100L, "lobby", host, 4);
    }
}
