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
    void testPrometheus() {
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(super.baseUrl() + "/hello", String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        log.info("Prometheus response:\n{}", responseBody);
    }
}
