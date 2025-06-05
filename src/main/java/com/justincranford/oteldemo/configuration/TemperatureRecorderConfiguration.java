package com.justincranford.oteldemo.configuration;

import com.justincranford.oteldemo.service.TemperatureService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class TemperatureRecorderConfiguration {
    private final MeterRegistry meterRegistry;
    private final TemperatureService temperatureService;

    private Gauge temperatureMetric;

    @PostConstruct
    public void postConstruct() {
        this.temperatureMetric = Gauge.builder("TemperatureRecorder", this, obj -> measureTemperature()).description("TemperatureRecorder").baseUnit("celsius").register(this.meterRegistry);
    }

    @Scheduled(fixedRate=1000) // milliseconds; From javadoc: "With virtual threads, fixed rates and cron triggers are recommended over fixed delays."
    public void recordTemperature() {
        log.info("Recording temperature...");
        final Iterable<Measurement> temperatureMeasurements = this.temperatureMetric.measure();
        for (final Measurement measurement : temperatureMeasurements) {
            final double value = measurement.getValue();
            temperatureService.saveTemperature((float) value);
        }
        log.info("Recording temperature done");
    }

    private double measureTemperature() {
        return SECURE_RANDOM.nextDouble(0, 100); // this could be replaced with reading temperature from an actual sensor
    }
}
