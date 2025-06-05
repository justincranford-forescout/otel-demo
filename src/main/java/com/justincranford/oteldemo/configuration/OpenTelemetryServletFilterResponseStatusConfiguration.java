package com.justincranford.oteldemo.configuration;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.Filter;

import java.io.IOException;

@Configuration
public class OpenTelemetryServletFilterResponseStatusConfiguration {
    @Bean
    public Filter otelStatusFilter() {
        return new OtelStatusFilter();
    }

    public static class OtelStatusFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            try {
                chain.doFilter(request, response);
            } finally {
                final int status = ((HttpServletResponse) response).getStatus();
                final Span span = Span.current();
                if (status >= 200 && status < 300) {
                    span.setStatus(StatusCode.OK);
                } else {
                    span.setStatus(StatusCode.ERROR);
                }
            }
        }
    }}
