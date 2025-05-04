package com.nurbb.libris.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
//@EntityLister
@Getter
@Setter
public class BaseEntity {


    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID(); // ID otomatik üretilir
        }
        if (createdDate == null) {
            createdDate = LocalDateTime.now(); // Auditing çalışmazsa fallback
        }
        updatedDate = LocalDateTime.now(); // İlk seferde güncelleme tarihi de yazılır
    }

    //  Immutable status update using Lombok's @With
}
