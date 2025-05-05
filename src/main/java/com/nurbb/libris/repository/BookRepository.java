package com.nurbb.libris.repository;

import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Book> findByGenre(Genre genre, Pageable pageable);

    Page<Book> findByAuthors_NameContainingIgnoreCase(String name, Pageable pageable);

    Page<Book> findByIsbnContainingIgnoreCase(String isbn, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.authors a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Book> findByAuthorName(@Param("name") String name, Pageable pageable);

}
