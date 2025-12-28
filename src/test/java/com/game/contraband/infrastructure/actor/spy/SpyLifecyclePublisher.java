package com.game.contraband.infrastructure.actor.spy;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher;
import java.util.ArrayList;
import java.util.List;

public class SpyLifecyclePublisher implements GameLifecycleEventPublisher {

    private final List<GameLifecycleEvent> events = new ArrayList<>();

    @Override
    public void publish(GameLifecycleEvent event) {
        events.add(event);
    }

    public List<GameLifecycleEvent> events() {
        return events;
    }
}
