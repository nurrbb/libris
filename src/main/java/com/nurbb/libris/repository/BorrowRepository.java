package com.nurbb.libris.repository;

import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BorrowRepository extends JpaRepository<Borrow, UUID> {

    List<Borrow> findByUserId(UUID userId);

    List<Borrow> findByBookId(UUID bookId);

    List<Borrow> findByUser(User user);

    List<Borrow> findByReturnedFalse();

    List<Borrow> findByReturnedFalseAndDueDateBefore(LocalDate date); // Overdue

    List<Borrow> findByUserIdAndReturnedFalse(UUID userId);

    List<Borrow> findByDueDateBeforeAndReturnedFalse(LocalDate today);


}
