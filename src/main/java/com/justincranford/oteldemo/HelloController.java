package com.justincranford.oteldemo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HelloController {
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

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
        return this.observation.observe(() -> "Hello OpenTelemetry!");
    }
}
