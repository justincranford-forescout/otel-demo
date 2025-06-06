package com.justincranford.oteldemo.configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

//@Configuration
@RequiredArgsConstructor
public class OpenTelemetryJdbcConfiguration  {
    private final OpenTelemetry openTelemetry; // Bean managed by Spring Boot lifecycle
    @Lazy
    private final DataSource dataSource;

    @Bean
    @Primary
    public DataSource wrappedDatasource() {
        final JdbcTelemetry jdbcTelemetry = JdbcTelemetry.builder(openTelemetry)
            .setStatementSanitizationEnabled(true)
            .setCaptureQueryParameters(true)
            .setTransactionInstrumenterEnabled(true)
            .build();

        DataSource wrapped = jdbcTelemetry.wrap(dataSource);
        return wrapped;
    }
}
