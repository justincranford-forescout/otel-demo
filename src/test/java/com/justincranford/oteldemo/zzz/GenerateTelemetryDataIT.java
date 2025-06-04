package com.justincranford.oteldemo.zzz;

import com.justincranford.oteldemo.AbstractIT;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.function.LongConsumer;

import static com.justincranford.oteldemo.actuator.Constants.ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.containers.ContainerUtil.printUrlsWithMappedPorts;
import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.SleepUtil.waitMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GenerateTelemetryDataIT extends AbstractIT {
    @Order(Integer.MIN_VALUE)
    @Test
    void printUrls() {
        assertDoesNotThrow(() -> printUrlsWithMappedPorts(super.baseUrl()));
    }

    @Test
    void logs() {
        log.info("Starting logs thread...");
        doLoopInThread(currentIteration -> log.info("Log message {}", currentIteration), 1000, 3000, "logging");
    }

    @Test
    void counterMetric() {
        log.info("Starting counterMetric thread...");
        final Counter counter = Counter.builder("example.counter").description("counter example").baseUnit("tasks").tags("a", "1", "b", "2").register(super.meterRegistry());
        doLoopInThread(currentIteration ->  counter.increment(), 500, 1000, "increment counter");
    }

    @Test
    void histogramMetric() {
        log.info("Starting histogramMetric thread...");
        final DistributionSummary histogram = DistributionSummary.builder("example.histogram").description("histogram example").baseUnit("ms").tags("b", "2", "c", "3").register(super.meterRegistry());
        doLoopInThread(currentIteration ->  histogram.record(SECURE_RANDOM.nextDouble(1, 2000)), 500, 1000, "record histogram");
    }

    @Test
    void gaugeMetric() {
        log.info("Starting gaugeMetric thread...");
        final Gauge gauge = Gauge.builder("example.gauge", this, obj -> SECURE_RANDOM.nextInt(0, 100)).description("gauge example").baseUnit("celsius").register(super.meterRegistry());
        doLoopInThread(currentIteration ->  gauge.measure(), 500, 1000, "measure gauge");
    }

    @Test
    void actuatorTraces() {
        log.info("Starting actuatorTraces threads...");
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        for (final String url : ACTUATOR_ENDPOINTS.stream().map(path -> super.baseUrl() + path).toList()) {
            doLoopInThread(currentIteration -> restTemplate.getForEntity(url, String.class), 500 * ACTUATOR_ENDPOINTS.size(), 1000 * ACTUATOR_ENDPOINTS.size(), "get " + url);
        }
    }

    @Test
    void controllerTrace() {
        log.info("Starting controllerTrace thread...");
        final String url = super.baseUrl() + "/hello";
        final RestTemplate restTemplate = new RestTemplateBuilder().build();
        doLoopInThread(currentIteration -> restTemplate.getForEntity(url, String.class), 500, 1000, "get " + url);
    }

    @Order(Integer.MAX_VALUE)
    @Test
    void liveDemo() {
        log.info("livedemo: {}", super.livedemo());
        if (super.livedemo()) {
            doLoop(currentIteration -> {}, 10000, 20000, "Livedemo");
        }
    }

    private static void doLoopInThread(final LongConsumer loopBody, final int minMillis, final int maxMillis, final String thing) {
        assertThat(loopBody).isNotNull();
        assertThat(minMillis).isGreaterThanOrEqualTo(1);
        assertThat(maxMillis).isGreaterThan(minMillis); // can't be same or lower than minMillis
        assertThat(thing).isNotBlank();

        final Thread logThread = new Thread(() -> doLoop(loopBody, minMillis, maxMillis, thing));
        logThread.setName("doLoopInThread " + thing);
        logThread.setDaemon(true);
        logThread.start();
    }

    private static void doLoop(LongConsumer loopBody, int minMillis, int maxMillis, String thing) {
        for (long currentIteration = 1; currentIteration < Long.MAX_VALUE; currentIteration++) {
            log.info("{} iteration {}", thing, currentIteration);
            loopBody.accept(currentIteration);
            waitMillis(thing + " " + currentIteration, SECURE_RANDOM.nextInt(minMillis, maxMillis));
        }
    }
}
