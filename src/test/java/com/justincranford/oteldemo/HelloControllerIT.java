package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class HelloControllerIT extends AbstractIT {
    @Test
    void testHelloApi_verifyBuiltInPrometheus_verifyOtelPrometheus() {
        extracted(super.baseUrl() + "/hello");
    }

    @Test
    void testHelloTelemetryApi_verifyBuiltInPrometheus_verifyOtelPrometheus() {
        extracted(super.baseUrl() + "/hello/telemetry");
    }

    private static void extracted(String url) {
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        log.info("Hello Controller {} API response:\n{}", url, responseBody);
    }
}
