package com.justincranford.oteldemo.containers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
@Slf4j
public class ContainerUtil {
    public static void startContainersConcurrently(final List<Thread> startContainerThreads, final Duration totalDurationForContainersToStart) {
        final long millisStart = System.currentTimeMillis();
        for (final Thread startContainerThread : startContainerThreads) {
            if (startContainerThread.isAlive()) {
                final long millisRemaining = totalDurationForContainersToStart.toMillis() - (System.currentTimeMillis() - millisStart);
                if (millisRemaining <= 0) {
                    final String message = startContainerThread.getName() + " took too long to finish";
                    log.error(message);
                    startContainerThreads.stream().filter(Thread::isAlive).parallel().forEach(Thread::interrupt); // interrupt all remaining alive threads
                    throw new RuntimeException(message);
                }
                try {
                    startContainerThread.join(millisRemaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for thread " + startContainerThread.getName() + " to end", e);
                }
                if (startContainerThread.isAlive()) {
                    final String message = startContainerThread.getName() + " timed out waiting to finish";
                    log.error(message);
                    startContainerThreads.stream().filter(Thread::isAlive).parallel().forEach(Thread::interrupt); // interrupt all remaining alive threads
                    throw new RuntimeException(message);
                }
            }
        }
    }

    public static @NotNull Map<Integer, Integer> getMappedPorts(final GenericContainer<?> container, final Integer[] internalPorts) {
        return Arrays.stream(internalPorts)
                .map(port -> Map.entry(port, container.getMappedPort(port)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }
}
