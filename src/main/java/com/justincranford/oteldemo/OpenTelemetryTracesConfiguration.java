package com.justincranford.oteldemo;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryTracesConfiguration {
    @Bean
    public SpanProcessor customSpanProcessor() {
        return new SpanProcessor() {
            @Override
            public boolean isStartRequired() {
                return false; // this SpanProcessor doesn't require start events
            }

            @Override
            public boolean isEndRequired() {
                return false; // this SpanProcessor doesn't require end events
            }

            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                span.setAttribute("foo", "OpenTelemetryTracesConfiguration");
                span.setAttribute("bar", "1");
            }

            @Override
            public void onEnd(ReadableSpan span) {
                // do nothing
            }
        };
    }
}
