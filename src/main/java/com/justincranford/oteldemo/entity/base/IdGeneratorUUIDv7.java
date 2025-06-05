package com.justincranford.oteldemo.entity.base;

import com.github.f4b6a3.uuid.alt.GUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.justincranford.oteldemo.util.SecureRandomUtil.SECURE_RANDOM;

/**
 * UUIDv7 ID generator for Hibernate.
 * UUIDv7 is a time-based UUID prefixed by a 48-bit timestamp, so it is monotonic increasing like an auto-incremented counter.
 * UUIDv7 also contains 76-bits of randomness to avoid collisions; that is 12-bits more than a 64-bit unsigned integer.
 * UUIDv7 is ideal for distributed systems where you want unique identifiers that can be generated independently on different nodes,
 * while still maintaining monotonic increasing order and time-based order; this makes insertion-order DBs like PostgreSQL
 * behave like cluster-order DBs like SQL Server, mitigating fragmentation after doing VACUUM followed by INSERT.
 */
public class IdGeneratorUUIDv7 extends SequenceStyleGenerator {
    @Override
    public UUID generate(SharedSessionContractImplementor session, Object object) {
        final Instant nowUTC = OffsetDateTime.now(ZoneOffset.UTC).toInstant();
        return GUID.v7(nowUTC, SECURE_RANDOM).toUUID();
    }
}
