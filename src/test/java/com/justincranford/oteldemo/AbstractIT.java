package com.justincranford.oteldemo;

import com.justincranford.oteldemo.containers.ContainerManager;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "default"})
@Getter
@Accessors(fluent=true)
@Slf4j
public class AbstractIT {
    static {
        ContainerManager.initialize();
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @LocalServerPort
    private int localServerPort;

    protected String baseUrl() {
        return "http://" + this.serverAddress() + ":" + this.localServerPort();
    }

    protected String doHttpGet(final String url) {
        log.info("Getting URL {}", url);
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        return responseBody;
    }
}
