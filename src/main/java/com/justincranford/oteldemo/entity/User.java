package com.justincranford.oteldemo.entity;

import com.justincranford.oteldemo.entity.base.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name="users")
@Getter
@Setter
@RequiredArgsConstructor
@Accessors(fluent=true,chain= true)
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@Builder
@AllArgsConstructor
public class User extends AbstractEntity {
    @Column(nullable=false,length=100,unique=true)
    private String name;
}
