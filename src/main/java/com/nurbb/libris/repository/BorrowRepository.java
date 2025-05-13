package com.nurbb.libris.repository;

import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BorrowRepository extends JpaRepository<Borrow, UUID> {

    List<Borrow> findByUser(User user);

    List<Borrow> findByReturnedFalseAndDueDateBefore(LocalDate date);

    long countByBookIdAndReturnedFalse(UUID bookId);

    boolean existsByUserAndReturnedFalse(User user);

    boolean existsByBookAndUserAndReturnedFalse(Book book, User user);

    List<Borrow> findByUserEmail(String email);

}
