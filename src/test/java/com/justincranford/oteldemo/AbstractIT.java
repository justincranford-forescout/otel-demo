package com.justincranford.oteldemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {})
@ActiveProfiles({"test", "default"})
@Getter
@Accessors(fluent = true)
@Slf4j
public class AbstractIT {
    private static final ClassLoader CLASS_LOADER = AbstractIT.class.getClassLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String OTEL_CONTRIB_YAML_FILE_PATH = requireNonNull(CLASS_LOADER.getResource("otel-config.yaml")).getPath();
    private static final GenericContainer<?> OTEL_CONTRIB_CONTAINER = new GenericContainer<>(DockerImageName.parse("otel/opentelemetry-collector:latest"))
            .withExposedPorts(4317)
            .withFileSystemBind(OTEL_CONTRIB_YAML_FILE_PATH, "/etc/otelcol-contrib/config.yaml", BindMode.READ_ONLY);

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @LocalServerPort
    private int localServerPort;

    protected String baseUrl() {
        return "http://" + this.serverAddress() + ":" + this.localServerPort();
    }

    // Spring Boot actuator properties: https://docs.spring.io/spring-boot/appendix/application-properties/index.html
    @DynamicPropertySource
    static void otelProperties(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        OTEL_CONTRIB_CONTAINER.start();
        final Integer grpcPort = OTEL_CONTRIB_CONTAINER.getMappedPort(4317); // GRPC port 4317 more efficient than HTTP port 4318

        final Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("management.otlp.metrics.export.enabled", "true");
        configMap.put("management.otlp.metrics.export.url", "http://localhost:" + grpcPort);
        configMap.put("management.otlp.metrics.export.step", "1m");
        configMap.put("management.otlp.tracing.export.enabled", "true");
        configMap.put("management.otlp.tracing.endpoint", "http://localhost:" + grpcPort);
        configMap.put("management.otlp.tracing.transport", "grpc");
        configMap.put("management.otlp.tracing.compression", "gzip");
        configMap.put("management.otlp.logging.export.enabled", "true");
        configMap.put("management.otlp.logging.endpoint", "http://localhost:" + grpcPort);
        configMap.put("management.otlp.logging.transport", "grpc");
        configMap.put("management.otlp.logging.compression", "gzip");
        configMap.put("otel.propagators", "tracecontext,b3");
        configMap.put("otel.resource.attributes.deployment.environment", "integTest");
        configMap.put("otel.resource.attributes.service.name", "otel-demo_integTest");
        configMap.put("otel.resource.attributes.service.namespace", "justincranford-forescout");

        log.info("Spring Boot Actuator dynamic properties for otel-contrib testcontainer: {}", OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(configMap));
        configMap.forEach((propertyName,propertyValue) -> registry.add(propertyName, () -> propertyValue));
    }
}
