package com.game.contraband.infrastructure.actor.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public class ActorTestUtils {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);
    static final Duration POLL_SLICE = Duration.ofMillis(50);

    static final int MAX_BUFFER_DUMP = 50;
    static final int MAX_TO_STRING_LEN = 200;

    static final int MAX_WAIT_RECENT_DUMP = 30;

    static final Map<Object, Deque<Object>> BUFFER = Collections.synchronizedMap(new WeakHashMap<>());

    public static <T> MonitoredActor<T> spawnMonitored(
            ActorTestKit actorTestKit,
            Class<T> interceptMessageClass,
            Behavior<T> behavior
    ) {
        TestProbe<T> monitor = actorTestKit.createTestProbe(interceptMessageClass);
        Behavior<T> monitored = Behaviors.monitor(interceptMessageClass, monitor.getRef(), behavior);
        ActorRef<T> ref = actorTestKit.spawn(monitored);
        return new MonitoredActor<>(ref, monitor);
    }

    public static <T> MonitoredActor<T> spawnMonitored(
            ActorTestKit actorTestKit,
            String name,
            Class<T> interceptMessageClass,
            Behavior<T> behavior
    ) {
        TestProbe<T> monitor = actorTestKit.createTestProbe(interceptMessageClass);
        Behavior<T> monitored = Behaviors.monitor(interceptMessageClass, monitor.getRef(), behavior);
        ActorRef<T> ref = actorTestKit.spawn(monitored, name);
        return new MonitoredActor<>(ref, monitor);
    }

    @SafeVarargs
    public static <T> void waitUntilMessages(
            TestProbe<T> probe,
            Duration timeout,
            Class<? extends T>... requiredTypes
    ) {
        if (requiredTypes == null || requiredTypes.length == 0) {
            throw new AssertionError("기대 메시지 타입이 비어 있습니다.");
        }

        List<Class<? extends T>> missing = new ArrayList<>(Arrays.asList(requiredTypes));
        Instant deadline = Instant.now().plus(timeout);

        List<T> recent = new ArrayList<>(Math.min(MAX_WAIT_RECENT_DUMP, 16));

        while (!missing.isEmpty() && Instant.now().isBefore(deadline)) {
            T bufferedMatch = pollFirstMatchingFromBuffer(probe, missing);
            if (bufferedMatch != null) {
                int hit = indexOfFirstMatch(missing, bufferedMatch);
                if (hit >= 0) {
                    missing.remove(hit);
                }
                continue;
            }

            Duration remaining = Duration.between(Instant.now(), deadline);
            Duration slice;
            if (remaining.compareTo(POLL_SLICE) > 0) {
                slice = POLL_SLICE;
            } else {
                slice = remaining;
            }

            if (slice.isZero() || slice.isNegative()) {
                break;
            }

            T message;
            try {
                message = probe.receiveMessage(slice);
            } catch (AssertionError timeoutEx) {
                continue;
            }

            int hitIndex = indexOfFirstMatch(missing, message);
            if (hitIndex >= 0) {
                missing.remove(hitIndex);
            } else {
                if (recent.size() >= MAX_WAIT_RECENT_DUMP) {
                    recent.remove(0);
                }
                recent.add(message);
            }
        }

        if (!missing.isEmpty()) {
            throw new AssertionError(buildWaitFailureMessage(
                    "메시지를 받지 못했습니다.",
                    probe,
                    requiredTypes,
                    missing,
                    recent
            ));
        }
    }

    @SafeVarargs
    public static <T> void waitUntilMessages(TestProbe<T> probe, Class<? extends T>... requiredTypes) {
        waitUntilMessages(probe, DEFAULT_TIMEOUT, requiredTypes);
    }

    @SafeVarargs
    public static <T> void expectMessages(
            TestProbe<T> probe,
            Class<? extends T>... requiredTypes
    ) {
        expectMessages(probe, DEFAULT_TIMEOUT, requiredTypes);
    }

    @SafeVarargs
    public static <T> void expectMessages(
            TestProbe<T> probe,
            Duration timeout,
            Class<? extends T>... requiredTypes
    ) {
        if (requiredTypes == null || requiredTypes.length == 0) {
            throw new AssertionError("기대 메시지 타입이 비어 있습니다.");
        }

        List<Class<? extends T>> missing = new ArrayList<>(Arrays.asList(requiredTypes));
        Instant deadline = Instant.now().plus(timeout);

        while (!missing.isEmpty() && Instant.now().isBefore(deadline)) {
            T bufferedMatch = pollFirstMatchingFromBuffer(probe, missing);
            if (bufferedMatch != null) {
                missing.remove(indexOfFirstMatch(missing, bufferedMatch));
                continue;
            }

            Duration remaining = Duration.between(Instant.now(), deadline);
            Duration slice;
            if (remaining.compareTo(POLL_SLICE) > 0) {
                slice = POLL_SLICE;
            } else {
                slice = remaining;
            }

            if (slice.isZero() || slice.isNegative()) {
                break;
            }

            T message;
            try {
                message = probe.receiveMessage(slice);
            } catch (AssertionError timeoutEx) {
                continue;
            }

            int hitIndex = indexOfFirstMatch(missing, message);
            if (hitIndex >= 0) {
                missing.remove(hitIndex);
            } else {
                bufferOf(probe).addLast(message);
            }
        }

        if (!missing.isEmpty()) {
            throw new AssertionError(buildFailureMessage(
                    "메시지를 받지 못했습니다.",
                    probe,
                    requiredTypes,
                    missing
            ));
        }
    }

    public static <T> void expectNoMessages(TestProbe<T> probe) {
        expectNoMessages(probe, DEFAULT_TIMEOUT);
    }

    public static <T> void expectNoMessages(TestProbe<T> probe, Duration duration) {
        if (!bufferOf(probe).isEmpty()) {
            throw new AssertionError(buildNoMessageFailure(
                    "이미 버퍼에 메시지가 존재합니다.",
                    probe,
                    null
            ));
        }

        Instant deadline = Instant.now().plus(duration);

        while (Instant.now().isBefore(deadline)) {
            Duration remaining = Duration.between(Instant.now(), deadline);
            Duration slice;
            if (remaining.compareTo(POLL_SLICE) > 0) {
                slice = POLL_SLICE;
            } else {
                slice = remaining;
            }

            if (slice.isZero() || slice.isNegative()) {
                break;
            }

            try {
                T msg = probe.receiveMessage(slice);
                bufferOf(probe).addLast(msg);
                throw new AssertionError(buildNoMessageFailure(
                        "예상치 못한 메시지를 수신했습니다.",
                        probe,
                        msg
                ));
            } catch (AssertionError ignored) {
            }
        }
    }

    static <T> T pollFirstMatchingFromBuffer(TestProbe<T> probe, List<Class<? extends T>> missing) {
        Deque<T> buf = bufferOf(probe);
        if (buf.isEmpty()) {
            return null;
        }

        Iterator<T> it = buf.iterator();
        while (it.hasNext()) {
            T msg = it.next();
            if (indexOfFirstMatch(missing, msg) >= 0) {
                it.remove();
                return msg;
            }
        }
        return null;
    }

    static <T> int indexOfFirstMatch(List<Class<? extends T>> missing, T message) {
        for (int i = 0; i < missing.size(); i++) {
            if (missing.get(i).isInstance(message)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    static <T> Deque<T> bufferOf(Object key) {
        Deque<Object> deque = BUFFER.computeIfAbsent(key, k -> new ArrayDeque<>());
        return (Deque<T>) deque;
    }

    static <T> String buildNoMessageFailure(
            String title,
            TestProbe<T> probe,
            T received
    ) {
        List<String> dump = new ArrayList<>();
        int count = 0;

        for (Object msg : bufferOf(probe)) {
            if (count >= MAX_BUFFER_DUMP) {
                dump.add("...(+" + (bufferOf(probe).size() - MAX_BUFFER_DUMP) + " more)");
                break;
            }
            dump.add(String.format("[%02d] %s %s",
                    count,
                    msg.getClass().getName(),
                    summarizeMessage(msg)
            ));
            count++;
        }

        return title
                + "\n- 수신 메시지: "
                + (received == null ? "(none)" : received.getClass().getName())
                + "\n- 버퍼 크기: " + bufferOf(probe).size()
                + "\n- 버퍼 상세:"
                + "\n" + joinLines(dump);
    }

    static <T> String buildFailureMessage(
            String title,
            TestProbe<T> probe,
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

        Map<String, Integer> freq = new LinkedHashMap<>();
        for (Object msg : bufferOf(probe)) {
            freq.merge(msg.getClass().getSimpleName(), 1, Integer::sum);
        }

        return title
                + "\n- 기대: " + required
                + "\n- 누락: " + miss
                + "\n- 버퍼 타입 빈도: " + freq;
    }

    static <T> String buildWaitFailureMessage(
            String title,
            TestProbe<T> probe,
            Class<? extends T>[] requiredTypes,
            List<Class<? extends T>> missing,
            List<T> recent
    ) {
        List<String> required = new ArrayList<>();
        for (Class<? extends T> c : requiredTypes) {
            required.add(c.getSimpleName());
        }

        List<String> miss = new ArrayList<>();
        for (Class<? extends T> c : missing) {
            miss.add(c.getSimpleName());
        }

        List<String> recentDump = new ArrayList<>();
        int idx = 0;
        for (T msg : recent) {
            recentDump.add(String.format("[%02d] %s %s",
                    idx,
                    msg.getClass().getName(),
                    summarizeMessage(msg)
            ));
            idx++;
        }

        Map<String, Integer> bufFreq = new LinkedHashMap<>();
        for (Object msg : bufferOf(probe)) {
            bufFreq.merge(msg.getClass().getSimpleName(), 1, Integer::sum);
        }

        return title
                + "\n- 기대: " + required
                + "\n- 누락: " + miss
                + "\n- 최근 수신(버퍼링 안함):"
                + (recentDump.isEmpty() ? "\n  (empty)" : "\n" + joinLines(recentDump))
                + "\n- (참고) 기존 버퍼 타입 빈도: " + bufFreq;
    }

    static String joinLines(List<String> lines) {
        if (lines.isEmpty()) {
            return "(empty)";
        }

        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append("  ").append(l).append("\n");
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    static String summarizeMessage(Object msg) {
        try {
            return "(toString: " + safeTruncate(String.valueOf(msg), MAX_TO_STRING_LEN) + ")";
        } catch (Throwable t) {
            return "(toString threw " + t.getClass().getSimpleName() + ")";
        }
    }

    static String safeTruncate(String s, int maxLen) {
        if (s == null) {
            return "null";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }

    public static class MonitoredActor<T> {
        ActorRef<T> ref;
        TestProbe<T> monitor;

        public MonitoredActor(ActorRef<T> ref, TestProbe<T> monitor) {
            this.ref = ref;
            this.monitor = monitor;
        }

        public ActorRef<T> ref() {
            return ref;
        }

        public TestProbe<T> monitor() {
            return monitor;
        }
    }
}
