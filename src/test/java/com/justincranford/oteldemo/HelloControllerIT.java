package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.stream.IntStream;

import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_OTEL_CONTRIB;
import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class HelloControllerIT extends AbstractIT {
    private static final long TOTAL_WAIT_ACTUATOR_HEALTH       = 2_000L;
    private static final long INCREMENTAL_WAIT_ACTUATOR_HEALTH = 100L;

    private static final long TOTAL_WAIT_ACTUATOR_PROMETHEUS       = 2_000L;
    private static final long INCREMENTAL_WAIT_ACTUATOR_PROMETHEUS = 100L;

    private static final long TOTAL_WAIT_OTEL_PROMETHEUS       = 5_000L;
    private static final long INCREMENTAL_WAIT_OTEL_PROMETHEUS = 500L;

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

        for (final int i : IntStream.rangeClosed(1, SECURE_RANDOM.nextInt(2, 4)).toArray()) {
            final String response = pollHttpGet("HEALTH"+i,       actuatorHealth,     "\"status\":\"UP\"", TOTAL_WAIT_ACTUATOR_HEALTH, INCREMENTAL_WAIT_ACTUATOR_HEALTH);
            assertTrue(response.contains("\"status\":\"UP\""));
        }
        for (final int i : IntStream.rangeClosed(1, SECURE_RANDOM.nextInt(2, 4)).toArray()) {
            final String response = pollHttpGet("PROMETHEUS"+i,   actuatorPrometheus, "/hello",            TOTAL_WAIT_ACTUATOR_PROMETHEUS, INCREMENTAL_WAIT_ACTUATOR_PROMETHEUS);
            assertTrue(response.contains("/hello"));
        }
        for (final int i : IntStream.rangeClosed(1, SECURE_RANDOM.nextInt(1, 3)).toArray()) {
            final String response = pollHttpGet("OTEL-CONTRIB"+i, otelPrometheus,     "/hello",            TOTAL_WAIT_OTEL_PROMETHEUS, INCREMENTAL_WAIT_OTEL_PROMETHEUS);
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
