package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.mapper.BookMapper;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.service.AuthorService;
import com.nurbb.libris.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookMapper bookMapper;

    private final List<Consumer<Book>> bookCreationListeners = new ArrayList<>();

    @Override
    @Transactional
    public BookResponse addBook(BookRequest request) {
        validateBookInput(request);

        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("Book with the same ISBN already exists.");
        }

        Author author = authorService.getAuthorByNameOrCreate(request.getAuthorName());
        Book book = bookMapper.toEntity(request, author);
        Book saved = bookRepository.save(book);

        bookCreationListeners.forEach(listener -> listener.accept(saved));
        return bookMapper.toResponse(saved);
    }

    @Override
    public BookResponse getBookById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        return bookMapper.toResponse(book);
    }

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toResponse)
                .toList();
    }

    @Override
    public Page<BookResponse> searchBooks(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Set<Book> mergedResults = Stream.of(
                bookRepository.findByTitleContainingIgnoreCase(query, pageable),
                bookRepository.findByAuthor_NameContainingIgnoreCase(query, pageable),
                bookRepository.findByIsbnContainingIgnoreCase(query, pageable)
        ).flatMap(p -> p.getContent().stream()).collect(Collectors.toSet());

        List<BookResponse> responseList = mergedResults.stream()
                .map(bookMapper::toResponse)
                .toList();

        return new PageImpl<>(responseList, pageable, responseList.size());
    }

    @Override
    @Transactional
    public BookResponse updateBook(UUID id, BookRequest request) {
        validateBookInput(request);

        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));

        Author author = authorService.getAuthorByNameOrCreate(request.getAuthorName());

        existing.setTitle(request.getTitle());
        existing.setIsbn(request.getIsbn());
        existing.setPublishedDate(request.getPublishedDate());
        existing.setGenre(request.getGenre());
        existing.setCount(request.getCount());
        existing.setAvailable(request.getCount() > 0);
        existing.setAuthor(author);

        return bookMapper.toResponse(bookRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        bookRepository.delete(book);
    }

    public void addBookCreationListener(Consumer<Book> listener) {
        bookCreationListeners.add(listener);
    }

    private void validateBookInput(BookRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }

        if (request.getIsbn() == null || request.getIsbn().isBlank()) {
            throw new IllegalArgumentException("ISBN cannot be empty");
        }

        if (request.getAuthorName() == null || request.getAuthorName().isBlank()) {
            throw new IllegalArgumentException("Author name must be specified");
        }

        if (request.getCount() == null || request.getCount() < 0) {
            throw new IllegalArgumentException("Book count must be 0 or greater");
        }

        if (request.getPublishedDate() == null) {
            throw new IllegalArgumentException("Published date cannot be null");
        }

        if (request.getGenre() == null) {
            throw new IllegalArgumentException("Genre must be provided");
        }
    }
}
