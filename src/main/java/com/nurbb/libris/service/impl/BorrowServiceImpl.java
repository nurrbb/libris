package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.dto.response.BorrowResponse;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.mapper.BorrowMapper;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.repository.UserRepository;
import com.nurbb.libris.service.BorrowService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowMapper borrowMapper;
    @Transactional
    @Override
    public BorrowResponse borrowBook(BorrowRequest request) {
        validateBorrowInput(request);

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!book.isAvailable() || book.getCount() <= 0) {
            throw new IllegalStateException("Book is not available for borrowing");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));

        Borrow borrow = Borrow.builder()
                .book(book)
                .user(user)
                .borrowDate(request.getBorrowDate())
                .dueDate(request.getDueDate())
                .returned(false)
                .build();

        book.setCount(book.getCount() - 1);
        book.setAvailable(book.getCount() > 0);

        Borrow saved = borrowRepository.save(borrow);
        return borrowMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BorrowResponse returnBook(UUID borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new NotFoundException("Borrow record not found"));

        if (borrow.getReturned()) {
            throw new IllegalStateException("Book has already been returned");
        }

        borrow.setReturned(true);
        borrow.setReturnDate(LocalDate.now());

        Book book = borrow.getBook();
        book.setCount(book.getCount() + 1);
        book.setAvailable(true);

        return borrowMapper.toResponse(borrowRepository.save(borrow));
    }

    @Override
    public List<BorrowResponse> getBorrowHistoryByUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return borrowRepository.findByUser(user).stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BorrowResponse> getAllBorrows() {
        return borrowRepository.findAll().stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BorrowResponse> getOverdueBorrows() {
        return borrowRepository.findByReturnedFalseAndDueDateBefore(LocalDate.now()).stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    //bunun üzerine özelleştirme yapılabilir mi
    private void validateBorrowInput(BorrowRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or blank");
        }

        if (request.getBookId() == null) {
            throw new IllegalArgumentException("Book ID cannot be null");
        }

        if (request.getBorrowDate() == null) {
            throw new IllegalArgumentException("Borrow date cannot be null");
        }

        if (request.getDueDate() == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }

        if (request.getDueDate().isBefore(request.getBorrowDate())) {
            throw new IllegalArgumentException("Due date cannot be before borrow date");
        }
    }


}
