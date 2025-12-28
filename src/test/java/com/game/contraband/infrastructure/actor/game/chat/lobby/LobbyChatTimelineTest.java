package com.game.contraband.infrastructure.actor.game.chat.lobby;

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
class LobbyChatTimelineTest {

    @Test
    void 메시지를_추가한다() {
        // given
        StubSnowflakeSequenceGenerator sequenceGenerator = new StubSnowflakeSequenceGenerator(10L);
        SpyChatMessages chatMessages = new SpyChatMessages();
        LobbyChatTimeline timeline = new LobbyChatTimeline(1L, sequenceGenerator, chatMessages);

        // when
        ChatMessage actual = timeline.append(2L, "작성자", "내용");

        // then
        assertAll(
                () -> assertThat(actual.id()).isEqualTo(10L),
                () -> assertThat(actual.roomId()).isEqualTo(1L),
                () -> assertThat(actual.writerId()).isEqualTo(2L),
                () -> assertThat(actual.writerName()).isEqualTo("작성자"),
                () -> assertThat(actual.message()).isEqualTo("내용"),
                () -> assertThat(actual.masked()).isFalse(),
                () -> assertThat(chatMessages.addedMessages()).containsExactly(actual)
        );
    }

    @Test
    void 작성자_메시지를_마스킹한다() {
        // given
        StubSnowflakeSequenceGenerator sequenceGenerator = new StubSnowflakeSequenceGenerator(1L);
        SpyChatMessages chatMessages = new SpyChatMessages();
        LobbyChatTimeline timeline = new LobbyChatTimeline(2L, sequenceGenerator, chatMessages);
        ChatMessage original = new ChatMessage(1L, 2L, 3L, "작성자", "내용", LocalDateTime.now());
        ChatMessage masked = original.markMasked();
        chatMessages.initMessages(List.of(original), List.of(masked));

        // when
        List<ChatMessage> actual = timeline.maskMessagesByWriter(3L);

        // then
        assertAll(
                () -> assertThat(actual).hasSize(1),
                () -> assertThat(actual.get(0).writerId()).isEqualTo(3L),
                () -> assertThat(actual.get(0).masked()).isTrue()
        );
    }
}
