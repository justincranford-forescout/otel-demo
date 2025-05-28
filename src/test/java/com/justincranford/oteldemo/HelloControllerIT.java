package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.stream.LongStream;

import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_OTEL_CONTRIB;
import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class HelloControllerIT extends AbstractIT {
    private static final long ACTUATOR_HEALTH_TOTAL_WAIT = 2_000L;
    private static final long ACTUATOR_HEALTH_INCREMENTAL_WAIT = 100L;
    private static final long ACTUATOR_HEALTH_MAX_REPEATS = SECURE_RANDOM.nextLong(3, 6); // 3-5 inclusive

    private static final long ACTUATOR_PROMETHEUS_TOTAL_WAIT = 2_000L;
    private static final long ACTUATOR_PROMETHEUS_INCREMENTAL_WAIT = 100L;
    private static final long ACTUATOR_PROMETHEUS_MAX_REPEATS = SECURE_RANDOM.nextLong(1, 2); // 1-2 inclusive

    private static final long OTEL_PROMETHEUS_TOTAL_WAIT = 5_000L;
    private static final long OTEL_PROMETHEUS_INCREMENTAL_WAIT = 500L;
    private static final long OTEL_PROMETHEUS_MAX_REPEATS = SECURE_RANDOM.nextLong(1, 3); // 1-2 inclusive

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
