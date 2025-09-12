package com.justincranford.oteldemo.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
@SuppressWarnings({"unused"})
public abstract class AbstractEntity {
    // UUIDv7 is monotonic increasing (i.e. time prefix), similar to Integer-based AUTO_INCREMENT
    //        Table rows inserts are always appended to the end of the table (no b-tree page splits), but...
    // UUID4: Index rows inserts are always random over the whole index range (frequent b-tree page splits)
    // UUID7: Index rows inserts are always appended to the end of the index (no b-tree page splits); exactly the same append order as table rows!
    @Id
    @IdGeneratorTypeUUIDv7
    @Column(nullable=false,updatable=false,columnDefinition="uuid")
    private java.util.UUID id;

    // Optimistic locking: UPDATE ... WHERE ... AND version = ?
    // PreparedStatement.executeUpdate rc: 1=success (version didn't change since read), 0=failure (version changed since read; throw OptimisticLockException)
    @Version
    @Column(nullable=false)
    private Integer version;
}
