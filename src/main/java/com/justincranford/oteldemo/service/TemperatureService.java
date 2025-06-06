package com.justincranford.oteldemo.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import com.justincranford.oteldemo.entity.Temperature;
import com.justincranford.oteldemo.repository.TemperatureRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TemperatureService {
    private final TemperatureRepository temperatureRepository;

    public void saveManyTemperatures(final List<Float> values) {
        final List<Temperature> temperatures = values.stream().map(this::saveTemperature).toList();
        log.trace("Saved {} temperatures: {}", temperatures.size(), temperatures);
    }

    public void saveOneTemperature(final float value) {
        final Temperature temperature = saveTemperature(value);
        log.trace("Saved temperature: {}", temperature);
    }

    @WithSpan
    private Temperature saveTemperature(final float value) {
        return temperatureRepository.save(
            Temperature.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .celcius(value)
                .build()
        );
    }
}
