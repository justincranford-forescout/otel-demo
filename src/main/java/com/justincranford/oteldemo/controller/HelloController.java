package com.justincranford.oteldemo.controller;

import com.justincranford.oteldemo.repository.TemperatureRepository;
import com.justincranford.oteldemo.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.SleepUtil.waitMillis;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class HelloController {
    private final OpenTelemetry openTelemetry; // Bean managed by Spring Boot lifecycle
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    private final AtomicLong atomicLong = new AtomicLong(0L);
    private LongCounter upLongCounter;
    private LongUpDownCounter upLongDownCounter;
    private Counter upCounter;
    private Observation observation;
    private Gauge gauge;

    private final UserRepository userRepository;
    private final TemperatureRepository temperatureRepository;

    @PostConstruct
    public void postConstruct() {
        final Meter meter = openTelemetry.getMeterProvider().get(HelloController.class.getCanonicalName());

        this.upLongCounter = meter.counterBuilder("HelloController.upLongCounter").setDescription("HelloController upLongCounter").build();
        this.upLongDownCounter = meter.upDownCounterBuilder("HelloController.upLongDownCounter").setDescription("HelloController upLongDownCounter").build();
        this.upCounter = Counter.builder("HelloController.upCounter").description("HelloController.upCounter description").baseUnit("ms").tags("foo", "upCounter").register(this.meterRegistry);
        this.observation = Observation.createNotStarted("HelloController.span", this.observationRegistry).lowCardinalityKeyValue("foo", "observation");
        this.gauge = Gauge.builder("HelloController.gauge", this, obj -> SECURE_RANDOM.nextInt(0, 100)).description("HelloController.gauge example").baseUnit("celsius").register(this.meterRegistry);
    }

    @GetMapping("/hello")
    public String hello() {
        try (final MDC.MDCCloseable ignored = MDC.putCloseable("Hello?", "Is it me you're looking for?")) {
            final long currentCount = atomicLong.incrementAndGet(); // local metric

            this.upLongCounter.add(100); // custom metric
            this.upLongDownCounter.add(SECURE_RANDOM.nextBoolean() ? -1 : 1); // custom metric
            this.upCounter.increment(); // custom metric

            waitMillis("Simulate processing delay before custom trace span", SECURE_RANDOM.nextLong(100, 150));
            this.observation.observe(() -> { // custom trace span
                this.gauge.measure();

                waitMillis("Simulate processing delay during custom trace span", SECURE_RANDOM.nextLong(150, 250));
                log.info("Hello OpenTelemetry {}!", currentCount);
            });
            waitMillis("Simulate processing delay after custom trace span", SECURE_RANDOM.nextLong(50, 100));

            return "Hello OpenTelemetry " + currentCount + "!";
        }
    }
}
