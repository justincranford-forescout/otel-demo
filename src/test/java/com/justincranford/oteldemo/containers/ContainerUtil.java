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

import static com.justincranford.oteldemo.actuator.Constants.ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_OTEL_CONTRIB;
import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_PORTS_GRAFANA_LGTM;
import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_PORTS_OTEL_CONTRIB;

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

    public static void printUrlsWithMappedPorts(final String baseUrl) {
        final List<String> urlsWithMappedPorts = new java.util.ArrayList<>();
        for (final String path : ACTUATOR_ENDPOINTS) {
            urlsWithMappedPorts.add(String.format("spring-boot-actuator: %s%s", baseUrl, path));
        }
        for (final Integer port : CONTAINER_PORTS_OTEL_CONTRIB) {
            urlsWithMappedPorts.add(String.format("otel-contrib %d:    http://localhost:%d/", port, CONTAINER_OTEL_CONTRIB.get().getMappedPort(port)));
        }
        for (final Integer port : CONTAINER_PORTS_GRAFANA_LGTM) {
            urlsWithMappedPorts.add(String.format("grafana-lgtm %d:    http://localhost:%d/", port, ContainerManager.CONTAINER_GRAFANA_LGTM.get().getMappedPort(port)));
        }
        log.info("URLs:\n{}", String.join("\n", urlsWithMappedPorts));
    }
}
