package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_OTEL_CONTRIB;
import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class HelloControllerIT extends AbstractIT {
    private static final long WAIT_MILLIS_API_IN_PROMETHEUS_METRICS = 10_000L;
    private static final long WAIT_MILLIS_API_IN_OTEL_METRICS = 10_000L;

    @Test
    void testPrometheus() {
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(super.baseUrl() + "/hello", String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        log.info("Hello API response:\n{}", responseBody);

        final String urlMetricsPrometheus = super.baseUrl() + "/actuator/prometheus";
        final String urlMetricsOtel = "http://localhost:" + CONTAINER_OTEL_CONTRIB.get().getMappedPort(4317) + "/v1/metrics";

        String latestResponsePrometheus, latestResponseOtel;

        final long endTimeMillisPrometheus = System.currentTimeMillis() + WAIT_MILLIS_API_IN_PROMETHEUS_METRICS; // 10 seconds timeout
        do {
            latestResponsePrometheus = doHttpGet(urlMetricsPrometheus);
            log.info("Prometheus response:\nPROMETHEUS>>>{}", prefixAllLines("PROMETHEUS", latestResponsePrometheus));
        } while (!latestResponsePrometheus.contains("/hello") && System.currentTimeMillis() < endTimeMillisPrometheus);
        assertTrue(latestResponsePrometheus.contains("/hello"));

        final long endTimeMillisOtel = System.currentTimeMillis() + WAIT_MILLIS_API_IN_OTEL_METRICS; // 10 seconds timeout
        do {
            latestResponseOtel = doHttpGet(urlMetricsOtel);
            log.info("Otel response:\nPROMETHEUS>>>{}", prefixAllLines("OTEL", latestResponsePrometheus));
        } while (!latestResponseOtel.contains("/hello") && System.currentTimeMillis() < endTimeMillisOtel);
        assertTrue(latestResponseOtel.contains("/hello"));
    }
}
