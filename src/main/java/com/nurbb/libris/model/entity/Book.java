package com.nurbb.libris.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "book")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book extends BaseEntity {

    @Column(name = "title",nullable = false)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();

    @Column(name = "isbn", nullable = false, unique = true)
    private String isbn;

    @Column(name = "published_date", nullable = false, updatable = false)
    private LocalDate publishedDate;

    @Column(name = "genre", nullable = false)
    @Enumerated(EnumType.STRING)
    //@with?
    private Genre genre;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    //@Transient
    //public Boolean getIsAvailable() {
    //    return count != null && count > 0 && !Boolean.TRUE.equals(deleted);
    //}

}
