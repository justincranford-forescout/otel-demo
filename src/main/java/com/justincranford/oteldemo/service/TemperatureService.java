package com.justincranford.oteldemo.service;

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

    public void saveTemperatures(final List<Float> temperatures) {
        for (final Float value : temperatures) {
            saveTemperature(value);
        }
        log.info("Saved {} temperatures: {}", temperatures.size(), temperatures);
    }

    public void saveTemperature(final float value) {
        final Temperature temperature = temperatureRepository.save(Temperature.builder()
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .celcius(value)
            .build());
        log.info("Saved temperature: {}", temperature);
    }
}
