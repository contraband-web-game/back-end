package com.game.contraband.infrastructure.event;

import com.game.contraband.infrastructure.actor.game.engine.GameLifecycleEventPublisher.GameLifecycleEvent;
import com.game.contraband.infrastructure.event.ApplicationEventGameLifecyclePublisher.GameLifecycleApplicationEvent;
import com.game.contraband.infrastructure.monitor.payload.MonitorLifecyclePayload;
import com.game.contraband.infrastructure.monitor.payload.MonitorMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitorLifecycleEventListener implements ApplicationListener<GameLifecycleApplicationEvent> {

    private final MonitorEventBroadcaster broadcaster;

    @Override
    public void onApplicationEvent(GameLifecycleApplicationEvent event) {
        GameLifecycleEvent payload = event.payload();
        MonitorLifecyclePayload monitorPayload = new MonitorLifecyclePayload(
                payload.entityId(),
                payload.roomId(),
                payload.type().name()
        );

        broadcaster.publish(new MonitorMessage(MonitorEventType.LIFECYCLE_EVENT, monitorPayload));
    }
}
