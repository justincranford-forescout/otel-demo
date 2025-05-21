package com.justincranford.oteldemo;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestcontainersConfiguration.class})
@ActiveProfiles({"test", "default"})
@Getter
@Accessors(fluent = true)
public class AbstractIT {
    @Autowired
    private ApplicationContext applicationContext;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @LocalServerPort
    private int localServerPort;

    protected String baseUrl() {
        return "http://" + this.serverAddress() + ":" + this.localServerPort();
    }
}
