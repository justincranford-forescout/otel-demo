package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_OTEL_CONTRIB;
import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class HelloControllerIT extends AbstractIT {
    private static final long INCREMENTAL_WAIT_MILLIS_PROMETHEUS_ACTUATOR = 100L;
    private static final long TOTAL_WAIT_MILLIS_PROMETHEUS_ACTUATOR = 2_000L;
    private static final long INCREMENTAL_WAIT_MILLIS_PROMETHEUS_OTEL = 500L;
    private static final long TOTAL_WAIT_MILLIS_PROMETHEUS_OTEL = 10_000L;

    @Test
    void testHelloApi_verifyBuiltInPrometheus_verifyOtelPrometheus() {
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(super.baseUrl() + "/hello", String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        log.info("Hello API response:\n{}", responseBody);

        verifySpringBootActuatorPrometheus(TOTAL_WAIT_MILLIS_PROMETHEUS_ACTUATOR + SECURE_RANDOM.nextLong(1), INCREMENTAL_WAIT_MILLIS_PROMETHEUS_ACTUATOR + SECURE_RANDOM.nextLong(1));
        verifyOtelContribPrometheus(TOTAL_WAIT_MILLIS_PROMETHEUS_OTEL + SECURE_RANDOM.nextLong(1), INCREMENTAL_WAIT_MILLIS_PROMETHEUS_OTEL + SECURE_RANDOM.nextLong(1));
    }

    // Verify that the /hello API metrics are present in the embedded Spring Boot Actuator Prometheus metrics endpoint
    private void verifySpringBootActuatorPrometheus(final long totalWaitMillis, final long incrementalWaitMillis) {
        final String url = super.baseUrl() + "/actuator/prometheus";
        final long endTimeMillisPrometheus = System.currentTimeMillis() + totalWaitMillis;
        String latestResponsePrometheus = "";
        do {
            try {
                latestResponsePrometheus = doHttpGet(url);
                log.info("Spring Boot Actuator response:\n{}", prefixAllLines("ACTUATOR", latestResponsePrometheus));
            } catch(ResourceAccessException e) {
                log.warn("Spring Boot Actuator error: {}", e.getMessage());
                wait_(incrementalWaitMillis);
            }
        } while (!latestResponsePrometheus.contains("/hello") && System.currentTimeMillis() < endTimeMillisPrometheus);
        assertTrue(latestResponsePrometheus.contains("/hello"));
    }

    // Verify that the /hello API metrics are present in the external Otel Contrib Prometheus metrics endpoint
    private void verifyOtelContribPrometheus(final long totalWaitMillis, final long incrementalWaitMillis) {
        final String url = "http://localhost:" + CONTAINER_OTEL_CONTRIB.get().getMappedPort(8888) + "/metrics";
        final long endTimeMillisOtel = System.currentTimeMillis() + totalWaitMillis;
        String latestResponseOtel = "";
        do {
            try {
                latestResponseOtel = doHttpGet(url);
                log.info("Otel Contrib response:\n{}", prefixAllLines("OTEL-CONTRIB", latestResponseOtel));
            } catch(ResourceAccessException e) {
                log.warn("Otel Contrib error: {}", e.getMessage());
                wait_(incrementalWaitMillis);
            }
        } while (!latestResponseOtel.contains("/hello") && System.currentTimeMillis() < endTimeMillisOtel);
        assertTrue(latestResponseOtel.contains("/hello"));
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
