package com.justincranford.oteldemo.entity.base;

import com.github.f4b6a3.uuid.alt.GUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;

public class IdGeneratorUUIDv7 extends SequenceStyleGenerator {
    @Override
    public UUID generate(SharedSessionContractImplementor session, Object object) {
        final Instant nowUTC = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        return GUID.v7(nowUTC, SECURE_RANDOM).toUUID();
    }
}
