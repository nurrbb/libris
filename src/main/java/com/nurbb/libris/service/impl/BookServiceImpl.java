package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.model.dto.response.BookDeleteResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.mapper.BookMapper;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.BorrowRepository;
import com.nurbb.libris.service.AuthorService;
import com.nurbb.libris.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookMapper bookMapper;
    private final BorrowRepository borrowRepository;
    private final BookAvailabilityPublisher bookAvailabilityPublisher;


    @CacheEvict(value = { "bookList", "libraryStatistics" }, allEntries = true)
    @Override
    @Transactional
    public BookResponse addBook(BookRequest request) {

        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new InvalidRequestException("Book with the same ISBN already exists.");
        }

        Author author = authorService.getAuthorByNameOrCreate(request.getAuthorName());
        Book book = bookMapper.toEntity(request, author);
        Book saved = bookRepository.save(book);

        // Real-time availability publish
        bookAvailabilityPublisher.publish(BookAvailabilityResponse.builder()
                .bookId(saved.getId())
                .isAvailable(saved.isAvailable())
                .title(saved.getTitle())
                .build());

        return bookMapper.toResponse(saved);
    }

    @Override
    public BookResponse getBookById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        return bookMapper.toResponse(book);
    }

    @Cacheable(value = "bookList")
    @Override
    public List<BookResponse> getAllBooks() {
        List<Book> books = bookRepository.findAll();

        if (books.isEmpty()) {
            log.warn("No books found in the system.");
        }

        return books.stream()
                .map(bookMapper::toResponse)
                .toList();
    }

    @Override
    public Page<BookResponse> searchBooks(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        var mergedResults = Stream.of(
                bookRepository.findByTitleContainingIgnoreCase(query, pageable),
                bookRepository.findByAuthor_NameContainingIgnoreCase(query, pageable),
                bookRepository.findByIsbnContainingIgnoreCase(query, pageable)
        ).flatMap(p -> p.getContent().stream()).distinct().toList();

        if (mergedResults.isEmpty()) {
            log.info("Search query '{}' returned no results.", query);
        }

        return new PageImpl<>(
                mergedResults.stream().map(bookMapper::toResponse).toList(),
                pageable,
                mergedResults.size()
        );
    }

    @Override
    @Transactional
    public BookResponse updateBook(UUID id, BookRequest request) {

        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));

        Author author = authorService.getAuthorByNameOrCreate(request.getAuthorName());

        boolean isChanged = false;

        if (!existing.getTitle().equals(request.getTitle())) {
            existing.setTitle(request.getTitle());
            isChanged = true;
        }

        if (!existing.getIsbn().equals(request.getIsbn())) {
            existing.setIsbn(request.getIsbn());
            isChanged = true;
        }

        if (!existing.getPublishedDate().equals(request.getPublishedDate())) {
            existing.setPublishedDate(request.getPublishedDate());
            isChanged = true;
        }

        if (!existing.getGenre().equals(request.getGenre())) {
            existing.setGenre(request.getGenre());
            isChanged = true;
        }

        if (!existing.getCount().equals(request.getCount())) {
            existing.setCount(request.getCount());
            existing.setAvailable(request.getCount() > 0);
            isChanged = true;
        }

        if (!existing.getAuthor().getId().equals(author.getId())) {
            existing.setAuthor(author);
            isChanged = true;
        }

        if (existing.getPageCount() != request.getPageCount()) {
            existing.setPageCount(request.getPageCount());
            isChanged = true;
        }

        if (!isChanged) {
            throw new InvalidRequestException("No changes detected. Book is already up-to-date.");
        }

        existing.setTitle(request.getTitle());
        existing.setIsbn(request.getIsbn());
        existing.setPublishedDate(request.getPublishedDate());
        existing.setGenre(request.getGenre());
        existing.setCount(request.getCount());
        existing.setAvailable(request.getCount() > 0);
        existing.setAuthor(author);
        existing.setPageCount(request.getPageCount());

        Book saved = bookRepository.save(existing);

        // Real-time availability publish
        bookAvailabilityPublisher.publish(BookAvailabilityResponse.builder()
                .bookId(saved.getId())
                .isAvailable(saved.isAvailable())
                .title(saved.getTitle())
                .build());

        return bookMapper.toResponse(saved);
    }

    /**
     * Decreases the book count by one and updates availability.
     * Prevents deletion if all copies are currently borrowed.
     */

    @CacheEvict(value = { "bookList", "libraryStatistics", "overdueStats" }, allEntries = true)
    @Override
    @Transactional
    public BookDeleteResponse deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));

        long activeBorrows = borrowRepository.countByBookIdAndReturnedFalse(id);

        if (book.getCount() <= activeBorrows) {
            throw new InvalidRequestException("Cannot delete this book. All copies are currently borrowed.");
        }

        int previousCount = book.getCount();

        book.setCount(book.getCount() - 1);
        book.setAvailable(book.getCount() > activeBorrows);
        Book updated = bookRepository.save(book);

        bookAvailabilityPublisher.publish(BookAvailabilityResponse.builder()
                .bookId(book.getId())
                .isAvailable(book.isAvailable())
                .title(book.getTitle())
                .build());

        log.info("One copy of '{}' deleted. Remaining: {}, Active borrows: {}",
                updated.getTitle(), updated.getCount(), activeBorrows);

        return new BookDeleteResponse(
                updated.getTitle(),
                previousCount,
                updated.getCount(),
                "One copy deleted. Current count: " + updated.getCount()
        );
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
