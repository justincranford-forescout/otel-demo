package com.justincranford.oteldemo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static java.lang.Thread.sleep;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class HelloController {
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    private final AtomicLong counter = new AtomicLong(0L);
    private Counter helloCounter;
    private Observation observation;

    @PostConstruct
    public void postConstruct() {
        this.helloCounter = Counter.builder("hello")
                .description("hello counter")
                .tags("env", "example")
                .register(this.meterRegistry);

        this.observation = Observation.createNotStarted("hello", this.observationRegistry)
                .lowCardinalityKeyValue("env", "example");
    }

    @GetMapping("/hello")
    public String hello() {
        this.helloCounter.increment();
        return this.observation.observe(() -> {
            try {
                sleep(SECURE_RANDOM.nextLong(250, 750)); // Simulate some processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
            final long currentCount = counter.incrementAndGet();
            log.info("Hello OpenTelemetry {}!", currentCount);
            return "Hello OpenTelemetry " + currentCount + "!";
        });
    }
}
