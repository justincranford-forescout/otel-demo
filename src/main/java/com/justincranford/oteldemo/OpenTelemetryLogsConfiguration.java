package com.justincranford.oteldemo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * <pre>
 * There be dragons! Spring Boot autoconfigures metrics, tracing, but not logging.
 *
 * TL;DR: Do these steps to enable logging:
 *  1. Add OpenTelemetry Collector URL in application.properties
 *  2. Add OpenTelemetry Logback appender in build.gradle / pom.xml dependencies
 *  3. Add OpenTelemetry Logback appender in spring-logback.xml
 *  4. Inject OpenTelemetrySDK bean into OpenTelemetryAppender (i.e. static)
 * </pre>
 * ============================================================================
 * The following is copied from SB 3.5 docs to here, in case it changes or moves:
 * <P>
 * <a href="https://docs.spring.io/spring-boot/reference/actuator/loggers.html#actuator.loggers.opentelemetry">https://docs.spring.io/spring-boot/reference/actuator/loggers.html#actuator.loggers.opentelemetry</a>:
 * ============================================================================
 * <P>
 * By default, logging via OpenTelemetry is not configured. You have to provide the location of the OpenTelemetry logs endpoint to configure it:
 * <pre>
 *     management.otlp.logging.endpoint=https://otlp.example.com:4318/v1/logs
 * </pre>
 * Note
 * <P>
 * The OpenTelemetry Logback appender and Log4j appender are not part of Spring Boot.
 * For more details, see the <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library">OpenTelemetry Logback appender</a>
 * or the <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/log4j/log4j-appender-2.17/library">OpenTelemetry Log4j2 appender</a>
 * in the <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation">OpenTelemetry Java instrumentation GitHub repository</a>.
 * <P>
 * TIP
 * <P>
 * You have to configure the appender in your logback-spring.xml or log4j2-spring.xml configuration to get OpenTelemetry logging working.
 * <P>
 * The OpenTelemetryAppender for both Logback and Log4j requires access to an OpenTelemetry instance to function properly. This instance must be set programmatically during application startup, which can be done like this:
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
class OpenTelemetryLogsConfiguration implements InitializingBean {
    private final OpenTelemetry openTelemetry; // Bean managed by Spring Boot lifecycle

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry); // static OpenTelemetryAppender is not managed by Spring Boot lifecycle
    }
}
