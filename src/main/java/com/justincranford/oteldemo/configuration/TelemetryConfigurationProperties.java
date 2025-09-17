package com.justincranford.oteldemo.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "test.telemetry")
@Data
public class TelemetryConfigurationProperties {

    private Counter counter = new Counter();
    private Gauge gauge = new Gauge();
    private Histogram histogram = new Histogram();

    @Data
    public static class Counter {
        @NotBlank
        private String name = "fake_event";
        @NotBlank
        private String description = "fake event counter";
        @NotBlank
        private String baseUnit = "tasks";
    }

    @Data
    public static class Gauge {
        @NotBlank
        private String name = "fake_temperature";
        @NotBlank
        private String description = "fake temperature gauge";
        @NotBlank
        private String baseUnit = "celsius";
    }

    @Data
    public static class Histogram {
        @NotBlank
        private String name = "fake_duration";
        @NotBlank
        private String description = "fake duration histogram";
        @NotBlank
        private String baseUnit = "ms";
        @NotEmpty
        private List<Double> serviceLevelObjectives = List.of(10.0, 50.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0);
        @NotEmpty
        private List<Double> percentiles = List.of(0.500, 0.950, 0.990, 0.995);
        @Min(2)
        private int percentilePrecision = 5;
        @NotNull
        private Duration distributionStatisticExpiry = Duration.ofMinutes(10);
    }
}
