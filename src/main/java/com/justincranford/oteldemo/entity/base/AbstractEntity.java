package com.justincranford.oteldemo.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@SuppressWarnings({"unused"})
public abstract class AbstractEntity {
    @Id
    @GeneratedUuidV7
    @Column(nullable=false,updatable=false,columnDefinition="uuid")
    private java.util.UUID id;
}
