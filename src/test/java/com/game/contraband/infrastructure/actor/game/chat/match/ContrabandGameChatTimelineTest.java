package com.game.contraband.infrastructure.actor.game.chat.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.chat.ChatMessage;
import com.game.contraband.infrastructure.actor.spy.SpyChatMessages;
import com.game.contraband.infrastructure.actor.stub.StubSnowflakeSequenceGenerator;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ContrabandGameChatTimelineTest {

    @Test
    void 메시지를_추가한다() {
        // given
        StubSnowflakeSequenceGenerator sequenceGenerator = new StubSnowflakeSequenceGenerator(5L);
        SpyChatMessages chatMessages = new SpyChatMessages();
        ContrabandGameChatTimeline timeline = new ContrabandGameChatTimeline(1L, sequenceGenerator, chatMessages);

        // when
        ChatMessage actual = timeline.append(10L, "작성자", "메시지");

        // then
        assertAll(
                () -> assertThat(actual.id()).isEqualTo(5L),
                () -> assertThat(actual.roomId()).isEqualTo(1L),
                () -> assertThat(actual.writerId()).isEqualTo(10L),
                () -> assertThat(actual.writerName()).isEqualTo("작성자"),
                () -> assertThat(actual.message()).isEqualTo("메시지"),
                () -> assertThat(actual.masked()).isFalse(),
                () -> assertThat(chatMessages.addedMessages()).containsExactly(actual)
        );
    }

    @Test
    void 작성자_메시지를_마스킹한다() {
        // given
        StubSnowflakeSequenceGenerator sequenceGenerator = new StubSnowflakeSequenceGenerator(1L);
        SpyChatMessages chatMessages = new SpyChatMessages();
        ContrabandGameChatTimeline timeline = new ContrabandGameChatTimeline(2L, sequenceGenerator, chatMessages);
        ChatMessage first = new ChatMessage(1L, 2L, 10L, "작성자", "내용", LocalDateTime.now());
        ChatMessage second = new ChatMessage(2L, 2L, 30L, "다른", "유지", LocalDateTime.now());

        chatMessages.initMessages(List.of(first, second), List.of(first.markMasked()));

        // when
        List<ChatMessage> actual = timeline.maskMessagesByWriter(10L);

        // then
        assertAll(
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(0).writerId()).isEqualTo(10L),
                () -> assertThat(actual.get(0).masked()).isTrue()
        );
    }
}
