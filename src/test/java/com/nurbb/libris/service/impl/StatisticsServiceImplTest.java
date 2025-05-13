package com.nurbb.libris.service.impl;

import com.nurbb.libris.model.dto.response.LibraryStatisticsResponse;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Borrow;
import com.nurbb.libris.model.entity.User;
import com.nurbb.libris.model.entity.valueobject.Genre;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Mock private BookRepository bookRepository;
    @Mock private BorrowRepository borrowRepository;
    @Mock private UserRepository userRepository;

    @Test
    void getLibraryStatistics_shouldReturnValidResponse() {
        List<Book> books = Arrays.asList(
                createBook("Book A", Genre.FANTASY, 2),
                createBook("Book B", Genre.FANTASY, 3),
                createBook("Book C", Genre.HISTORY, 1)
        );

        List<Borrow> borrows = new ArrayList<>();

        Borrow returnedBorrow = createBorrow("Book A", true, LocalDate.now().minusDays(10), LocalDate.now().minusDays(2));
        Borrow overdueBorrow = createBorrow("Book B", false, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));
        Borrow activeBorrow = createBorrow("Book C", false, LocalDate.now().minusDays(3), LocalDate.now().plusDays(7));

        borrows.add(returnedBorrow);
        borrows.add(overdueBorrow);
        borrows.add(activeBorrow);

        when(bookRepository.count()).thenReturn((long) books.size());
        when(userRepository.count()).thenReturn(10L);
        when(borrowRepository.count()).thenReturn((long) borrows.size());
        when(bookRepository.findAll()).thenReturn(books);
        when(borrowRepository.findAll()).thenReturn(borrows);
        when(borrowRepository.findByReturnedFalseAndDueDateBefore(any())).thenReturn(List.of(overdueBorrow));

        LibraryStatisticsResponse response = statisticsService.getLibraryStatistics();

        assertNotNull(response);
        assertEquals(3, response.getTotalBooks());
        assertEquals(6, response.getAvailableBooks()); // total count
        assertEquals(2, response.getBorrowedBooks()); // not returned
        assertEquals(1, response.getOverdueBooks());
        assertEquals(10, response.getTotalUsers());
        assertEquals(3, response.getTotalBorrows());
        assertEquals(8.0, response.getAverageReturnDays()); // 10 -> 2 = 8 days
        assertEquals(2, response.getTopGenres().size());
        assertEquals(3, response.getMostBorrowedBooks().size()); // all 3 books used
        assertTrue(response.getTextReport().contains("LIBRARY STATISTICS REPORT"));
    }

    @Test
    void getOverdueBookStatistics_shouldReturnCorrectMap() {
        List<Borrow> borrows = new ArrayList<>();
        borrows.add(createBorrow("A", true, LocalDate.now().minusDays(10), LocalDate.now().minusDays(2))); // returned
        borrows.add(createBorrow("B", false, LocalDate.now().minusDays(7), LocalDate.now().minusDays(1))); // overdue
        borrows.add(createBorrow("C", false, LocalDate.now().minusDays(2), LocalDate.now().plusDays(3))); // active

        when(borrowRepository.findByReturnedFalseAndDueDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(borrows.get(1))); // sadece overdue
        when(borrowRepository.count()).thenReturn(3L);

        Map<String, Object> result = statisticsService.getOverdueBookStatistics();

        assertEquals(3L, result.get("totalBorrows"));
        assertEquals(1L, result.get("overdueBorrows"));
        assertEquals(BigDecimal.valueOf(0.33).setScale(2, RoundingMode.HALF_UP), result.get("overdueRatio"));
    }


    // === HELPERS ===

    private Book createBook(String title, Genre genre, int count) {
        Book book = new Book();
        book.setTitle(title);
        book.setGenre(genre);
        book.setCount(count);
        return book;
    }
    private Borrow createBorrow(String bookTitle, boolean returned, LocalDate borrowDate, LocalDate returnOrDueDate) {
        Borrow b = new Borrow();

        Book book = new Book();
        book.setTitle(bookTitle);
        book.setGenre(Genre.FANTASY);
        book.setCount(1);
        b.setBook(book);

        User user = new User();
        user.setEmail(bookTitle.toLowerCase() + "@test.com");
        b.setUser(user);

        b.setBorrowDate(borrowDate);
        b.setReturned(returned);
        if (returned) {
            b.setReturnDate(returnOrDueDate);
        } else {
            b.setDueDate(returnOrDueDate);
        }

        return b;
    }

}
