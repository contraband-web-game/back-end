package com.game.contraband.infrastructure.actor.game.chat.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandGameChatTimelinesTest {

    @Test
    void 팀_채팅에_메시지를_추가한다() {
        // given
        ContrabandGameChatTimelines timelines = new ContrabandGameChatTimelines(1L);

        // when
        ChatMessage actual = timelines.appendTeamMessage(TeamRole.SMUGGLER, 10L, "밀수꾼", "안녕");

        // then
        assertAll(
                () -> assertThat(actual.roomId()).isEqualTo(1L),
                () -> assertThat(actual.writerId()).isEqualTo(10L),
                () -> assertThat(actual.writerName()).isEqualTo("밀수꾼"),
                () -> assertThat(actual.message()).isEqualTo("안녕"),
                () -> assertThat(actual.masked()).isFalse()
        );
    }

    @Test
    void 라운드_채팅에_메시지를_추가한다() {
        // given
        ContrabandGameChatTimelines timelines = new ContrabandGameChatTimelines(1L);

        // when
        ChatMessage actual = timelines.appendRoundMessage(20L, "검사관", "라운드 메시지");

        // then
        assertAll(
                () -> assertThat(actual.roomId()).isEqualTo(1L),
                () -> assertThat(actual.writerId()).isEqualTo(20L),
                () -> assertThat(actual.writerName()).isEqualTo("검사관"),
                () -> assertThat(actual.message()).isEqualTo("라운드 메시지"),
                () -> assertThat(actual.masked()).isFalse()
        );
    }

    @Test
    void 팀_메시지를_마스킹한다() {
        // given
        ContrabandGameChatTimelines timelines = new ContrabandGameChatTimelines(1L);
        timelines.appendTeamMessage(TeamRole.SMUGGLER, 10L, "밀수꾼", "내용");
        timelines.appendTeamMessage(TeamRole.INSPECTOR, 30L, "검사관", "다른 팀");

        // when
        List<ChatMessage> actual = timelines.maskTeamMessages(TeamRole.SMUGGLER, 10L);

        // then
        assertAll(
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(0).writerId()).isEqualTo(10L),
                () -> assertThat(actual.get(0).masked()).isTrue()
        );
    }

    @Test
    void 라운드_메시지를_마스킹한다() {
        // given
        ContrabandGameChatTimelines timelines = new ContrabandGameChatTimelines(1L);

        timelines.appendRoundMessage(40L, "참가자", "라운드 내용");
        timelines.appendRoundMessage(50L, "다른", "유지");

        // when
        List<ChatMessage> actual = timelines.maskRoundMessages(40L);

        // then
        assertAll(
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(0).writerId()).isEqualTo(40L),
                () -> assertThat(actual.get(0).masked()).isTrue()
        );
    }
}
