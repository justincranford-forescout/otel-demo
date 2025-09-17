package com.justincranford.oteldemo.zzzlast;

import com.justincranford.oteldemo.AbstractIT;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import static com.justincranford.oteldemo.actuator.Constants.ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.containers.ContainerUtil.printUrlsWithMappedPorts;
import static com.justincranford.oteldemo.util.HttpUtil.doHttpGet;
import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;
import static com.justincranford.oteldemo.util.SleepUtil.waitMillis;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_COUNTER_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_GAUGE_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_HELLO_ENDPOINT;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_LIVE_DEMO_CHECK;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MAX_MILLIS_WAIT_LOGS;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_COUNTER_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_GAUGE_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_HELLO_ENDPOINT;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_LIVE_DEMO_CHECK;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_MILLIS_WAIT_LOGS;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_SAMPLES_WAIT_ACTUATOR_ENDPOINTS;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_SAMPLES_WAIT_COUNTER_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_SAMPLES_WAIT_GAUGE_METRIC;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_SAMPLES_WAIT_HELLO_ENDPOINT;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_SAMPLES_WAIT_HELLO_TELEMETRY_ENDPOINT;
import static com.justincranford.oteldemo.zzzlast.GenerateTelemetryDataIT.Constants.MIN_SAMPLES_WAIT_HISTOGRAM_GAUGE_METRIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GenerateTelemetryDataIT extends AbstractIT {
    private static final LinkedBlockingDeque<Thread> STARTED_THREADS = new LinkedBlockingDeque<>();
    private static final AtomicBoolean SIGNAL_THREADS_TO_STOP = new AtomicBoolean(false);

    private long multiplier;

    @BeforeEach
    void beforeEach() {
        this.multiplier = GenerateTelemetryDataIT.super.livedemo() ? 1000L : 1L;  // Generate logs and metrics 1000x slower when livedemo=true
    }

    @AfterAll
    static void afterAll() {
        log.info("Signaling threads to stop");
        SIGNAL_THREADS_TO_STOP.set(true);
        for (final Thread thread : STARTED_THREADS) {
            try {
                log.debug("Cleaning up thread {}", thread.getName());
                thread.join(200L);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while joining thread {}", thread.getName(), e);
            }
        }
    }

    @Order(Integer.MIN_VALUE)
    @Test
    void printUrls() {
        assertDoesNotThrow(() -> printUrlsWithMappedPorts(GenerateTelemetryDataIT.super.baseUrl()));
    }

    @Order(Integer.MAX_VALUE)
    @Test
    void liveDemo() {
        log.info("livedemo: {}", GenerateTelemetryDataIT.super.livedemo());
        if (GenerateTelemetryDataIT.super.livedemo()) {
            doIndefiniteLoop(currentIteration -> {}, multiplier * MIN_MILLIS_WAIT_LIVE_DEMO_CHECK, multiplier * MAX_MILLIS_WAIT_LIVE_DEMO_CHECK, "Livedemo");
        }
    }

    /**
     * Generate custom logs.
     * They can be published by an OTLP exporter, scraped from container STDOUT/STDERR, or both.
     */
    @Nested
    class Logs {
        @Order(1)
        @Test
        void event() {
            log.info("Starting Logs.event thread...");
            doIndefiniteLoopInDaemonThread(currentIteration -> log.info("Log message {}", currentIteration), multiplier * MIN_MILLIS_WAIT_LOGS, multiplier * MAX_MILLIS_WAIT_LOGS, "logging");
        }
    }

    /**
     * Generate custom metrics of different types: counter, gauge, and histogram.
     * They can be published by an OTLP exporter, scraped from the /actuator/prometheus endpoint, or both.
     */
    @Nested
    class Metrics {
        private final Counter fakeEventCounter = Counter.builder(telemetryConfigurationProperties().getCounter().getName())
            .description(telemetryConfigurationProperties().getCounter().getDescription())
            .baseUnit(telemetryConfigurationProperties().getCounter().getBaseUnit())
            .tags("terminator", "t1000")
            .register(GenerateTelemetryDataIT.super.meterRegistry());

        final ToDoubleFunction<Metrics> fakeTemperatureMeasurement = obj -> SECURE_RANDOM.nextInt(0, 100);
        final Gauge gauge = Gauge.builder(telemetryConfigurationProperties().getGauge().getName(), this, fakeTemperatureMeasurement)
            .description(telemetryConfigurationProperties().getGauge().getDescription())
            .baseUnit(telemetryConfigurationProperties().getGauge().getBaseUnit())
            .tags("droid", "c3p0")
            .register(GenerateTelemetryDataIT.super.meterRegistry());

        final DistributionSummary fakeDurationHistogram = DistributionSummary.builder(telemetryConfigurationProperties().getHistogram().getName())
            .description(telemetryConfigurationProperties().getHistogram().getDescription())
            .baseUnit(telemetryConfigurationProperties().getHistogram().getBaseUnit())
            .tags("droid", "r2d2")
            .minimumExpectedValue(0.1)
            .maximumExpectedValue(30000.0)
            .serviceLevelObjectives(telemetryConfigurationProperties().getHistogram().getServiceLevelObjectives().stream().mapToDouble(Double::doubleValue).toArray())
            .publishPercentileHistogram() // percentiles not enabled by default
            .publishPercentiles(telemetryConfigurationProperties().getHistogram().getPercentiles().stream().mapToDouble(Double::doubleValue).toArray())
            .percentilePrecision(telemetryConfigurationProperties().getHistogram().getPercentilePrecision())
            .distributionStatisticExpiry(telemetryConfigurationProperties().getHistogram().getDistributionStatisticExpiry())
            .register(GenerateTelemetryDataIT.super.meterRegistry());

        @Order(1)
        @Test
        void counter() {
            log.info("Starting Metrics.counter thread...");
            final LongConsumer recordFakeEvent = currentIteration -> fakeEventCounter.increment();
            doIndefiniteLoopInDaemonThread(recordFakeEvent, multiplier * MIN_MILLIS_WAIT_COUNTER_METRIC, multiplier * MAX_MILLIS_WAIT_COUNTER_METRIC, "increment fake event counter");
            logPrometheusMetrics(MIN_SAMPLES_WAIT_COUNTER_METRIC * multiplier * MAX_MILLIS_WAIT_COUNTER_METRIC, telemetryConfigurationProperties().getCounter().getName());
        }

        @Order(1)
        @Test
        void gauge() {
            log.info("Starting Metrics.gauge thread...");
            final LongConsumer measureFakeTemperature = currentIteration -> gauge.measure();
            doIndefiniteLoopInDaemonThread(measureFakeTemperature, multiplier * MIN_MILLIS_WAIT_GAUGE_METRIC, multiplier * MAX_MILLIS_WAIT_GAUGE_METRIC, "measure fake temperature");
            logPrometheusMetrics(MIN_SAMPLES_WAIT_GAUGE_METRIC * multiplier * MAX_MILLIS_WAIT_GAUGE_METRIC, telemetryConfigurationProperties().getGauge().getName());
        }

        @Order(1)
        @Test
        void histogram() {
            log.info("Starting Metrics.histogram thread...");
            final LongConsumer recordFakeDuration = currentIteration -> fakeDurationHistogram.record(SECURE_RANDOM.nextDouble(0.1, 200.0));
            doIndefiniteLoopInDaemonThread(recordFakeDuration, multiplier * MIN_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC, multiplier * MAX_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC, "record duration histogram");
            logPrometheusMetrics(MIN_SAMPLES_WAIT_HISTOGRAM_GAUGE_METRIC * multiplier * MAX_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC, telemetryConfigurationProperties().getHistogram().getName());
        }
    }

    /**
     * Generate traces by calling /actuator/* endpoints, /hello endpoint, and /hello/telemetry endpoint.
     * Traces can only be published by an OTLP exporter; no standard exists to support scraping traces from an endpoint.
     * <P>
     * Traces generate metrics too (e.g. HTTP trace-metrics); they are published in the /actuator/prometheus endpoint.
     * Trace-Metrics can be published by an OTLP exporter, scraped from the /actuator/prometheus endpoint, or both.
     */
    @Nested
    class Traces {
        private static Stream<String> actuatorEndpoints() {
            return ACTUATOR_ENDPOINTS.stream();
        }

        @Order(1)
        @ParameterizedTest
        @MethodSource("actuatorEndpoints")
        void httpActuator(final String actuatorEndpoint) {
            log.info("Starting Traces.httpActuator thread for actuatorEndpoint: {}", actuatorEndpoint);
            final String url = GenerateTelemetryDataIT.super.baseUrl() + actuatorEndpoint;
            final RestTemplate restTemplate = new RestTemplateBuilder().build();

            restTemplate.getForEntity(url, String.class); // WARM UP: Controller, Service, and Repository instances might be lazy initialized by Spring Boot
            doIndefiniteLoopInDaemonThread(currentIteration -> restTemplate.getForEntity(url, String.class),multiplier * MIN_MILLIS_WAIT_ACTUATOR_ENDPOINTS, multiplier * MAX_MILLIS_WAIT_ACTUATOR_ENDPOINTS,"get " + url);
            logPrometheusMetrics(MIN_SAMPLES_WAIT_ACTUATOR_ENDPOINTS * multiplier * MAX_MILLIS_WAIT_ACTUATOR_ENDPOINTS,"http_server_requests", actuatorEndpoint);
        }

        @Order(1)
        @Test
        void httpHello() {
            log.info("Starting Traces.httpHello thread...");
            final String url = GenerateTelemetryDataIT.super.baseUrl() + "/hello";
            final RestTemplate restTemplate = new RestTemplateBuilder().build();
            restTemplate.getForEntity(url, String.class); // WARM UP: Controller, Service, and Repository instances might be lazy initialized by Spring Boot
            doIndefiniteLoopInDaemonThread(currentIteration -> restTemplate.getForEntity(url, String.class), multiplier * MIN_MILLIS_WAIT_HELLO_ENDPOINT, multiplier * MAX_MILLIS_WAIT_HELLO_ENDPOINT, "get " + url);
            logPrometheusMetrics(MIN_SAMPLES_WAIT_HELLO_ENDPOINT * multiplier * MAX_MILLIS_WAIT_HELLO_ENDPOINT, "http_server_requests", "/hello");
        }

        @Order(1)
        @Test
        void httpHelloTelemetry() {
            log.info("Starting Traces.httpHelloTelemetry thread...");
            final String url = GenerateTelemetryDataIT.super.baseUrl() + "/hello/telemetry";
            final RestTemplate restTemplate = new RestTemplateBuilder().build();
            restTemplate.getForEntity(url, String.class); // WARM UP: Controller, Service, and Repository instances might be lazy initialized by Spring Boot
            doIndefiniteLoopInDaemonThread(currentIteration -> restTemplate.getForEntity(url, String.class), multiplier * MIN_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT, multiplier * MAX_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT, "get " + url);
            logPrometheusMetrics(MIN_SAMPLES_WAIT_HELLO_TELEMETRY_ENDPOINT * multiplier * MAX_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT, "http_server_requests", "/hello/telemetry");
        }
    }

    private static void doIndefiniteLoopInDaemonThread(final LongConsumer loopBody, final long minMillis, final long maxMillis, final String taskName) {
        assertThat(loopBody).isNotNull();
        assertThat(minMillis).isGreaterThanOrEqualTo(1);
        assertThat(maxMillis).isGreaterThan(minMillis); // can't be same or lower than minMillis
        assertThat(taskName).isNotBlank();

        final Thread logThread = new Thread(() -> doIndefiniteLoop(loopBody, minMillis, maxMillis, taskName));
        logThread.setName("doLoopInThread " + taskName);
        logThread.setDaemon(true);
        logThread.start();
        STARTED_THREADS.add(logThread);
    }

    private static void doIndefiniteLoop(LongConsumer loopBody, long minMillis, long maxMillis, String thing) {
        for (long currentIteration = 1; currentIteration < Long.MAX_VALUE; currentIteration++) {
            if (SIGNAL_THREADS_TO_STOP.get()) {
                log.info("{} stopped, iterations {}", thing, currentIteration - 1);
                break;
            }
            log.info("{} iteration {}", thing, currentIteration);
            loopBody.accept(currentIteration);
            waitMillis(thing + " " + currentIteration, SECURE_RANDOM.nextLong(minMillis, maxMillis));
        }
    }

    // Do prometheus logging 1) Synchronously if livedemo=false or, 2) Asynchronously if livedemo=true
    private void logPrometheusMetrics(final long waitMillis, final String... stringFilters) {
        final Runnable logPrometheusMetricsFunction = () -> {
            final long now = System.nanoTime();
            waitMillis("", waitMillis);
            log.info("Waited {} ms before querying /actuator/prometheus", (System.nanoTime() - now) / 1_000_000F);

            final String prometheusResponse = doHttpGet(GenerateTelemetryDataIT.super.baseUrl() + "/actuator/prometheus");
            assertThat(prometheusResponse).isNotBlank();

            if (stringFilters == null || stringFilters.length == 0) {
                log.info("Unfiltered Prometheus response:\n{}", prometheusResponse);
                return;
            }

            for (final String stringFilter : stringFilters) {
                assertThat(prometheusResponse).contains(stringFilter);
            }

            final String filteredResponse = Arrays.stream(prometheusResponse.split("\n"))
                .filter(line -> Arrays.stream(stringFilters).allMatch(line::contains))
                .reduce((line1, line2) -> line1 + "\n" + line2)
                .orElse("No matching lines found for filters: " + Arrays.toString(stringFilters));

            log.info("Filtered Prometheus response {}:\n{}", Arrays.toString(stringFilters), filteredResponse);
        };
        if (GenerateTelemetryDataIT.super.livedemo()) {
            // do prometheus logging asynchronous, don't block other tests from starting their own threads
            final Thread t = new Thread(logPrometheusMetricsFunction);
            t.setName("logPrometheusMetrics " + Arrays.toString(stringFilters));
            t.setDaemon(true);
            t.start();
            STARTED_THREADS.add(t);
        } else {
            // do prometheus logging synchronously, block until done
            logPrometheusMetricsFunction.run();
        }
    }

    static class Constants {
        static final long MIN_MILLIS_WAIT_LIVE_DEMO_CHECK = 1000L;
        static final long MAX_MILLIS_WAIT_LIVE_DEMO_CHECK = 2000L;

        static final long MIN_MILLIS_WAIT_LOGS = 3L;
        static final long MAX_MILLIS_WAIT_LOGS = 5L;

        static final long MIN_MILLIS_WAIT_COUNTER_METRIC = 2L;
        static final long MAX_MILLIS_WAIT_COUNTER_METRIC = 5L;
        static final long MIN_SAMPLES_WAIT_COUNTER_METRIC = 3L;

        static final long MIN_MILLIS_WAIT_GAUGE_METRIC = 2L;
        static final long MAX_MILLIS_WAIT_GAUGE_METRIC = 5L;
        static final long MIN_SAMPLES_WAIT_GAUGE_METRIC = 3L;

        static final long MIN_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC = 1L;
        static final long MAX_MILLIS_WAIT_HISTOGRAM_GAUGE_METRIC = 4L;
        static final long MIN_SAMPLES_WAIT_HISTOGRAM_GAUGE_METRIC = 100L;

        static final long MIN_MILLIS_WAIT_ACTUATOR_ENDPOINTS = 8L;
        static final long MAX_MILLIS_WAIT_ACTUATOR_ENDPOINTS = 10L;
        static final long MIN_SAMPLES_WAIT_ACTUATOR_ENDPOINTS = 3L;

        static final long MIN_MILLIS_WAIT_HELLO_ENDPOINT = 5L;
        static final long MAX_MILLIS_WAIT_HELLO_ENDPOINT = 8L;
        static final long MIN_SAMPLES_WAIT_HELLO_ENDPOINT = 3L;

        static final long MIN_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT = 5L;
        static final long MAX_MILLIS_WAIT_HELLO_TELEMETRY_ENDPOINT = 8L;
        static final long MIN_SAMPLES_WAIT_HELLO_TELEMETRY_ENDPOINT = 3L;
    }
}
