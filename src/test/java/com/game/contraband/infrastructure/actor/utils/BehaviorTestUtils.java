package com.game.contraband.infrastructure.actor.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.pekko.actor.testkit.typed.Effect;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;

public class BehaviorTestUtils {

    private static final Map<Object, Deque<Object>> BUFFER = Collections.synchronizedMap(new WeakHashMap<>());

    public static <T> BehaviorTestHarness<T> createHarness(Behavior<T> behavior) {
        BehaviorTestKit<T> kit = BehaviorTestKit.create(behavior);
        return new BehaviorTestHarness<>(kit);
    }

    public static String expectSpawnedPrefix(BehaviorTestHarness<?> harness, String childNamePrefix) {
        List<Effect> effects = harness.effects();
        for (Effect effect : effects) {
            if (effect instanceof Effect.Spawned spawned) {
                String name = spawned.childName();
                if (name != null && name.startsWith(childNamePrefix)) {
                    return name;
                }
            }
        }

        List<String> spawnedNames = new ArrayList<>();
        for (Effect effect : effects) {
            if (effect instanceof Effect.Spawned spawned) {
                spawnedNames.add(spawned.childName());
            }
        }

        throw new AssertionError(
                "prefix로 Spawned child actor를 찾지 못했습니다."
                        + "\n- prefix: " + childNamePrefix
                        + "\n- Spawned 목록: " + spawnedNames
        );
    }

    @SafeVarargs
    public static <T> void expectMessages(TestInbox<T> inbox, Class<? extends T>... requiredTypes) {
        if (requiredTypes == null || requiredTypes.length == 0) {
            throw new AssertionError("기대 메시지 타입이 비어 있습니다.");
        }

        drainInboxToBuffer(inbox);

        List<Class<? extends T>> missing = new ArrayList<>();
        Collections.addAll(missing, requiredTypes);

        while (!missing.isEmpty()) {
            T hit = findAndRemoveFirstMatch(inbox, missing.get(0));
            if (hit != null) {
                missing.remove(0);
                continue;
            }

            for (int i = 0; i < missing.size(); i++) {
                Class<? extends T> type = missing.get(i);
                T msg = findAndRemoveFirstMatch(inbox, type);
                if (msg != null) {
                    missing.remove(i);
                    break;
                }
            }

            if (!missing.isEmpty()) {
                throw new AssertionError(buildFailureMessage(inbox, requiredTypes, missing));
            }
        }
    }

    static <T> void drainInboxToBuffer(TestInbox<T> inbox) {
        List<T> received = inbox.getAllReceived(); // inbox는 비우지만,
        if (received == null || received.isEmpty()) {
            return;
        }
        Deque<T> buf = bufferOf(inbox);
        for (T msg : received) {
            buf.addLast(msg);
        }
    }

    static <T> T findAndRemoveFirstMatch(TestInbox<T> inbox, Class<? extends T> type) {
        Deque<T> buf = bufferOf(inbox);
        for (T msg : buf) {
            if (type.isInstance(msg)) {
                buf.remove(msg);
                return msg;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> Deque<T> bufferOf(Object key) {
        return (Deque<T>) BUFFER.computeIfAbsent(key, k -> new ArrayDeque<>());
    }

    static <T> String buildFailureMessage(
            TestInbox<T> inbox,
            Class<? extends T>[] requiredTypes,
            List<Class<? extends T>> missing
    ) {
        List<String> required = new ArrayList<>();
        for (Class<? extends T> c : requiredTypes) {
            required.add(c.getSimpleName());
        }

        List<String> miss = new ArrayList<>();
        for (Class<? extends T> c : missing) {
            miss.add(c.getSimpleName());
        }

        List<String> buffered = new ArrayList<>();
        for (Object msg : bufferOf(inbox)) {
            buffered.add(msg.getClass().getSimpleName());
        }

        return "TestInbox에서 메시지를 받지 못했습니다."
                + "\n- 기대: " + required
                + "\n- 누락: " + miss
                + "\n- 버퍼에 남은 메시지: " + buffered;
    }

    public static class BehaviorTestHarness<T> {
        BehaviorTestKit<T> kit;
        List<Effect> cachedEffects;

        public BehaviorTestHarness(BehaviorTestKit<T> kit) {
            this.kit = kit;
            this.cachedEffects = new ArrayList<>();
        }

        public BehaviorTestKit<T> kit() {
            return kit;
        }

        public List<Effect> effects() {
            List<Effect> newly = kit.getAllEffects();

            if (newly != null && !newly.isEmpty()) {
                cachedEffects.addAll(newly);
            }

            return List.copyOf(cachedEffects);
        }
    }
}
