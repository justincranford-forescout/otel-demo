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
import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class HelloControllerIT extends AbstractIT {
    private static final long WAIT_MILLIS_API_IN_PROMETHEUS_METRICS = 10_000L;
    private static final long WAIT_MILLIS_API_IN_OTEL_METRICS = 10_000L;

    @Test
    void testHelloApi_verifyBuiltInPrometheus_verifyOtelPrometheus() {
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(super.baseUrl() + "/hello", String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        log.info("Hello API response:\n{}", responseBody);

        verifySpringBootActuatorPrometheus();
        verifyOtelContribPrometheus();
    }

    // Verify that the /hello API metrics are present in the embedded Spring Boot Actuator Prometheus metrics endpoint
    private void verifySpringBootActuatorPrometheus() {
        final String url = super.baseUrl() + "/actuator/prometheus";
        final long endTimeMillisPrometheus = System.currentTimeMillis() + WAIT_MILLIS_API_IN_PROMETHEUS_METRICS; // 10 seconds timeout
        String latestResponsePrometheus = "";
        do {
            try {
                latestResponsePrometheus = doHttpGet(url);
                log.info("Spring Boot Actuator response:\n{}", prefixAllLines("ACTUATOR", latestResponsePrometheus));
            } catch(ResourceAccessException e) {
                log.warn("Spring Boot Actuator error: {}", e.getMessage());
                wait_(100);
            }
        } while (!latestResponsePrometheus.contains("/hello") && System.currentTimeMillis() < endTimeMillisPrometheus);
        assertTrue(latestResponsePrometheus.contains("/hello"));
    }

    // Verify that the /hello API metrics are present in the external Otel Contrib Prometheus metrics endpoint
    private void verifyOtelContribPrometheus() {
        final String url = "http://localhost:" + CONTAINER_OTEL_CONTRIB.get().getMappedPort(8888) + "/metrics";
        final long endTimeMillisOtel = System.currentTimeMillis() + WAIT_MILLIS_API_IN_OTEL_METRICS; // 10 seconds timeout
        String latestResponseOtel = "";
        do {
            try {
                latestResponseOtel = doHttpGet(url);
                log.info("Otel Contrib response:\n{}", prefixAllLines("OTEL-CONTRIB", latestResponseOtel));
            } catch(ResourceAccessException e) {
                log.warn("Otel Contrib error: {}", e.getMessage());
                wait_(500);
            }
        } while (!latestResponseOtel.contains("/hello") && System.currentTimeMillis() < endTimeMillisOtel);
        assertTrue(latestResponseOtel.contains("/hello"));
    }

    private static void wait_(int millis)  {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for {} milliseconds", millis);
        }
    }
}
