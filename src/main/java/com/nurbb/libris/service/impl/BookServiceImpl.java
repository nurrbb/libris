package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.mapper.BookMapper;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.service.AuthorService;
import com.nurbb.libris.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BookAvailabilityPublisher bookAvailabilityPublisher;

    @Override
    @Transactional
    public BookResponse addBook(BookRequest request) {
        validateBookInput(request);

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

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
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

        return new PageImpl<>(
                mergedResults.stream().map(bookMapper::toResponse).toList(),
                pageable,
                mergedResults.size()
        );
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

    @Override
    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));

        bookRepository.delete(book);

        // Real-time availability removal notification
        bookAvailabilityPublisher.publish(BookAvailabilityResponse.builder()
                .bookId(book.getId())
                .isAvailable(false)
                .title(book.getTitle())
                .build());
    }

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
}
