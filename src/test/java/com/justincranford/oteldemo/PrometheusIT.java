package com.justincranford.oteldemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class PrometheusIT extends AbstractIT {
    @Test
    void testPrometheus() {
        log.info("Prometheus response:\n{}", doHttpGet(super.baseUrl() + "/actuator/prometheus"));
    }
}
