package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@TestConfiguration
@Slf4j
class TestcontainersConfiguration {
    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("otel/opentelemetry-collector:latest");

    @Bean
    public GenericContainer<?> otelContainer() throws IOException {
        final GenericContainer<?> otelCContainer = createOtelCContainer();
        otelCContainer.start();
        final Integer mappedGrpcPort = otelCContainer.getMappedPort(4317);
        final Integer mappedHttpPort = otelCContainer.getMappedPort(4318);
        log.info("OpenTelemetry mapped ports, gRPC: {}, HTTP: {}", mappedGrpcPort, mappedHttpPort);
        return otelCContainer;
    }

    GenericContainer<?> createOtelCContainer() throws IOException {
        return new GenericContainer<>(DOCKER_IMAGE_NAME)
                .withExposedPorts(4317, 4318) // 4317 gRPC, 4318 HTTP
                .withFileSystemBind(createOtelConfigFile().toString(), "/etc/otel/config.yaml", BindMode.READ_ONLY)
                .withNetworkAliases("otel-collector")
                .withNetworkMode("host")
                .withCommand("--config=/etc/otel/config.yaml");
    }

    private static @NotNull Path createOtelConfigFile() throws IOException {
        final Path tempOtelConfigPath = Files.createTempFile("otel-config", ".yaml").toAbsolutePath();
        try (BufferedWriter writer = Files.newBufferedWriter(tempOtelConfigPath)) {
            writer.write("""
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
processors:
  batch:

exporters:
  debug:
    verbosity: detailed

extensions:
  health_check:
  pprof:
  zpages:

service:
  extensions: [health_check, pprof, zpages]
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
                    """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp otel-config.yaml", e);
        }
        return tempOtelConfigPath;
    }
}
