package com.universitymanagement.auditing;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BasedEntity {
    @CreatedDate
    public LocalDateTime createdAt;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    public LocalDateTime lastUpdateAt;
    @LastModifiedBy
    @Column(name = "last_updated_by")
    private String lastUpdatedBy;
}
