package com.game.contraband.infrastructure.event.dev;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class ApplicationEventGameLifecyclePublisher implements GameLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(GameLifecycleEvent event) {
        applicationEventPublisher.publishEvent(new GameLifecycleApplicationEvent(this, event));
    }

    public static class GameLifecycleApplicationEvent extends ApplicationEvent {

        private final GameLifecycleEvent payload;

        public GameLifecycleApplicationEvent(Object source, GameLifecycleEvent payload) {
            super(source);
            this.payload = payload;
        }

        public GameLifecycleEvent payload() {
            return payload;
        }
    }
}
