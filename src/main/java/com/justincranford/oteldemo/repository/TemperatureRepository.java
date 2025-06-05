package com.justincranford.oteldemo.repository;

import com.justincranford.oteldemo.entity.Temperature;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings({"unused"})
public interface TemperatureRepository extends CrudRepository<Temperature, Long> {
}
