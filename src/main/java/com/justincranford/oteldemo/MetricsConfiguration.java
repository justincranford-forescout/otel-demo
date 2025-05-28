package com.justincranford.oteldemo;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> additionalTagsForMetricsOnly() {
        return registry -> registry.config().commonTags("foo", "bar", "test1", "value1");
    }
}
