package com.game.contraband.infrastructure.actor.game.engine.match.selection;

import static org.assertj.core.api.Assertions.assertThat;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SelectionParticipantsTest {

    @Test
    void 두_플레이어만이_게임에_참여하고_있는지_확인한다() {
        // given
        SelectionParticipants participants = twoPlayerGame();

        // when
        boolean actual = participants.isTwoPlayerGame();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 두_명을_초과한_플레이어가_게임에_참여하고_있는지_확인한다() {
        // given
        SelectionParticipants participants = multiPlayerGame();

        // when
        boolean actual = participants.isNotTwoPlayerGame();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 밀수꾼_후보_선정_과정이_필요한지_확인한다() {
        // given
        SelectionParticipants participants = multiPlayerGame();

        // when
        boolean actual = participants.requiresSmugglerConsensus();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 검사관_후보_선정_과정이_필요한지_확인한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR), player(4L, TeamRole.INSPECTOR)),
                1,
                2
        );

        // when
        boolean actual = participants.requiresInspectorConsensus();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 밀수꾼_후보_선정_시_필요한_찬성_수를_계산한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER), player(3L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR)),
                2,
                1
        );

        // when
        int actual = participants.requiredSmugglerApprovals();

        // then
        assertThat(actual).isEqualTo(1);
    }

    @Test
    void 검사관_후보_선정_시_필요한_찬성_수를_계산한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(
                        player(2L, TeamRole.INSPECTOR),
                        player(4L, TeamRole.INSPECTOR),
                        player(5L, TeamRole.INSPECTOR)
                ),
                1,
                3
        );

        // when
        int actual = participants.requiredInspectorApprovals();

        // then
        assertThat(actual).isEqualTo(2);
    }

    @Test
    void 첫번째_밀수꾼_ID를_조회한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER), player(3L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR)),
                2,
                1
        );

        // when
        Optional<Long> actual = participants.firstSmugglerId();

        // then
        assertThat(actual).contains(1L);
    }

    @Test
    void 첫번째_검사관_ID를_조회한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR), player(4L, TeamRole.INSPECTOR)),
                1,
                2
        );

        // when
        Optional<Long> actual = participants.firstInspectorId();

        // then
        assertThat(actual).contains(2L);
    }

    @Test
    void 밀수꾼_후보가_있는지_확인한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR)),
                1,
                1
        );

        // when
        boolean actual = participants.hasSmugglerCandidate();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 검사관_후보가_있는지_확인한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR)),
                1,
                1
        );

        // when
        boolean actual = participants.hasInspectorCandidate();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 밀수꾼과_검사관_후보가_모두_있는지_확인한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR)),
                1,
                1
        );

        // when
        boolean actual = participants.hasBothCandidates();

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 랜덤으로_밀수꾼_ID를_선택한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(player(1L,
                        TeamRole.SMUGGLER),
                        player(3L, TeamRole.SMUGGLER),
                        player(5L, TeamRole.SMUGGLER)
                ),
                List.of(),
                3,
                0
        );
        Random random = new Random(1L);

        // when
        Optional<Long> actual = participants.pickRandomPlayerId(TeamRole.SMUGGLER, random);

        // then
        assertThat(actual).contains(1L);
    }

    @Test
    void 랜덤으로_검사관_ID를_선택한다() {
        // given
        SelectionParticipants participants = new SelectionParticipants(
                List.of(),
                List.of(
                        player(2L, TeamRole.INSPECTOR),
                        player(4L, TeamRole.INSPECTOR),
                        player(6L, TeamRole.INSPECTOR)
                ),
                0,
                3
        );
        Random random = new Random(2L);

        // when
        Optional<Long> actual = participants.pickRandomPlayerId(TeamRole.INSPECTOR, random);

        // then
        assertThat(actual).contains(4L);
    }

    private SelectionParticipants twoPlayerGame() {
        return new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR)),
                1,
                1
        );
    }

    private SelectionParticipants multiPlayerGame() {
        return new SelectionParticipants(
                List.of(player(1L, TeamRole.SMUGGLER), player(3L, TeamRole.SMUGGLER)),
                List.of(player(2L, TeamRole.INSPECTOR), player(4L, TeamRole.INSPECTOR)),
                2,
                2
        );
    }

    private PlayerProfile player(Long id, TeamRole role) {
        return PlayerProfile.create(id, role.isSmuggler() ? "밀수꾼" : "검사관", role);
    }
}
