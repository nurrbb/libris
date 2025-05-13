package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.valueobject.Genre;
import com.nurbb.libris.model.mapper.BookMapper;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.service.AuthorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock private BookRepository bookRepository;
    @Mock private AuthorService authorService;
    @Mock private BookMapper bookMapper;
    @Mock private BookAvailabilityPublisher availabilityPublisher;
    @Mock private BorrowRepository borrowRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private BookRequest bookRequest;
    private Author author;
    private Book book;
    private UUID bookId;

    @BeforeEach
    void init() {
        bookId = UUID.randomUUID();

        bookRequest = new BookRequest(
                "Test Book",
                "Test Author",
                "1234567890123",
                LocalDate.of(2020, 1, 1),
                Genre.FICTION,
                250,
                3
        );

        author = new Author();
        author.setId(UUID.randomUUID());
        author.setName("Test Author");

        book = new Book();
        book.setId(bookId);
        book.setTitle("Test Book");
        book.setIsbn("1234567890123");
        book.setAuthor(author);
        book.setPublishedDate(LocalDate.of(2020, 1, 1));
        book.setGenre(Genre.FICTION);
        book.setPageCount(250);
        book.setCount(3);
        book.setAvailable(true);
    }

    @Test
    void shouldAddBookSuccessfully() {
        when(bookRepository.existsByIsbn(bookRequest.getIsbn())).thenReturn(false);
        when(authorService.getAuthorByNameOrCreate("Test Author")).thenReturn(author);
        when(bookMapper.toEntity(bookRequest, author)).thenReturn(book);
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.toResponse(book)).thenReturn(new BookResponse(book.getId(), "Test Book", "Test Author", "1234567890123", LocalDate.of(2020, 1, 1), Genre.FICTION, 3, 250, true, "created", "updated"));

        BookResponse response = bookService.addBook(bookRequest);

        assertNotNull(response);
        assertEquals("Test Book", response.getTitle());
        verify(bookRepository).save(book);
        verify(availabilityPublisher).publish(any(BookAvailabilityResponse.class));
    }
    @Test
    void shouldSetAvailabilityFalseWhenCountBecomesZero() {
        UUID bookId = UUID.randomUUID();
        book.setId(bookId);
        book.setCount(1); // 1 tane kaldı

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(borrowRepository.countByBookIdAndReturnedFalse(bookId)).thenReturn(0L); // Aktif borrow yok
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.deleteBook(bookId);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book savedBook = captor.getValue();

        assertEquals(0, savedBook.getCount());
        assertFalse(savedBook.isAvailable());
    }
    @Test
    void shouldThrowExceptionWhenAllCopiesAreBorrowed() {
        UUID bookId = UUID.randomUUID();
        book.setId(bookId);
        book.setCount(2);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(borrowRepository.countByBookIdAndReturnedFalse(bookId)).thenReturn(2L);

        InvalidRequestException ex = assertThrows(
                InvalidRequestException.class,
                () -> bookService.deleteBook(bookId)
        );

        assertEquals("Cannot delete this book. All copies are currently borrowed.", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }
    @Test
    void shouldSearchBooksSuccessfully() {
        Book book1 = new Book();
        book1.setId(UUID.randomUUID());
        book1.setTitle("Java Book");
        book1.setAuthor(author);

        Book book2 = new Book();
        book2.setId(UUID.randomUUID());
        book2.setTitle("Spring Guide");
        book2.setAuthor(author);

        Page<Book> titleResults = new PageImpl<>(List.of(book1));
        Page<Book> authorResults = new PageImpl<>(List.of(book2));
        Page<Book> isbnResults = new PageImpl<>(List.of());

        when(bookRepository.findByTitleContainingIgnoreCase(eq("java"), any())).thenReturn(titleResults);
        when(bookRepository.findByAuthor_NameContainingIgnoreCase(eq("java"), any())).thenReturn(authorResults);
        when(bookRepository.findByIsbnContainingIgnoreCase(eq("java"), any())).thenReturn(isbnResults);

        when(bookMapper.toResponse(book1)).thenReturn(new BookResponse(book1.getId(), "Java Book", "Test Author", "111", LocalDate.now(), Genre.FICTION, 1, 100, true, "c", "u"));
        when(bookMapper.toResponse(book2)).thenReturn(new BookResponse(book2.getId(), "Spring Guide", "Test Author", "222", LocalDate.now(), Genre.FICTION, 1, 100, true, "c", "u"));

        Page<BookResponse> result = bookService.searchBooks("java", 0, 10);

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void shouldThrowExceptionWhenIsbnExists() {
        when(bookRepository.existsByIsbn(bookRequest.getIsbn())).thenReturn(true);
        assertThrows(InvalidRequestException.class, () -> bookService.addBook(bookRequest));
    }

    @Test
    void shouldGetBookById() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookMapper.toResponse(book)).thenReturn(new BookResponse(bookId, "Test Book", "Test Author", "1234567890123", LocalDate.of(2020, 1, 1), Genre.FICTION, 3, 250, true, "created", "updated"));

        BookResponse response = bookService.getBookById(bookId);
        assertEquals("Test Book", response.getTitle());
    }

    @Test
    void shouldThrowNotFoundWhenGettingUnknownBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookService.getBookById(bookId));
    }

    @Test
    void shouldUpdateBookSuccessfully() {

        Book existingBook = new Book();
        existingBook.setId(bookId);
        existingBook.setTitle("Old Title");
        existingBook.setIsbn("1111111111111");
        existingBook.setPublishedDate(LocalDate.of(2000, 1, 1));
        existingBook.setPageCount(100);
        existingBook.setCount(1);
        existingBook.setGenre(Genre.FICTION);
        existingBook.setAuthor(author);
        Book updated = new Book();
        updated.setId(bookId);
        updated.setTitle("Updated Book");
        updated.setIsbn("9999999999999");
        updated.setPublishedDate(LocalDate.now());
        updated.setPageCount(200);
        updated.setCount(2);
        updated.setGenre(Genre.SCIENCE);
        updated.setAuthor(author);

        BookRequest bookRequest = new BookRequest(
                "Updated Book",
                "Test Author",
                "9999999999999",
                LocalDate.now(),
                Genre.SCIENCE,
                2,
                200
        );

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));
        when(authorService.getAuthorByNameOrCreate("Test Author")).thenReturn(author);
        when(bookRepository.save(any(Book.class))).thenReturn(updated);
        when(bookMapper.toResponse(updated)).thenReturn(new BookResponse(
                bookId, "Updated Book", "Test Author", "9999999999999",
                LocalDate.now(), Genre.SCIENCE, 2, 200, true, "created", "updated"
        ));

        BookResponse response = bookService.updateBook(bookId, bookRequest);

        assertNotNull(response);
        assertEquals("Updated Book", response.getTitle());
        verify(bookRepository).save(any(Book.class));
    }


    @Test
    void shouldThrowNotFoundWhenUpdatingUnknownBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookService.updateBook(bookId, bookRequest));
    }

    @Test
    void shouldDeleteBookSuccessfully() {
        UUID bookId = UUID.randomUUID();

        Book book = new Book();
        book.setId(bookId);
        book.setTitle("Test Book");
        book.setIsbn("1234567890");
        book.setGenre(Genre.SCIENCE);
        book.setCount(3);
        book.setAvailable(true);
        book.setAuthor(new Author());
        book.setPublishedDate(LocalDate.now());
        book.setPageCount(100);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(borrowRepository.countByBookIdAndReturnedFalse(bookId)).thenReturn(1L);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.deleteBook(bookId);

        assertEquals(2, book.getCount());
        assertTrue(book.isAvailable());

        verify(bookRepository).save(book);
        verify(availabilityPublisher).publish(any(BookAvailabilityResponse.class));
    }





    @Test
    void shouldThrowNotFoundWhenDeletingUnknownBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bookService.deleteBook(bookId));
    }

    @Test
    void shouldThrowExceptionWhenBookHasActiveBorrows() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(borrowRepository.countByBookIdAndReturnedFalse(bookId)).thenReturn(3L);
        book.setCount(3);

        assertThrows(InvalidRequestException.class, () -> bookService.deleteBook(bookId));
    }

    @Test
    void shouldGetAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(book));
        when(bookMapper.toResponse(book)).thenReturn(new BookResponse(book.getId(), "Test Book", "Test Author", "1234567890123", LocalDate.of(2020, 1, 1), Genre.FICTION, 3, 250, true, "created", "updated"));

        List<BookResponse> responses = bookService.getAllBooks();
        assertEquals(1, responses.size());
    }

    @Test
    void shouldLogWarningWhenNoBooksFound() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<BookResponse> responses = bookService.getAllBooks();

        assertTrue(responses.isEmpty());
        verify(bookRepository).findAll();
    }

    @Test
    void shouldReturnEmptyWhenNoSearchResults() {
        Page<Book> emptyPage = new PageImpl<>(List.of());

        when(bookRepository.findByTitleContainingIgnoreCase(eq("xyz"), any())).thenReturn(emptyPage);
        when(bookRepository.findByAuthor_NameContainingIgnoreCase(eq("xyz"), any())).thenReturn(emptyPage);
        when(bookRepository.findByIsbnContainingIgnoreCase(eq("xyz"), any())).thenReturn(emptyPage);

        Page<BookResponse> result = bookService.searchBooks("xyz", 0, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }
    @Test
    void shouldThrowExceptionWhenNoChangesInUpdate() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(authorService.getAuthorByNameOrCreate(bookRequest.getAuthorName())).thenReturn(author);

        InvalidRequestException ex = assertThrows(
                InvalidRequestException.class,
                () -> bookService.updateBook(bookId, bookRequest)
        );

        assertEquals("No changes detected. Book is already up-to-date.", ex.getMessage());
    }

}
