package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.exception.QuotasFullException;
import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.model.dto.response.BorrowResponse;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.model.entity.valueobject.Role;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.mapper.BorrowMapper;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.repository.UserRepository;
import com.nurbb.libris.service.BorrowService;
import com.nurbb.libris.util.LevelUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

    /**
     * Borrows a book if it's available and the user is eligible.
     * Updates user score, level, and book availability in real time.
     */

    @CacheEvict(
            value = { "borrowHistory", "libraryStatistics", "overdueStats", "userById" },
            key = "#request.email",
            allEntries = true
    )
    @Transactional
    @Override
    public BorrowResponse borrowBook(@Valid BorrowRequest request) {

        String userRole = getLoggedInUserRole();
        validateBorrowDate(request.getBorrowDate(), userRole);

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!book.isAvailable() || book.getCount() <= 0) {
            throw new InvalidRequestException("Book is not available for borrowing.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));

        checkUserEligibility(user);

        if (borrowRepository.existsByBookAndUserAndReturnedFalse(book, user)) {
            throw new InvalidRequestException("This user has already borrowed this book and has not returned it yet.");
        }

        // Update level based on score
        user.setLevel(LevelUtils.determineLevel(user.getScore()));
        int maxAllowedDays = LevelUtils.getMaxTotalBorrowDays(user.getLevel());

        // Set due date if not provided (based on level default)
        if (request.getDueDate() == null) {
            int defaultDays = LevelUtils.getDefaultBorrowDays(user.getLevel());
            request.setDueDate(request.getBorrowDate().plusDays(defaultDays));
        }

        // Get user's currently active (not returned) borrows
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

        Borrow borrow = Borrow.builder()
                .book(book)
                .user(user)
                .borrowDate(request.getBorrowDate())
                .dueDate(request.getDueDate())
                .returned(false)
                .build();

        book.setCount(book.getCount() - 1);
        book.setAvailable(book.getCount() > 0);

        bookRepository.save(book);

        Borrow saved = borrowRepository.save(borrow);

        availabilityPublisher.publish(
                BookAvailabilityResponse.builder()
                        .bookId(book.getId())
                        .title(book.getTitle())
                        .isAvailable(book.isAvailable())
                        .build()
        );

        log.info("User {} borrowed book '{}' from {} to {}", user.getEmail(), book.getTitle(), borrow.getBorrowDate(), borrow.getDueDate());

        user.setTotalBorrowedBooks(user.getTotalBorrowedBooks() + 1);
        int reward = user.getTotalBorrowedBooks() == 1 ? 3 : 1;
        user.setScore(user.getScore() + reward);
        user.setLevel(LevelUtils.determineLevel(user.getScore()));
        userRepository.save(user);

        return borrowMapper.toResponse(saved);
    }

    /**
     * Returns a borrowed book.
     * Calculates score delta based on return timing and updates user stats accordingly.
     */

    @CacheEvict(
            value = { "borrowHistory", "libraryStatistics", "overdueStats" },
            allEntries = true
    )    @Transactional
    @Override
    public BorrowResponse returnBook(UUID borrowId) {

        String userRole = getLoggedInUserRole();
        validateReturnDate(LocalDate.now(), userRole);


        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new NotFoundException("Borrow record not found"));

        User user = borrow.getUser();

        if (borrow.getReturned()) {
            throw new InvalidRequestException("Book has already been returned.");
        }

        // Ensure that only librarians or the borrowing user can return the book
        User currentUser = getCurrentAuthenticatedUser();
        if (!isLibrarian(currentUser) && !borrow.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only return your own borrowed books.");
        }

        LocalDate returnDate = LocalDate.now();
        LocalDate borrowDate = borrow.getBorrowDate();
        LocalDate dueDate = borrow.getDueDate();

        borrow.setReturned(true);
        borrow.setReturnDate(returnDate);

        Book book = borrow.getBook();

        book.setCount(book.getCount() + 1);
        book.setAvailable(true);

        availabilityPublisher.publish(
                BookAvailabilityResponse.builder()
                        .bookId(book.getId())
                        .title(book.getTitle())
                        .isAvailable(true)
                        .build()
        );

        int delta = 0;

        // Update user score based on return timing:
        // - Late: -3 (≤7 days) or -6 (>7 days), reset streak
        // - On time: +5, +2 bonus if early, +10 bonus every 5 timely returns
        // Then update level, reading stats, and save all changes.

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

        user.setScore(user.getScore() + delta);
        user.setLevel(LevelUtils.determineLevel(user.getScore()));
        user.setTotalReturnedBooks(user.getTotalReturnedBooks() + 1);

        long readingDays = Duration.between(borrowDate.atStartOfDay(), returnDate.atStartOfDay()).toDays();
        user.setTotalReadingDays(user.getTotalReadingDays() + (int) readingDays);
        user.setTotalReadPages(user.getTotalReadPages() + book.getPageCount());

        log.info("User {} returned book '{}' on {}. Score delta: {}, new score: {}", user.getEmail(), book.getTitle(), returnDate, delta, user.getScore());

        userRepository.save(user);
        bookRepository.save(book);
        borrowRepository.save(borrow);

        return borrowMapper.toResponse(borrow);
    }

    /**
     * Retrieves the borrowing history of a specific user.
     * Patrons can only access their own records.
     */

    @Cacheable(value = "borrowHistory", key = "#userId")
    @Override
    public List<BorrowResponse> getBorrowHistoryByUser(UUID userId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));

        if (loggedInUser.getRole().equals(Role.PATRON) && !loggedInUser.getId().equals(userId)) {
            throw new InvalidRequestException("Patrons can only view their own borrow history.");
        }

        User requestedUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return borrowRepository.findByUser(requestedUser).stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BorrowResponse> getAllBorrows() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        boolean isLibrarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));

        List<Borrow> borrows;

        if (isLibrarian) {
            borrows = borrowRepository.findAll();
        } else {
            borrows = borrowRepository.findByUserEmail(email);
        }

        return borrows.stream()
                .map(borrowMapper::toResponse)
                .toList();
    }



    @Override
    public List<BorrowResponse> getOverdueBorrows() {
        List<Borrow> borrows = borrowRepository.findByReturnedFalseAndDueDateBefore(LocalDate.now());

        if (borrows.isEmpty()) {
            log.info("No overdue borrows found.");
        }

        return borrows.stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());

    }

    private void checkUserEligibility(User user) {
        if (user.getScore() < -20) {
            log.warn("User {} cannot borrow due to low score ({})", user.getEmail(), user.getScore());
            throw new InvalidRequestException("Your score is too low to borrow books. Minimum allowed: -20");
        }
    }

    /**
     * Returns the currently authenticated user from security context.
     */

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Unauthorized access.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found."));
    }

    private void validateBorrowDate(LocalDate borrowDate, String userRole) {
        if (!userRole.equals("LIBRARIAN") && !borrowDate.equals(LocalDate.now())) {
            throw new InvalidRequestException("Patrons can only borrow books for today.");
        }
    }

    private void validateReturnDate(LocalDate returnDate, String userRole) {
        if (!userRole.equals("LIBRARIAN") && !returnDate.equals(LocalDate.now())) {
            throw new InvalidRequestException("Patrons can only return books for today.");
        }
    }

    private String getLoggedInUserRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().findFirst().get().getAuthority().replace("ROLE_", "");
    }


    private boolean isLibrarian(User user) {
        return user.getRole().name().equalsIgnoreCase("LIBRARIAN");
    }

    /*
 This method was commented out temporarily to avoid redundant manual validation,
 since validation is now expected to be handled globally via @Valid and DTO-level constraints.

 private void validateBookInput(BookRequest request) {
     if (request.getTitle() == null || request.getTitle().isBlank()) {
         throw new InvalidRequestException("Book title cannot be empty");
     }
     if (request.getIsbn() == null || request.getIsbn().isBlank()) {
         throw new InvalidRequestException("ISBN cannot be empty");
     }
     if (request.getAuthorName() == null || request.getAuthorName().isBlank()) {
         throw new InvalidRequestException("Author name must be specified");
     }
     if (request.getCount() == null || request.getCount() < 0) {
         throw new InvalidRequestException("Book count must be 0 or greater");
     }
     if (request.getPublishedDate() == null) {
         throw new InvalidRequestException("Published date cannot be null");
     }
     if (request.getGenre() == null) {
         throw new InvalidRequestException("Genre must be provided");
     }
     if (request.getPageCount() <= 0) {
         throw new InvalidRequestException("Page count must be greater than 0");
     }
 }
*/

}
