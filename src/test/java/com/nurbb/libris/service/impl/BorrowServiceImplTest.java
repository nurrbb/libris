package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.QuotasFullException;
import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.dto.response.BorrowResponse;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.entity.valueobject.Level;
import com.nurbb.libris.model.entity.valueobject.Role;
import com.nurbb.libris.model.mapper.BorrowMapper;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BorrowServiceImplTest {

    @InjectMocks
    private BorrowServiceImpl borrowService;

    @Mock private BorrowRepository borrowRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @Mock private BorrowMapper borrowMapper;
    @Mock private BookAvailabilityPublisher availabilityPublisher;

    private UUID userId;
    private UUID bookId;
    private BorrowRequest request;
    private Book book;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();

        request = new BorrowRequest();
        request.setBookId(bookId);
        request.setEmail("user@mail.com");
        request.setBorrowDate(LocalDate.now());

        book = new Book();
        book.setId(bookId);
        book.setAvailable(true);
        book.setCount(2);
        book.setPageCount(300);
        book.setTitle("Test Book");

        user = new User();
        user.setId(userId);
        user.setEmail("user@mail.com");
        user.setRole(Role.PATRON);
        user.setScore(10);
        user.setLevel(Level.READER);
        user.setTotalBorrowedBooks(0);
        user.setTotalReturnedBooks(0);
        user.setTotalReadingDays(0);
        user.setTotalReadPages(0);
        user.setStreakTimelyReturns(0);
    }

    @Test
    void borrowBook_shouldSucceed_whenValidRequest() {
        request.setDueDate(request.getBorrowDate().plusDays(7));

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(borrowRepository.existsByBookAndUserAndReturnedFalse(book, user)).thenReturn(false);
        when(borrowRepository.findByUser(user)).thenReturn(List.of());
        when(borrowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowMapper.toResponse(any())).thenReturn(new BorrowResponse());

        BorrowResponse response = borrowService.borrowBook(request);

        assertNotNull(response);
        verify(bookRepository).findById(bookId);
        verify(userRepository).findByEmail(user.getEmail());
        verify(borrowRepository).save(any());
    }

    @Test
    void borrowBook_shouldThrow_whenBookNotAvailable() {
        book.setAvailable(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThrows(InvalidRequestException.class, () -> borrowService.borrowBook(request));
    }

    @Test
    void borrowBook_shouldThrow_whenAlreadyBorrowedAndNotReturned() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(borrowRepository.existsByBookAndUserAndReturnedFalse(book, user)).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> borrowService.borrowBook(request));
    }

    @Test
    void borrowBook_shouldThrow_whenBorrowLimitExceeded() {
        request.setDueDate(request.getBorrowDate().plusDays(30));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(borrowRepository.existsByBookAndUserAndReturnedFalse(book, user)).thenReturn(false);

        Borrow activeBorrow = Borrow.builder()
                .borrowDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(10))
                .returned(false)
                .build();

        when(borrowRepository.findByUser(user)).thenReturn(List.of(activeBorrow));

        assertThrows(QuotasFullException.class, () -> borrowService.borrowBook(request));
    }

    @Test
    void returnBook_shouldSucceed_whenLibrarian() {
        Borrow borrow = new Borrow();
        borrow.setId(UUID.randomUUID());
        borrow.setBook(book);
        borrow.setUser(user);
        borrow.setBorrowDate(LocalDate.now().minusDays(10));
        borrow.setDueDate(LocalDate.now().plusDays(1));
        borrow.setReturned(false);

        user.setRole(Role.LIBRARIAN);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), "", "ROLE_LIBRARIAN")
        );

        when(borrowRepository.findById(borrow.getId())).thenReturn(Optional.of(borrow));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(borrowMapper.toResponse(any())).thenReturn(new BorrowResponse());

        BorrowResponse response = borrowService.returnBook(borrow.getId());

        assertNotNull(response);
        assertTrue(borrow.getReturned());
    }

    @Test
    void returnBook_shouldThrow_whenAlreadyReturned() {
        Borrow borrow = new Borrow();
        borrow.setId(UUID.randomUUID());
        borrow.setReturned(true);

        when(borrowRepository.findById(borrow.getId())).thenReturn(Optional.of(borrow));

        assertThrows(InvalidRequestException.class, () -> borrowService.returnBook(borrow.getId()));
    }

    @Test
    void getBorrowHistoryByUser_shouldSucceed_forLibrarian() {
        user.setRole(Role.LIBRARIAN);
        UUID targetUserId = UUID.randomUUID();
        User targetUser = new User();
        targetUser.setId(targetUserId);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), "", "ROLE_LIBRARIAN")
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(borrowRepository.findByUser(targetUser)).thenReturn(List.of());
        List<BorrowResponse> list = borrowService.getBorrowHistoryByUser(targetUserId);
        assertNotNull(list);
    }

    @Test
    void getBorrowHistoryByUser_shouldThrow_forPatronRequestingOthersData() {
        user.setRole(Role.PATRON);
        UUID otherUserId = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), "", "ROLE_PATRON")
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(InvalidRequestException.class,
                () -> borrowService.getBorrowHistoryByUser(otherUserId));
    }

    @Test
    void borrowBook_shouldThrow_whenPatronTriesPastDate() {
        user.setRole(Role.PATRON);
        request.setBorrowDate(LocalDate.now().minusDays(1));
        request.setDueDate(request.getBorrowDate().plusDays(5));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), "", "ROLE_PATRON")
        );


        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        lenient().when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThrows(InvalidRequestException.class, () -> borrowService.borrowBook(request));
    }

    @Test
    void borrowBook_shouldSucceed_whenLibrarianBorrowsForPastDate() {
        user.setRole(Role.LIBRARIAN);
        request.setBorrowDate(LocalDate.now().minusDays(3));
        request.setDueDate(request.getBorrowDate().plusDays(5));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), "", "ROLE_LIBRARIAN")
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(borrowRepository.existsByBookAndUserAndReturnedFalse(book, user)).thenReturn(false);
        when(borrowRepository.findByUser(user)).thenReturn(List.of());
        when(borrowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(borrowMapper.toResponse(any())).thenReturn(new BorrowResponse());

        BorrowResponse response = borrowService.borrowBook(request);
        assertNotNull(response);
    }
}
