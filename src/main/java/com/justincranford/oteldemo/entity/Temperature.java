package com.justincranford.oteldemo.entity;

import com.justincranford.oteldemo.entity.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

@Entity
@Table(name="temperatures")
@Getter
@Setter
@RequiredArgsConstructor
@Accessors(fluent=true,chain= true)
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@Builder
@AllArgsConstructor
public class Temperature extends AbstractEntity {
    @Column(nullable=false,updatable=false)
    OffsetDateTime timestamp;

    @Column(nullable=false,updatable=false)
    @DecimalMin("-273.15") // absolute zero
    @DecimalMax("15000000.0") // temperature inside the sun
    private Float celcius;
}
