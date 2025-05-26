package com.justincranford.oteldemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {})
@ActiveProfiles({"test", "default"})
@Getter
@Accessors(fluent = true)
@Slf4j
public class AbstractIT {
    private static final ClassLoader CLASS_LOADER = AbstractIT.class.getClassLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Duration TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START = Duration.ofSeconds(45);
    private static final DockerImageName DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER = DockerImageName.parse("otel/opentelemetry-collector:latest");
    private static final DockerImageName DOCKER_IMAGE_NAME_GRAFANA_LGTM = DockerImageName.parse("grafana/otel-lgtm:latest");

    private static final AtomicReference<GenericContainer<?>> CONTAINER_OTEL_CONTRIB = new AtomicReference<>(); // OpenTelemetry + Contrib
    private static final AtomicReference<GenericContainer<?>> CONTAINER_GRAFANA_LGTM = new AtomicReference<>(); // Grafana LGTM (Logs=Loki, GUI=Grafana, Traces=Tempo, Metrics=Prometheus)
    private static final List<AtomicReference<GenericContainer<?>>> CONTAINER_REFERENCES = List.of(CONTAINER_OTEL_CONTRIB, CONTAINER_GRAFANA_LGTM);

    @AfterAll
    static void stopContainers() {
        for (final AtomicReference<GenericContainer<?>> containerReference : CONTAINER_REFERENCES) {
            if (containerReference.get() == null) {
                log.warn("Container reference is null.");
            } else if (!containerReference.get().isRunning()) {
                log.info("Container is not running.");
            } else {
                final String logs = containerReference.get().getLogs().replaceAll("(\r)?\n(\r)?", "$1\n$2CONTAINER>>> ");
                log.info("Container stopping...\nCONTAINER>>> {}", logs);
                containerReference.get().stop();
            }
        }
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


    // Spring Boot actuator properties: https://docs.spring.io/spring-boot/appendix/application-properties/index.html
    @DynamicPropertySource
    private static void registerDynamicProperties(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        startContainers();
        final Integer grpcPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4317); // GRPC port 4317 more efficient than HTTP port 4318

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

    private static void startContainers() {
        final List<Thread> startContainerThreads = List.of(asyncStartContainerOtelContrib(), asyncStartContainerGrafanaLgtm());

        final long millisStart = System.currentTimeMillis();
        for (final Thread startContainerThread : startContainerThreads) {
            if (startContainerThread.isAlive()) {
                final long millisRemaining = TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START.toMillis() - (System.currentTimeMillis() - millisStart);
                if (millisRemaining <= 0) {
                    final String message = "Container " + startContainerThread.getName() + " took too long to finish";
                    log.error(message);
                    startContainerThreads.stream().filter(Thread::isAlive).parallel().forEach(Thread::interrupt); // interrupt all remaining alive threads
                    throw new RuntimeException(message);
                }
                try {
                    startContainerThread.join(millisRemaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for thread " + startContainerThread.getName() + " to end", e);
                }
                if (startContainerThread.isAlive()) {
                    final String message = "Container " + startContainerThread.getName() + " timed out waiting to finish";
                    log.error(message);
                    startContainerThreads.stream().filter(Thread::isAlive).parallel().forEach(Thread::interrupt); // interrupt all remaining alive threads
                    throw new RuntimeException(message);
                }
            }
        }
        for (final AtomicReference<GenericContainer<?>> containerReference : CONTAINER_REFERENCES) {
            if (containerReference.get() == null) {
                log.error("Container reference is null after starting all containers.");
                throw new IllegalStateException("Container reference is null after starting all containers.");
            } else if (!containerReference.get().isRunning()) {
                log.error("Container is not running after starting all containers.");
                throw new IllegalStateException("Container is not running after starting all containers.");
            }
        }
    }

    @SuppressWarnings({"resource"})
    private static @NotNull Thread asyncStartContainerOtelContrib() {
        final Thread otelContribThread = new Thread(() -> {
            if (CONTAINER_OTEL_CONTRIB.get() == null) {
                final long nanosStart = System.nanoTime();
                final String otelContribYamlFilePath = requireNonNull(CLASS_LOADER.getResource("otel-config.yaml")).getPath();
                final GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER).withNetworkAliases("otel-1").withExposedPorts(4317)
                        .withFileSystemBind(otelContribYamlFilePath, "/etc/otelcol-contrib/config.yaml", BindMode.READ_ONLY);
                container.start(); // long blocking call
                CONTAINER_OTEL_CONTRIB.set(container);
                log.info("Container {} finished starting in {}", DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER, Duration.ofNanos(System.nanoTime() - nanosStart));
            } else {
                log.warn("Container {} is already started", DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER);
            }
        });
        otelContribThread.setName(DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER.toString());
        otelContribThread.setDaemon(true);
        otelContribThread.start();
        return otelContribThread;
    }

    @SuppressWarnings({"resource"})
    private static @NotNull Thread asyncStartContainerGrafanaLgtm() {
        final Thread grafanaThread = new Thread(() -> {
            if (CONTAINER_OTEL_CONTRIB.get() == null) {
                final long nanosStart = System.nanoTime();
                final GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME_GRAFANA_LGTM).withNetworkAliases("grafana-1").withExposedPorts(3000)
                        .withEnv("GF_SECURITY_ADMIN_PASSWORD", "admin").withEnv("GF_SECURITY_ADMIN_USER", "admin");
                container.start(); // long blocking call
                CONTAINER_GRAFANA_LGTM.set(container);
                log.info("Container {} finished starting in {}", DOCKER_IMAGE_NAME_GRAFANA_LGTM, Duration.ofNanos(System.nanoTime() - nanosStart));
            } else {
                log.warn("Container {} is already started", DOCKER_IMAGE_NAME_GRAFANA_LGTM);
            }
        });
        grafanaThread.setName(DOCKER_IMAGE_NAME_GRAFANA_LGTM.toString());
        grafanaThread.setDaemon(true);
        grafanaThread.start();
        return grafanaThread;
    }
}
