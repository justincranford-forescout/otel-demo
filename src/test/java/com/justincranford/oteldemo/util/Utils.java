package com.justincranford.oteldemo.util;

import com.justincranford.oteldemo.containers.ContainerManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.justincranford.oteldemo.Constants.ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_OTEL_CONTRIB;
import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_PORTS_GRAFANA_LGTM;
import static com.justincranford.oteldemo.containers.ContainerManager.CONTAINER_PORTS_OTEL_CONTRIB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
@Slf4j
public class Utils {
    public static @NotNull String prefixAllLines(final String prefix, final String content) {
        return prefix + ">>>" + content.replaceAll("(\r)?\n(\r)?", "$1\n$2" + prefix+ ">>> ");
    }

    public static String readFileContents(String otelContribYamlFilePath, final Charset charset) {
        try {
            return Files.readString(Path.of(otelContribYamlFilePath), charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeFileContents(String otelContribYamlFileUpdatedContents) {
        try {
            final Path tempOtelConfig = Files.createTempFile("otel-config-", ".yaml");
            Files.writeString(tempOtelConfig, otelContribYamlFileUpdatedContents, StandardCharsets.UTF_8);
            return tempOtelConfig.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String pollHttpGet(final String logPrefix, final String url, final String stopString, final long totalWaitMillis, final long incrementalWaitMillis) {
        String latestResponse = "";
        final long endTimeMillisOtel = System.currentTimeMillis() + totalWaitMillis;
        do {
            try {
                latestResponse = doHttpGet(url);
                log.info("Response:\n{}", prefixAllLines(logPrefix, latestResponse));
            } catch(ResourceAccessException e) {
                log.warn("Error: {}", e.getMessage());
                wait_(incrementalWaitMillis);
            }
        } while ((!latestResponse.contains(stopString)) && (System.currentTimeMillis() < endTimeMillisOtel));
        return latestResponse;
    }

    public static String doHttpGet(final String url) {
        log.info("Getting URL {}", url);
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        final ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        final HttpStatusCode statusCode = responseEntity.getStatusCode();
        final String responseBody = responseEntity.getBody();
        assertEquals(HttpStatus.OK, statusCode);
        assertNotNull(responseBody);
        return responseBody;
    }

    public static void wait_(final long incrementalWaitMillis)  {
        try {
            Thread.sleep(incrementalWaitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            log.warn("Interrupted while waiting for {} milliseconds", incrementalWaitMillis);
        }
    }

    public static void printUrlsWithMappedPorts(final String baseUrl) {
        final List<String> actuator = new java.util.ArrayList<>();
        for (final String path : ACTUATOR_ENDPOINTS) {
            actuator.add(String.format("%s%s", baseUrl, path));
        }
        log.info("spring-boot-actuator URLs:\n{}\n", String.join("\n", actuator));

        final List<String> otel = new java.util.ArrayList<>();
        for (final Integer port : CONTAINER_PORTS_OTEL_CONTRIB) {
            otel.add(String.format("%d => http://localhost:%d/", port, CONTAINER_OTEL_CONTRIB.get().getMappedPort(port)));
        }
        log.info("otel-contrib URLs:\n{}\n", String.join("\n", otel));

        final List<String> grafana = new java.util.ArrayList<>();
        for (final Integer port : CONTAINER_PORTS_GRAFANA_LGTM) {
            grafana.add(String.format("%d => http://localhost:%d/", port, ContainerManager.CONTAINER_GRAFANA_LGTM.get().getMappedPort(port)));
        }
        log.info("grafana-lgtm URLs:\n{}\n", String.join("\n", grafana));
    }
}
