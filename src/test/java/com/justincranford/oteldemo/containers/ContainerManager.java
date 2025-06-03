package com.justincranford.oteldemo.containers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.Transport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.justincranford.oteldemo.containers.ContainerUtil.getMappedPorts;
import static com.justincranford.oteldemo.containers.ContainerUtil.startContainersConcurrently;
import static com.justincranford.oteldemo.util.Utils.*;
import static java.util.Objects.requireNonNull;

@Slf4j
public class ContainerManager {
    private static final ClassLoader CLASS_LOADER = ContainerManager.class.getClassLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Duration TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START = Duration.ofSeconds(45);
    private static final DockerImageName DOCKER_IMAGE_NAME_GRAFANA_LGTM = DockerImageName.parse("grafana/otel-lgtm:latest");
    private static final DockerImageName DOCKER_IMAGE_NAME_OTEL_CONTRIB = DockerImageName.parse("otel/opentelemetry-collector:latest");

    public static final AtomicReference<GenericContainer<?>> CONTAINER_GRAFANA_LGTM = new AtomicReference<>(); // Grafana LGTM (Logs=Loki, GUI=Grafana, Traces=Tempo, Metrics=Prometheus)
    public static final Integer[] CONTAINER_PORTS_GRAFANA_LGTM = {3000, 4317, 4318, 3200, 9090}; // 3000=Grafana, 4317=OTLP GRPC, 4318=OTLP HTTP, 3200=Tempo, 9090=Prometheus

    public static final AtomicReference<GenericContainer<?>> CONTAINER_OTEL_CONTRIB = new AtomicReference<>(); // OpenTelemetry + Contrib
    public static final Integer[] CONTAINER_PORTS_OTEL_CONTRIB = {4317, 4318, 8888}; // 4317=GRPC, 4318=HTTP, 8888=Metrics (Prometheus)
    private static final Transport CONTAINER_OTEL_CONTRIB_PREFERRED_TRANSPORT = Transport.HTTP; // HTTP or GRPC; only affects Traces and Logs; Micrometer limitation doesn't support GRPC Metrics (yet?)

    private static final AtomicReference<Boolean> INITIALIZED = new AtomicReference<>(Boolean.FALSE);
    private static final List<AtomicReference<GenericContainer<?>>> CONTAINER_REFERENCES = List.of(CONTAINER_GRAFANA_LGTM, CONTAINER_OTEL_CONTRIB);

