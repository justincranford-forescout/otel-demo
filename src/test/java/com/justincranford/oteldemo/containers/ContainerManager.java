package com.justincranford.oteldemo.containers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.Transport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.justincranford.oteldemo.util.Utils.prefixAllLines;
import static java.util.Objects.requireNonNull;

@Slf4j
public class ContainerManager {
    private static final Transport PREFERRED_TRANSPORT = Transport.GRPC; // HTTP or GRPC; only affects Traces and Logs; Micrometer limitation doesn't support GRPC Metrics (yet?)

    private static final ClassLoader CLASS_LOADER = ContainerManager.class.getClassLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Duration TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START = Duration.ofSeconds(45);
    private static final DockerImageName DOCKER_IMAGE_NAME_OTEL_CONTRIB = DockerImageName.parse("otel/opentelemetry-collector:latest");
    private static final DockerImageName DOCKER_IMAGE_NAME_GRAFANA_LGTM = DockerImageName.parse("grafana/otel-lgtm:latest");

    private static final AtomicReference<Boolean> INITIALIZED = new AtomicReference<>(Boolean.FALSE);
    public static final AtomicReference<GenericContainer<?>> CONTAINER_OTEL_CONTRIB = new AtomicReference<>(); // OpenTelemetry + Contrib
    public static final AtomicReference<GenericContainer<?>> CONTAINER_GRAFANA_LGTM = new AtomicReference<>(); // Grafana LGTM (Logs=Loki, GUI=Grafana, Traces=Tempo, Metrics=Prometheus)
    private static final List<AtomicReference<GenericContainer<?>>> CONTAINER_REFERENCES = List.of(CONTAINER_OTEL_CONTRIB, CONTAINER_GRAFANA_LGTM);
    public static final Integer[] CONTAINER_PORTS_OTEL_CONTRIB = {4317, 4318, 8888};
    public static final Integer[] CONTAINER_PORTS_GRAFANA_LGTM = {4317, 4318, 3000};

