package com.justincranford.oteldemo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    public static final List<String> ACTUATOR_ENDPOINTS = List.of(
        "/actuator",
        "/actuator/health",
        "/actuator/metrics",
        "/actuator/prometheus",
        "/actuator/info",
        "/actuator/env",
        "/actuator/beans",
        "/actuator/mappings",
        "/actuator/loggers",
        "/actuator/threaddump",
        "/actuator/caches"
    );
}
