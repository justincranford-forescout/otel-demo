package com.justincranford.oteldemo.controller;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class OtelStatusInterceptor implements HandlerInterceptor {
    @SuppressWarnings({"unused"})
    @Override
    public void postHandle(@NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response, @NonNull final Object handler, final @Nullable ModelAndView modelAndView) {
        final Span span = Span.current();
        final int status = response.getStatus();
        if (status >= 200 && status < 300) {
            span.setStatus(StatusCode.OK);
        } else {
            span.setStatus(StatusCode.ERROR);
        }
    }
}
