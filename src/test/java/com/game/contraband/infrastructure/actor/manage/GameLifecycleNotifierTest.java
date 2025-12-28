package com.game.contraband.infrastructure.actor.manage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.GameLifecycleEvent;
import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.LifecycleType;
import com.game.contraband.infrastructure.actor.spy.SpyPublisher;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GameLifecycleNotifierTest {

    @Test
    void 게임방_생성_이벤트를_발행한다() {
        // given
        SpyPublisher publisher = new SpyPublisher();
        GameLifecycleNotifier notifier = new GameLifecycleNotifier(publisher, "entity");

        // when
        notifier.roomCreated(1L);

        // then
        GameLifecycleEvent actual = publisher.events().get(0);

        assertAll(
                () -> assertThat(actual.type()).isEqualTo(LifecycleType.ROOM_CREATED),
                () -> assertThat(actual.entityId()).isEqualTo("entity"),
                () -> assertThat(actual.roomId()).isEqualTo(1L)
        );
    }

    @Test
    void 게임_시작_이벤트를_발행한다() {
        // given
        SpyPublisher publisher = new SpyPublisher();
        GameLifecycleNotifier notifier = new GameLifecycleNotifier(publisher, "entity");

        // when
        notifier.gameStarted(3L);

        // then
        GameLifecycleEvent actual = publisher.events().get(0);

        assertAll(
                () -> assertThat(actual.type()).isEqualTo(LifecycleType.GAME_STARTED),
                () -> assertThat(actual.entityId()).isEqualTo("entity"),
                () -> assertThat(actual.roomId()).isEqualTo(3L)
        );
    }

    @Test
    void 게임_종료_이벤트를_발행한다() {
        // given
        SpyPublisher publisher = new SpyPublisher();
        GameLifecycleNotifier notifier = new GameLifecycleNotifier(publisher, "entity");

        // when
        notifier.gameEnded(7L);

        // then
        GameLifecycleEvent actual = publisher.events().get(0);

        assertAll(
                () -> assertThat(actual.type()).isEqualTo(LifecycleType.GAME_ENDED),
                () -> assertThat(actual.entityId()).isEqualTo("entity"),
                () -> assertThat(actual.roomId()).isEqualTo(7L)
        );
    }

    @Test
    void 게임방_삭제_이벤트를_발행한다() {
        // given
        SpyPublisher publisher = new SpyPublisher();
        GameLifecycleNotifier notifier = new GameLifecycleNotifier(publisher, "entity");

        // when
        notifier.roomRemoved(9L);

        // then
        GameLifecycleEvent actual = publisher.events().get(0);

        assertAll(
                () -> assertThat(actual.type()).isEqualTo(LifecycleType.ROOM_REMOVED),
                () -> assertThat(actual.entityId()).isEqualTo("entity"),
                () -> assertThat(actual.roomId()).isEqualTo(9L)
        );
    }
}