    public static void initialize(final DynamicPropertyRegistry registry) throws JsonProcessingException {
        if (INITIALIZED.getAndSet(Boolean.TRUE)) {
            log.info("Test containers already initialized, skipping starting containers.");
            return;
        }

        // Group 1: grafana-lgtm (must start before otel-contrib)
        startContainersConcurrently(List.of(asyncStartContainerGrafanaLgtm()), TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START);
        final Map<Integer, Integer> grafanaLgtmMappedPorts = getMappedPorts(CONTAINER_GRAFANA_LGTM.get(), CONTAINER_PORTS_GRAFANA_LGTM);
        log.info("{}, mapped ports: {}", CONTAINER_GRAFANA_LGTM, grafanaLgtmMappedPorts);

        // Group 2: otel-contrib (must start after grafana-lgtm, because it will export telemetry through the mapped ports of grafana-lgtm)
        startContainersConcurrently(List.of(asyncStartContainerOtelContrib(grafanaLgtmMappedPorts)), TOTAL_DURATION_FOR_ALL_CONTAINERS_TO_START);
        final Map<Integer, Integer> otelContribMappedPorts = getMappedPorts(CONTAINER_OTEL_CONTRIB.get(), CONTAINER_PORTS_OTEL_CONTRIB);
        log.info("{}, mapped ports: {}", CONTAINER_OTEL_CONTRIB, otelContribMappedPorts);

        for (final AtomicReference<GenericContainer<?>> containerReference : CONTAINER_REFERENCES) {
            if (containerReference.get() == null) {
                log.error("Container reference is null after starting all containers.");
                throw new IllegalStateException("Container reference is null after starting all containers.");
            } else if (!containerReference.get().isRunning()) {
                log.error("Container is not running after starting all containers.");
                throw new IllegalStateException("Container is not running after starting all containers.");
            }
        }

        // gracefully shutdown containers on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (final AtomicReference<GenericContainer<?>> containerReference : CONTAINER_REFERENCES) {
                if (containerReference.get() == null) {
                    log.warn("Container reference is null.");
                    continue;
                }
                System.out.println(prefixAllLines(containerReference.get().getDockerImageName(), containerReference.get().getLogs())); // use STDOUT in case slf4j is shut down
                if (containerReference.get().isRunning()) {
                    log.info("Container stopping...");
                    containerReference.get().stop();
                    log.info("Container stopped");
                } else {
                    log.info("Container not running.");
                }
            }
        }));

        final Map<String, String> configMap = createOtelContribContainerProperties();
        log.info("Spring Boot Actuator dynamic properties for otel-contrib testcontainer: {}", OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(configMap));
        configMap.forEach((propertyName,propertyValue) -> registry.add(propertyName, () -> propertyValue));
    }

    private static @NotNull Thread asyncStartContainerGrafanaLgtm() {
        final Thread grafanaThread = new Thread(() -> {
            if (CONTAINER_GRAFANA_LGTM.get() == null) {
                final long nanosStart = System.nanoTime();
                final LgtmStackContainer container = new LgtmStackContainer(DOCKER_IMAGE_NAME_GRAFANA_LGTM).withNetworkAliases("grafana-1").withExposedPorts(CONTAINER_PORTS_GRAFANA_LGTM)
                        .withEnv("GF_SECURITY_ADMIN_PASSWORD", "admin").withEnv("GF_SECURITY_ADMIN_USER", "admin");
                container.start(); // long blocking call
                CONTAINER_GRAFANA_LGTM.set(container);
                log.info("{} started in {}, ports: {}", DOCKER_IMAGE_NAME_GRAFANA_LGTM, Duration.ofNanos(System.nanoTime() - nanosStart), getMappedPorts(container, CONTAINER_PORTS_GRAFANA_LGTM));
                log.info("Grafana URL: {}", container.getGrafanaHttpUrl());
                log.info("OTLP GRPC URL: {}", container.getOtlpGrpcUrl());
                log.info("OTLP HTTP URL: {}", container.getOtlpHttpUrl());
                log.info("Tempo URL: {}", container.getTempoUrl());
                log.info("Prometheus URL: {}", container.getPrometheusHttpUrl());
            } else {
                log.warn("{} already running", DOCKER_IMAGE_NAME_GRAFANA_LGTM);
            }
        });
        grafanaThread.setName(DOCKER_IMAGE_NAME_GRAFANA_LGTM.toString());
        grafanaThread.setDaemon(true);
        grafanaThread.start();
        return grafanaThread;
    }

    @SuppressWarnings({"resource"})
    private static @NotNull Thread asyncStartContainerOtelContrib(final Map<Integer, Integer> grafanaLgtmMappedPorts) {
        if (grafanaLgtmMappedPorts == null) {
            throw new IllegalArgumentException("Mapped ports must be non-null");
        }
        final Thread otelContribThread = new Thread(() -> {
            if (CONTAINER_OTEL_CONTRIB.get() == null) {
                final long nanosStart = System.nanoTime();
                final String otelContribYamlFileOriginalPath = requireNonNull(CLASS_LOADER.getResource("otel-config.yaml")).getPath();
                final String otelContribYamlFileOriginalContents = readFileContents(otelContribYamlFileOriginalPath, StandardCharsets.UTF_8);
                final AtomicReference<String> otelContribYamlFileUpdatedContents = new AtomicReference<>(otelContribYamlFileOriginalContents);
                grafanaLgtmMappedPorts.forEach((internalPort, mappedPort) -> otelContribYamlFileUpdatedContents.set(
                        otelContribYamlFileUpdatedContents.get().replace("host.docker.internal:" + internalPort, "host.docker.internal:" + mappedPort)
                ));
                final String otelContribYamlFileUpdatedPath = writeFileContents(otelContribYamlFileUpdatedContents.get());
                log.info("Updated otel-config.yaml:\n{}", prefixAllLines("otel-config.yaml", otelContribYamlFileUpdatedContents.get())); // use STDOUT in case slf4j is shut down

                final GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME_OTEL_CONTRIB).withNetworkAliases("otel-1").withExposedPorts(CONTAINER_PORTS_OTEL_CONTRIB)
                        .withFileSystemBind(otelContribYamlFileUpdatedPath, "/etc/otelcol-contrib/config.yaml", BindMode.READ_ONLY);
                container.start(); // long blocking call
                CONTAINER_OTEL_CONTRIB.set(container);
                log.info("{} started in {}, ports: {}", DOCKER_IMAGE_NAME_OTEL_CONTRIB, Duration.ofNanos(System.nanoTime() - nanosStart), getMappedPorts(container, CONTAINER_PORTS_OTEL_CONTRIB));
            } else {
                log.warn("{} already running", DOCKER_IMAGE_NAME_OTEL_CONTRIB);
            }
        });
        otelContribThread.setName(DOCKER_IMAGE_NAME_OTEL_CONTRIB.toString());
        otelContribThread.setDaemon(true);
        otelContribThread.start();
        return otelContribThread;
    }

    @SuppressWarnings({"unused"})
    private static @NotNull Map<String, String> createOtelContribContainerProperties() {
        final Integer otelContribGrpcPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4317); // GRPC port 4317
        final Integer otelContribHttpPort = CONTAINER_OTEL_CONTRIB.get().getMappedPort(4318); // HTTP port 4318
        final Integer grafanaGrpcPort = CONTAINER_GRAFANA_LGTM.get().getMappedPort(4317); // GRPC port 4317
        final Integer grafanaHttpPort = CONTAINER_GRAFANA_LGTM.get().getMappedPort(4318); // HTTP port 4318

        /// [org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsProperties] (since 3.0.0) - HTTP (Micrometer limitation, no GRPC support yet)
        /// [org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingProperties]        (since 3.4.0) - HTTP or GRPC
        /// [org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingProperties]        (since 3.4.0) - HTTP or GRPC

        final Map<String, String> otelContribDynamicProperties = new LinkedHashMap<>();
        otelContribDynamicProperties.put("management.otlp.metrics.export.step", "2s"); // default 1m is too slow for tests; make it faster for tests
        otelContribDynamicProperties.put("management.otlp.metrics.export.url", "http://localhost:" + grafanaHttpPort + "/v1/metrics");
        switch (CONTAINER_OTEL_CONTRIB_PREFERRED_TRANSPORT) {
            case HTTP:
                otelContribDynamicProperties.put("management.otlp.logging.transport", "HTTP");
                otelContribDynamicProperties.put("management.otlp.logging.endpoint", "http://localhost:" + grafanaHttpPort + "/v1/logs");
                otelContribDynamicProperties.put("management.otlp.tracing.transport", "HTTP");
                otelContribDynamicProperties.put("management.otlp.tracing.endpoint", "http://localhost:" + grafanaHttpPort + "/v1/traces");
                break;
            case GRPC:
                otelContribDynamicProperties.put("management.otlp.logging.transport", "GRPC");
                otelContribDynamicProperties.put("management.otlp.logging.endpoint", "http://localhost:" + grafanaGrpcPort);
                otelContribDynamicProperties.put("management.otlp.tracing.transport", "GRPC");
                otelContribDynamicProperties.put("management.otlp.tracing.endpoint", "http://localhost:" + grafanaGrpcPort);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transport " + CONTAINER_OTEL_CONTRIB_PREFERRED_TRANSPORT);
        }
        return otelContribDynamicProperties;
    }
}