    public static void initialize(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        if (INITIALIZED.getAndSet(Boolean.TRUE)) {
            log.info("Test containers already initialized, skipping starting containers.");
            return;
        }

        // Group 1: grafana-lgtm
        startContainersConcurrently(List.of(asyncStartContainerGrafanaLgtm())); // grafana-lgtm must start before otel-contrib
        final Integer grafanaGrpcPort = CONTAINER_GRAFANA_LGTM.get().getMappedPort(4317);
        final Integer grafanaHttpPort = CONTAINER_GRAFANA_LGTM.get().getMappedPort(4318);

        // Group 2: otel-contrib (depends on grafana-lgtm mapped port)
        startContainersConcurrently(List.of(asyncStartContainerOtelContrib(grafanaGrpcPort, grafanaHttpPort)));

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
                } else {
                    final String containerName = containerReference.get().getDockerImageName();
                    final String containerLogs = prefixAllLines(containerName, containerReference.get().getLogs());
                    System.out.println(containerLogs);
                    if (containerReference.get().isRunning()) {
                        log.info("Container stopping...");
                        containerReference.get().stop();
                        log.info("Container stopped");
                    } else {
                        log.info("Container not running.");
                    }
                }
            }
        }));

        registerDynamicProperties(registry);
    }

    private static void startContainersConcurrently(final List<Thread> startContainerThreadsPhase1) {
        final long millisStart = System.currentTimeMillis();
        for (final Thread startContainerThread : startContainerThreadsPhase1) {
            if (startContainerThread.isAlive()) {
                final long millisRemaining = TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START.toMillis() - (System.currentTimeMillis() - millisStart);
                if (millisRemaining <= 0) {
                    final String message = startContainerThread.getName() + " took too long to finish";
                    log.error(message);
                    startContainerThreadsPhase1.stream().filter(Thread::isAlive).parallel().forEach(Thread::interrupt); // interrupt all remaining alive threads
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
                    startContainerThreadsPhase1.stream().filter(Thread::isAlive).parallel().forEach(Thread::interrupt); // interrupt all remaining alive threads
                    throw new RuntimeException(message);
                }
            }
        }
    }

    public static void registerDynamicProperties(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        final Integer otelGrpcPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4317); // GRPC port 4317
        final Integer otelHttpPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4318); // HTTP port 4318
        final Map<String, String> configMap = createOtelContribContainerProperties(otelGrpcPort, otelHttpPort);

        log.info("Spring Boot Actuator dynamic properties for otel-contrib testcontainer: {}", OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(configMap));
        configMap.forEach((propertyName,propertyValue) -> registry.add(propertyName, () -> propertyValue));
    }

    @SuppressWarnings({"unused"})
    private static @NotNull Map<String, String> createOtelContribContainerProperties(final Integer grpcPort, final Integer httpPort) {
        final Map<String, String> configMap = new LinkedHashMap<>();

        /// [org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsProperties] (since 3.0.0) - HTTP only transport (because Micrometer)
        configMap.put("management.otlp.metrics.export.step", "2s"); // fast for tests; default 1m too slow
        configMap.put("management.otlp.metrics.export.url", "http://localhost:" + httpPort + "/v1/metrics");

        /// [org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingProperties] (since 3.4.0) - HTTP or GRPC transport
        /// [org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingProperties] (since 3.4.0) - HTTP or GRPC transport
        configMap.put("management.otlp.tracing.transport", PREFERRED_TRANSPORT.name());
        configMap.put("management.otlp.logging.transport", PREFERRED_TRANSPORT.name());
        switch (PREFERRED_TRANSPORT) {
            case HTTP:
                configMap.put("management.otlp.tracing.endpoint", "http://localhost:" + httpPort + "/v1/traces");
                configMap.put("management.otlp.logging.endpoint", "http://localhost:" + httpPort + "/v1/logs");
                break;
            case GRPC:
                configMap.put("management.otlp.tracing.endpoint", "http://localhost:" + grpcPort);
                configMap.put("management.otlp.logging.endpoint", "http://localhost:" + grpcPort);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transport " + PREFERRED_TRANSPORT);
        }
        return configMap;
    }

    @SuppressWarnings({"resource"})
    private static @NotNull Thread asyncStartContainerOtelContrib(final Integer grafanaLgtmGrpcPort, final Integer grafanaLgtmHttpPort) {
        if (grafanaLgtmGrpcPort == null || grafanaLgtmHttpPort == null) {
            throw new IllegalArgumentException("Grafana GRPC and HTTP ports must be non-null");
        }
        final Thread otelContribThread = new Thread(() -> {
            if (CONTAINER_OTEL_CONTRIB.get() == null) {
                final long nanosStart = System.nanoTime();
                final String otelContribYamlFileOriginalPath = requireNonNull(CLASS_LOADER.getResource("otel-config.yaml")).getPath();
                final String otelContribYamlFileOriginalContents = readFileContents(otelContribYamlFileOriginalPath, StandardCharsets.UTF_8);
                final String otelContribYamlFileUpdatedContents = otelContribYamlFileOriginalContents.replace("host.docker.internal:4317", "host.docker.internal:" + grafanaLgtmGrpcPort);
                final String otelContribYamlFileUpdatedPath = writeFileContents(otelContribYamlFileUpdatedContents);
                final GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME_OTEL_CONTRIB).withNetworkAliases("otel-1").withExposedPorts(CONTAINER_PORTS_OTEL_CONTRIB)
                        .withFileSystemBind(otelContribYamlFileUpdatedPath, "/etc/otelcol-contrib/config.yaml", BindMode.READ_ONLY);
                container.start(); // long blocking call
                CONTAINER_OTEL_CONTRIB.set(container);
                final List<String> mappedPorts = Arrays.stream(CONTAINER_PORTS_OTEL_CONTRIB).map(port -> port + ":" + container.getMappedPort(port)).toList();
                log.info("{} started in {}, ports: {}", DOCKER_IMAGE_NAME_OTEL_CONTRIB, Duration.ofNanos(System.nanoTime() - nanosStart), mappedPorts);
            } else {
                log.warn("{} already running", DOCKER_IMAGE_NAME_OTEL_CONTRIB);
            }
        });
        otelContribThread.setName(DOCKER_IMAGE_NAME_OTEL_CONTRIB.toString());
        otelContribThread.setDaemon(true);
        otelContribThread.start();
        return otelContribThread;
    }

    private static String readFileContents(String otelContribYamlFilePath, final Charset charset) {
        try {
            return Files.readString(Path.of(otelContribYamlFilePath), charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String writeFileContents(String otelContribYamlFileUpdatedContents) {
        try {
            final Path tempOtelConfig = Files.createTempFile("otel-config-", ".yaml");
            Files.writeString(tempOtelConfig, otelContribYamlFileUpdatedContents, StandardCharsets.UTF_8);
            return tempOtelConfig.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"resource"})
    private static @NotNull Thread asyncStartContainerGrafanaLgtm() {
        final Thread grafanaThread = new Thread(() -> {
            if (CONTAINER_OTEL_CONTRIB.get() == null) {
                final long nanosStart = System.nanoTime();
                final GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME_GRAFANA_LGTM).withNetworkAliases("grafana-1").withExposedPorts(CONTAINER_PORTS_GRAFANA_LGTM)
                        .withEnv("GF_SECURITY_ADMIN_PASSWORD", "admin").withEnv("GF_SECURITY_ADMIN_USER", "admin");
                container.start(); // long blocking call
                CONTAINER_GRAFANA_LGTM.set(container);
                final List<String> mappedPorts = Arrays.stream(CONTAINER_PORTS_GRAFANA_LGTM).map(port -> port + ":" + container.getMappedPort(port)).toList();
                log.info("{} started in {}, ports: {}", DOCKER_IMAGE_NAME_GRAFANA_LGTM, Duration.ofNanos(System.nanoTime() - nanosStart), mappedPorts);
            } else {
                log.warn("{} already running", DOCKER_IMAGE_NAME_GRAFANA_LGTM);
            }
        });
        grafanaThread.setName(DOCKER_IMAGE_NAME_GRAFANA_LGTM.toString());
        grafanaThread.setDaemon(true);
        grafanaThread.start();
        return grafanaThread;
    }
}
