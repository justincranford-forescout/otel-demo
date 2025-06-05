package com.justincranford.oteldemo.entity.base;

import com.github.f4b6a3.uuid.alt.GUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.time.Clock;
import java.time.Instant;

import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;

public class IdGeneratorUUIDv7 extends SequenceStyleGenerator {
    private static final Clock UTC_CLOCK = Clock.systemUTC();

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return GUID.v7(Instant.now(UTC_CLOCK), SECURE_RANDOM);
    }
}
