package com.justincranford.oteldemo;

import com.justincranford.oteldemo.containers.ContainerManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"livedemo", "test", "otel", "default"})
@Getter
@Accessors(fluent=true)
@Slf4j
public abstract class AbstractIT {
    @DynamicPropertySource
    static void registerDynamicProperties(final DynamicPropertyRegistry registry) throws Exception {
        ContainerManager.initialize(registry);
    }

    @Value("${otel.demo.livedemo:false}")
    private boolean livedemo;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @LocalServerPort
    private int localServerPort;

    protected String baseUrl() {
        return "http://" + this.serverAddress() + ":" + this.localServerPort();
    }
}
