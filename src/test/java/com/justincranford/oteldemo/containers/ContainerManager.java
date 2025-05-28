package com.justincranford.oteldemo.containers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static java.util.Objects.requireNonNull;

@Slf4j
public class ContainerManager {
    private static final ClassLoader CLASS_LOADER = ContainerManager.class.getClassLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Duration TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START = Duration.ofSeconds(45);
    private static final DockerImageName DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER = DockerImageName.parse("otel/opentelemetry-collector:latest");
    private static final DockerImageName DOCKER_IMAGE_NAME_GRAFANA_LGTM = DockerImageName.parse("grafana/otel-lgtm:latest");

    private static final AtomicReference<Boolean> INITIALIZED = new AtomicReference<>(Boolean.FALSE);
    public static final AtomicReference<GenericContainer<?>> CONTAINER_OTEL_CONTRIB = new AtomicReference<>(); // OpenTelemetry + Contrib
    public static final AtomicReference<GenericContainer<?>> CONTAINER_GRAFANA_LGTM = new AtomicReference<>(); // Grafana LGTM (Logs=Loki, GUI=Grafana, Traces=Tempo, Metrics=Prometheus)
    private static final List<AtomicReference<GenericContainer<?>>> CONTAINER_REFERENCES = List.of(CONTAINER_OTEL_CONTRIB, CONTAINER_GRAFANA_LGTM);

    public static void initialize(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        if (INITIALIZED.getAndSet(Boolean.TRUE)) {
            log.info("Test containers already initialized, skipping starting containers.");
            return;
        }

        final List<Thread> startContainerThreads = List.of(asyncStartContainerOtelContrib(), asyncStartContainerGrafanaLgtm());

        final long millisStart = System.currentTimeMillis();
        for (final Thread startContainerThread : startContainerThreads) {
            if (startContainerThread.isAlive()) {
                final long millisRemaining = TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START.toMillis() - (System.currentTimeMillis() - millisStart);
                if (millisRemaining <= 0) {
                    final String message = startContainerThread.getName() + " took too long to finish";
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
                    final String message = startContainerThread.getName() + " timed out waiting to finish";
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (final AtomicReference<GenericContainer<?>> containerReference : CONTAINER_REFERENCES) {
                if (containerReference.get() == null) {
                    log.warn("Container reference is null.");
                } else if (!containerReference.get().isRunning()) {
                    log.info("Container is not running.");
                } else {
                    log.info("Container stopping...\n{}", prefixAllLines("CONTAINER", containerReference.get().getLogs()));
                    containerReference.get().stop();
                }
            }
        }));

        registerDynamicProperties(registry);
    }

    public static void registerDynamicProperties(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        final Integer grpcPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4317); // GRPC port 4317;  more efficient than HTTP
        final Integer httpPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4318); // HTTP port 4318
        final Map<String, String> configMap = createOtelContribContainerProperties(grpcPort, httpPort);

        log.info("Spring Boot Actuator dynamic properties for otel-contrib testcontainer: {}", OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(configMap));
        configMap.forEach((propertyName,propertyValue) -> registry.add(propertyName, () -> propertyValue));
    }

    @SuppressWarnings({"unused"})
    private static @NotNull Map<String, String> createOtelContribContainerProperties(final Integer grpcPort, final Integer httpPort) {
        final Map<String, String> configMap = new LinkedHashMap<>();

        /// [org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsProperties] (since 3.0.0)
        configMap.put("management.otlp.metrics.export.step", "3s"); // override default 1ms in application.properties for quick testing
//        configMap.put("management.otlp.metrics.export.url", "http://localhost:" + httpPort + "/v1/metrics");
        configMap.put("management.otlp.metrics.export.url", "http://localhost:" + grpcPort);

        /// [org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingProperties] (since 3.4.0)
//        configMap.put("management.otlp.tracing.endpoint", "http://localhost:" + httpPort + "/v1/traces");
//        configMap.put("management.otlp.tracing.transport", "HTTP");
        configMap.put("management.otlp.tracing.endpoint", "http://localhost:" + grpcPort); // override port 4317 in application.properties
        configMap.put("management.otlp.tracing.transport", "GRPC");

        /// [org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingProperties] (since 3.4.0)
//        configMap.put("management.otlp.logging.endpoint", "http://localhost:" + httpPort + "/v1/logs");
//        configMap.put("management.otlp.logging.transport", "HTTP");
        configMap.put("management.otlp.logging.endpoint", "http://localhost:" + grpcPort); // override port 4317 in application.properties
        configMap.put("management.otlp.logging.transport", "GRPC");
        return configMap;
    }

    @SuppressWarnings({"resource"})
    private static @NotNull Thread asyncStartContainerOtelContrib() {
        final Thread otelContribThread = new Thread(() -> {
            if (CONTAINER_OTEL_CONTRIB.get() == null) {
                final long nanosStart = System.nanoTime();
                final String otelContribYamlFilePath = requireNonNull(CLASS_LOADER.getResource("otel-config.yaml")).getPath();
                final GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER).withNetworkAliases("otel-1").withExposedPorts(4317, 4318, 8888)
                        .withFileSystemBind(otelContribYamlFilePath, "/etc/otelcol-contrib/config.yaml", BindMode.READ_ONLY);
                container.start(); // long blocking call
                CONTAINER_OTEL_CONTRIB.set(container);
                log.info("{} finished starting in {}", DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER, Duration.ofNanos(System.nanoTime() - nanosStart));
            } else {
                log.warn("{} is already started", DOCKER_IMAGE_NAME_OTEL_CONTRIB_CONTAINER);
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
                log.info("{} finished starting in {}", DOCKER_IMAGE_NAME_GRAFANA_LGTM, Duration.ofNanos(System.nanoTime() - nanosStart));
            } else {
                log.warn("{} is already started", DOCKER_IMAGE_NAME_GRAFANA_LGTM);
            }
        });
        grafanaThread.setName(DOCKER_IMAGE_NAME_GRAFANA_LGTM.toString());
        grafanaThread.setDaemon(true);
        grafanaThread.start();
        return grafanaThread;
    }
}
