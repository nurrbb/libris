package com.nurbb.libris.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "book")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "isbn", nullable = false, unique = true)
    private String isbn;

    @Column(name = "published_date", nullable = false, updatable = false)
    private LocalDate publishedDate;

    @Column(name = "genre", nullable = false)
    @Enumerated(EnumType.STRING)
    private Genre genre;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    // Optional: You can still add logic here if needed
    // @Transient
    // public Boolean getIsAvailable() {
    //     return count != null && count > 0 && !Boolean.TRUE.equals(deleted);
    // }
}
