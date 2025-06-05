package com.justincranford.oteldemo.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryMetricsConfiguration {
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> additionalTagsForMetricsOnly() {
        return registry -> registry.config().commonTags("foo", "OpenTelemetryTracesConfiguration", "bar", "2");
    }
}
