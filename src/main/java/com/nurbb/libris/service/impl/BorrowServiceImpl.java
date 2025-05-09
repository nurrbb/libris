package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.exception.QuotasFullException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.nurbb.libris.util.BookAvailabilityPublisher;
import com.nurbb.libris.util.LevelUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowMapper borrowMapper;
    private final BookAvailabilityPublisher availabilityPublisher;

    @Transactional
    @Override
    public BorrowResponse borrowBook(BorrowRequest request) {
        validateBorrowInput(request);

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!book.isAvailable() || book.getCount() <= 0) {
            throw new InvalidRequestException("Book is not available for borrowing.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));

        // 🔹 Kullanıcının seviyesi ve max süresi
        user.setLevel(LevelUtils.determineLevel(user.getScore()));
        int maxAllowedDays = LevelUtils.getMaxTotalBorrowDays(user.getLevel());

        List<Borrow> activeBorrows = borrowRepository.findByUser(user).stream()
                .filter(b -> !b.getReturned())
                .toList();

        long activeDays = activeBorrows.stream()
                .mapToLong(b -> Duration.between(b.getBorrowDate().atStartOfDay(), b.getDueDate().atStartOfDay()).toDays())
                .sum();

        long newBorrowDays = Duration.between(request.getBorrowDate().atStartOfDay(), request.getDueDate().atStartOfDay()).toDays();

        if ((activeDays + newBorrowDays) > maxAllowedDays) {
            log.warn("User {} attempted to borrow beyond allowed days. Active: {}, New: {}, Max: {}",
                    user.getEmail(), activeDays, newBorrowDays, maxAllowedDays);
            throw new QuotasFullException("Borrowing this book would exceed your allowed total borrow day limit of " + maxAllowedDays + " days.");
        }

        // 🔹 Borrow kaydı oluşturuluyor
        Borrow borrow = Borrow.builder()
                .book(book)
                .user(user)
                .borrowDate(request.getBorrowDate())
                .dueDate(request.getDueDate())
                .returned(false)
                .build();

        // 🔹 Kitap stoğu güncelle
        book.setCount(book.getCount() - 1);
        book.setAvailable(book.getCount() > 0);

        Borrow saved = borrowRepository.save(borrow);
        availabilityPublisher.publish(book);  // 📡 REACTIVE güncelleme

        log.info("User {} borrowed book '{}' from {} to {}", user.getEmail(), book.getTitle(), borrow.getBorrowDate(), borrow.getDueDate());

        // 🔹 Kullanıcı puan ve istatistik güncellemesi
        user.setTotalBorrowedBooks(user.getTotalBorrowedBooks() + 1);
        int reward = user.getTotalBorrowedBooks() == 1 ? 3 : 1;
        user.setScore(user.getScore() + reward);
        user.setLevel(LevelUtils.determineLevel(user.getScore()));
        userRepository.save(user);

        return borrowMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public BorrowResponse returnBook(UUID borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new NotFoundException("Borrow record not found"));

        if (borrow.getReturned()) {
            throw new InvalidRequestException("Book has already been returned.");
        }

        LocalDate returnDate = LocalDate.now();
        LocalDate borrowDate = borrow.getBorrowDate();
        LocalDate dueDate = borrow.getDueDate();

        borrow.setReturned(true);
        borrow.setReturnDate(returnDate);

        Book book = borrow.getBook();
        User user = borrow.getUser();

        // 🔹 Kitap stoğu iade işlemi
        book.setCount(book.getCount() + 1);
        book.setAvailable(true);
        availabilityPublisher.publish(book);  // 📡 REACTIVE güncelleme

        // 🔹 Puan hesaplama
        int delta = 0;
        long delayDays = Duration.between(dueDate.atStartOfDay(), returnDate.atStartOfDay()).toDays();
        boolean isLate = delayDays > 0;
        boolean isEarly = returnDate.isBefore(dueDate.minusDays(1));

        if (isLate) {
            delta = delayDays > 7 ? -6 : -3;
            user.setTotalLateReturns(user.getTotalLateReturns() + 1);
            user.setStreakTimelyReturns(0);
        } else {
            delta = 5;
            if (isEarly) delta += 2;
            user.setStreakTimelyReturns(user.getStreakTimelyReturns() + 1);
            if (user.getStreakTimelyReturns() % 5 == 0) delta += 10;
        }

        user.setScore(Math.max(0, user.getScore() + delta));
        user.setLevel(LevelUtils.determineLevel(user.getScore()));

        log.debug("User {} score changed by {} → new score: {}, level: {}", user.getEmail(), delta, user.getScore(), user.getLevel());

        // 🔹 Kullanıcı istatistikleri
        user.setTotalReturnedBooks(user.getTotalReturnedBooks() + 1);
        long readingDays = Duration.between(borrowDate.atStartOfDay(), returnDate.atStartOfDay()).toDays();
        user.setTotalReadingDays(user.getTotalReadingDays() + (int) readingDays);
        user.setTotalReadPages(user.getTotalReadPages() + book.getPageCount());

        userRepository.save(user);
        bookRepository.save(book);
        borrowRepository.save(borrow);

        log.info("User {} returned book '{}' on {}", user.getEmail(), book.getTitle(), returnDate);

        return borrowMapper.toResponse(borrow);
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

    private void validateBorrowInput(BorrowRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new InvalidRequestException("User email cannot be null or blank");
        }

        if (request.getBookId() == null) {
            throw new InvalidRequestException("Book ID cannot be null");
        }

        if (request.getBorrowDate() == null) {
            throw new InvalidRequestException("Borrow date cannot be null");
        }

        if (request.getDueDate() == null) {
            throw new InvalidRequestException("Due date cannot be null");
        }

        if (request.getDueDate().isBefore(request.getBorrowDate())) {
            throw new InvalidRequestException("Due date cannot be before borrow date.");
        }
    }
}
