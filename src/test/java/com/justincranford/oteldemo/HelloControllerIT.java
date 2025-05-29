package com.justincranford.oteldemo;

import com.justincranford.oteldemo.containers.ContainerManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.LongStream;

import static com.justincranford.oteldemo.containers.ContainerManager.*;
import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class HelloControllerIT extends AbstractIT {
    private static final boolean WAIT_BEFORE_EXIT = true; // Default false, change to true to wait before exiting after all tests

    private static final long ACTUATOR_HEALTH_TOTAL_WAIT = 2_000L;
    private static final long ACTUATOR_HEALTH_INCREMENTAL_WAIT = 100L;
    private static final long ACTUATOR_HEALTH_MAX_REPEATS = SECURE_RANDOM.nextLong(3, 6); // 3-5 inclusive

    private static final long ACTUATOR_PROMETHEUS_TOTAL_WAIT = 2_000L;
    private static final long ACTUATOR_PROMETHEUS_INCREMENTAL_WAIT = 100L;
    private static final long ACTUATOR_PROMETHEUS_MAX_REPEATS = SECURE_RANDOM.nextLong(1, 2); // 1-2 inclusive

    private static final long OTEL_PROMETHEUS_TOTAL_WAIT = 500L;
    private static final long OTEL_PROMETHEUS_INCREMENTAL_WAIT = 500L;
    private static final long OTEL_PROMETHEUS_MAX_REPEATS = SECURE_RANDOM.nextLong(1, 2); // 1-1 inclusive


    @AfterAll
    static void afterAll() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            log.warn("Interrupted while waiting after all tests in HelloControllerIT");
        }
        final List<String> urlsWithMappedPorts = new java.util.ArrayList<>();
        for (final String path : List.of(
                "/actuator",
                "/actuator/health",
                "/actuator/metrics",
                "/actuator/prometheus",
                "/actuator/info",
                "/actuator/env",
                "/actuator/beans",
                "/actuator/mappings",
                "/actuator/loggers",
                "/actuator/threaddump",
                "/actuator/caches"
        )) {
            urlsWithMappedPorts.add(String.format("spring-boot-actuator: %s%s", BASE_URL, path));
        }
        for (final Integer port : CONTAINER_PORTS_OTEL_CONTRIB) {
            urlsWithMappedPorts.add(String.format("otel-contrib %d:    http://localhost:%d/", port, CONTAINER_OTEL_CONTRIB.get().getMappedPort(port)));
        }
        for (final Integer port : CONTAINER_PORTS_GRAFANA_LGTM) {
            urlsWithMappedPorts.add(String.format("grafana-lgtm %d:    http://localhost:%d/", port, ContainerManager.CONTAINER_GRAFANA_LGTM.get().getMappedPort(port)));
        }
        log.info("Summary after all tests:\n{}", String.join("\n", urlsWithMappedPorts));
        if (WAIT_BEFORE_EXIT) {
            log.info("Waiting for interrupt before exit...");
            while (WAIT_BEFORE_EXIT) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Interrupted.");
                    break;
                }
            }
        }
    }

    @Test
    void testHelloApi_verifyBuiltInPrometheus_verifyOtelPrometheus() {
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(super.baseUrl() + "/hello", String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        log.info("Hello API response:\n{}", responseBody);

        final String actuatorHealth     = super.baseUrl() + "/actuator/health";
        final String actuatorPrometheus = super.baseUrl() + "/actuator/prometheus";
        final String otelPrometheus     = "http://localhost:" + CONTAINER_OTEL_CONTRIB.get().getMappedPort(8888) + "/metrics";

        for (final long i : LongStream.rangeClosed(1, ACTUATOR_HEALTH_MAX_REPEATS).toArray()) {
            final String response = pollHttpGet("HEALTH"+i,       actuatorHealth,     "\"status\":\"UP\"", ACTUATOR_HEALTH_TOTAL_WAIT, ACTUATOR_HEALTH_INCREMENTAL_WAIT);
            assertTrue(response.contains("\"status\":\"UP\""));
        }
        for (final long i : LongStream.rangeClosed(1, ACTUATOR_PROMETHEUS_MAX_REPEATS).toArray()) {
            final String response = pollHttpGet("PROMETHEUS"+i,   actuatorPrometheus, "/hello", ACTUATOR_PROMETHEUS_TOTAL_WAIT, ACTUATOR_PROMETHEUS_INCREMENTAL_WAIT);
            assertTrue(response.contains("/hello"));
        }
        for (final long i : LongStream.rangeClosed(1, OTEL_PROMETHEUS_MAX_REPEATS).toArray()) {
            final String response = pollHttpGet("OTEL-CONTRIB"+i, otelPrometheus,     "/hello", OTEL_PROMETHEUS_TOTAL_WAIT, OTEL_PROMETHEUS_INCREMENTAL_WAIT);
            assertTrue(response.contains("/hello"));
        }
    }

    protected String pollHttpGet(final String logPrefix, final String url, final String stopString, final long totalWaitMillis, final long incrementalWaitMillis) {
        String latestResponse = "";
        final long endTimeMillisOtel = System.currentTimeMillis() + totalWaitMillis;
        do {
            try {
                latestResponse = doHttpGet(url);
                log.info("Response:\n{}", prefixAllLines(logPrefix, latestResponse));
            } catch(ResourceAccessException e) {
                log.warn("Error: {}", e.getMessage());
                wait_(incrementalWaitMillis);
            }
        } while ((!latestResponse.contains(stopString)) && (System.currentTimeMillis() < endTimeMillisOtel));
        return latestResponse;
    }

    private static void wait_(final long incrementalWaitMillis)  {
        try {
            Thread.sleep(incrementalWaitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            log.warn("Interrupted while waiting for {} milliseconds", incrementalWaitMillis);
        }
    }
}
