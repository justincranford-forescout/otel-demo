package com.justincranford.oteldemo.configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OpenTelemetryJdbcConfiguration  {
    private final DataSource dataSource;

    @Component
    @RequiredArgsConstructor
    public static class MyBeanPostProcessor implements BeanPostProcessor {
        private final OpenTelemetry openTelemetry; // Bean managed by Spring Boot lifecycle

        @Override
        public Object postProcessAfterInitialization(@NonNull final Object bean, @NonNull final String beanName) throws BeansException {
            if (bean instanceof DataSource dataSource) {
                final JdbcTelemetry jdbcTelemetry = JdbcTelemetry.builder(this.openTelemetry)
                    .setStatementSanitizationEnabled(true)
                    .setCaptureQueryParameters(true)
                    .setTransactionInstrumenterEnabled(true)
                    .build();
                final DataSource wrappedDataSource = jdbcTelemetry.wrap(dataSource);
                // Bean: dataSource, Plain: com.zaxxer.hikari.HikariDataSource, Wrapped: io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
                log.info("Bean: {}, Plain: {}, Wrapped: {}", beanName, bean.getClass().getName(), wrappedDataSource.getClass().getName());
                return wrappedDataSource;
            }
            return bean;
        }
    }
}
